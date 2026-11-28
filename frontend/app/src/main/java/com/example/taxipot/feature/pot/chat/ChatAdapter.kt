package com.example.taxipot.feature.pot.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taxipot.data.remote.dto.chat.ChatMessageResponse
import com.example.taxipot.databinding.ItemChatMessageBinding
import com.example.taxipot.feature.pot.data.clockOf
import com.example.taxipot.feature.pot.main.PotColors
import com.example.taxipot.feature.pot.main.roundedBg

/** 채팅 메시지 목록: 내 메시지는 오른쪽(베이지), 다른 사람은 왼쪽(흰색 + 닉네임) */
class ChatAdapter : ListAdapter<ChatMessageResponse, ChatAdapter.ViewHolder>(Diff) {

    /** 이 방에서 "나"의 회원 번호 (바뀌면 다시 그림) */
    var myUserId: Long? = null
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    inner class ViewHolder(private val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(m: ChatMessageResponse, prev: ChatMessageResponse?) {
            val context = binding.root.context
            val mine = m.senderId == myUserId
            // 퀵블록은 앞에 [퀵] 표시
            val text = if (m.msgType == "QUICK") "[퀵] ${m.content}" else m.content
            val time = if (m.sentAt.length >= 16) m.sentAt.substring(11, 16) else m.sentAt

            // 날짜가 바뀌는 첫 메시지 위에 "11월 26일"
            val day = m.sentAt.take(10)
            val newDay = prev == null || prev.sentAt.take(10) != day
            binding.tvDate.visibility = if (newDay) View.VISIBLE else View.GONE
            if (newDay && day.length == 10) {
                binding.tvDate.text = "${day.substring(5, 7).toInt()}월 ${day.substring(8, 10).toInt()}일"
                binding.tvDate.background = context.roundedBg(PotColors.CARD, 10f)
            }
            // 같은 사람이 연달아 보내면 닉네임은 처음 한 번만
            val sameSender = !newDay && prev?.senderId == m.senderId

            if (mine) {
                binding.leftBox.visibility = View.GONE
                binding.rightBox.visibility = View.VISIBLE
                binding.tvRightBubble.text = text
                binding.tvRightTime.text = time
                binding.tvRightBubble.background = context.roundedBg(PotColors.ACCENT, 14f)
            } else {
                binding.rightBox.visibility = View.GONE
                binding.leftBox.visibility = View.VISIBLE
                binding.tvNick.text = m.senderNickname
                binding.tvNick.visibility = if (sameSender) View.GONE else View.VISIBLE
                binding.tvLeftBubble.text = text
                binding.tvLeftTime.text = time
                binding.tvLeftBubble.background = context.roundedBg(PotColors.WHITE, 14f, PotColors.LINE)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position), if (position > 0) getItem(position - 1) else null)

    object Diff : DiffUtil.ItemCallback<ChatMessageResponse>() {
        override fun areItemsTheSame(a: ChatMessageResponse, b: ChatMessageResponse) = a.id == b.id
        override fun areContentsTheSame(a: ChatMessageResponse, b: ChatMessageResponse) = a == b
    }
}