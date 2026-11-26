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

        fun bind(m: ChatMessageResponse) {
            val context = binding.root.context
            val mine = m.senderId == myUserId
            // 퀵블록은 앞에 [퀵] 표시
            val text = if (m.msgType == "QUICK") "[퀵] ${m.content}" else m.content
            val time = clockOf(m.sentAt)

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
                binding.tvLeftBubble.text = text
                binding.tvLeftTime.text = time
                binding.tvLeftBubble.background = context.roundedBg(PotColors.WHITE, 14f, PotColors.LINE)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    object Diff : DiffUtil.ItemCallback<ChatMessageResponse>() {
        override fun areItemsTheSame(a: ChatMessageResponse, b: ChatMessageResponse) = a.id == b.id
        override fun areContentsTheSame(a: ChatMessageResponse, b: ChatMessageResponse) = a == b
    }
}