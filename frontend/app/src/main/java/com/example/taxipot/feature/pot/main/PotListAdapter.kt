package com.example.taxipot.feature.pot.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taxipot.databinding.ItemPotBinding
import com.example.taxipot.data.remote.dto.pot.PotSummaryResponse
import com.example.taxipot.feature.pot.data.departureClock
import com.example.taxipot.feature.pot.data.genderLabel
import com.example.taxipot.feature.pot.data.isVacancy

// onClick: 카드를 눌렀을 때 할 일 (화면에서 정해 줌)
class PotListAdapter(
    private val onClick: (PotSummaryResponse) -> Unit
) : ListAdapter<PotSummaryResponse, PotListAdapter.ViewHolder>(Diff) {

    // 카드 한 장(item_pot.xml)에 방 정보 채우기
    inner class ViewHolder(private val binding: ItemPotBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pot: PotSummaryResponse) {
            val context = binding.root.context

            // 카드 배경: 흰색 + 둥근 모서리 + 베이지 테두리
            binding.root.background = context.roundedBg(PotColors.CARD, 14f, PotColors.LINE)

            // 상태 칩 (결원 · 모집 중 / 모집 중 / 모집 마감)
            binding.tvStatus.text = when {
                pot.isVacancy -> "결원 · 모집 중"
                pot.status == "RECRUITING" -> "모집 중"
                pot.status == "CLOSED" -> "모집 마감"
                else -> "기타"
            }
            binding.tvStatus.background = context.roundedBg(PotColors.ACCENT, 6f)

            binding.tvTime.text = pot.departureClock
            binding.tvRoute.text = "${pot.departureName} → ${pot.destinationName}"
            binding.tvMembers.text = "${pot.currentMembers} / ${pot.maxMembers}명"
            binding.tvFare.text = "%,d원".format(pot.expectedFare)

            // 카드 아래 정보: 방장 · 거리 · 성별 제한
            val footer = mutableListOf("방장 ${pot.hostNickname} ★${pot.hostRatingAvg}")
            pot.distanceMeters?.let { footer.add("${it}m") }
            pot.genderLabel?.let { footer.add(it) }
            binding.tvHostInfo.text = footer.joinToString(" · ")

            binding.root.setOnClickListener { onClick(pot) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemPotBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // 목록이 바뀌었을 때 바뀐 카드만 다시 그리게 해 주는 비교 규칙
    object Diff : DiffUtil.ItemCallback<PotSummaryResponse>() {
        override fun areItemsTheSame(a: PotSummaryResponse, b: PotSummaryResponse) = a.id == b.id
        override fun areContentsTheSame(a: PotSummaryResponse, b: PotSummaryResponse) = a == b
    }
}