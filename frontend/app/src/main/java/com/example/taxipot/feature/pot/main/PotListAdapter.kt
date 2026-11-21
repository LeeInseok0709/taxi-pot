package com.example.taxipot.feature.pot.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taxipot.databinding.ItemPotBinding
import com.example.taxipot.feature.pot.data.PotSummaryResponse

// onClick: 카드를 눌렀을 때 할 일 (PotMainActivity에서 정해 줌)
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

            // 상태 칩
            binding.tvStatus.text = if (pot.status == "VACANCY") "결원 · 모집 중" else "모집 중"
            binding.tvStatus.background = context.roundedBg(PotColors.ACCENT, 6f)

            binding.tvTime.text = pot.departureTime
            binding.tvRoute.text = "${pot.departureName} → ${pot.arrivalName}"
            binding.tvMembers.text = "${pot.currentMembers} / ${pot.maxMembers}명"
            binding.tvFare.text = "%,d원".format(pot.totalFare)
            binding.tvHostInfo.text = "방장 ${pot.hostNickname} ★${pot.hostRating} · ${pot.distanceMeters}m"

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
        override fun areItemsTheSame(a: PotSummaryResponse, b: PotSummaryResponse) = a.potId == b.potId
        override fun areContentsTheSame(a: PotSummaryResponse, b: PotSummaryResponse) = a == b
    }
}