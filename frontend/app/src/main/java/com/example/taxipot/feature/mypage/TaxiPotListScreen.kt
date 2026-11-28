package com.example.taxipot.feature.mypage

import android.view.LayoutInflater
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.taxipot.core.ui.PagedListState
import com.example.taxipot.databinding.ActivityTaxipotListBinding
import com.example.taxipot.databinding.ItemTaxipotCardBinding

/**
 * 카드 목록 화면 공통 처리 (이용 기록·청구 내역·신고 내역·알림이 같이 씀).
 * activity_taxipot_list.xml + item_taxipot_card.xml 조합.
 */
object TaxiPotListScreen {

    /** 상단 바 제목·뒤로가기, 시스템 여백 */
    fun setup(activity: ComponentActivity, binding: ActivityTaxipotListBinding, title: String) {
        activity.enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        WindowCompat.getInsetsController(activity.window, binding.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding.appBar.tvAppBarTitle.text = title
        binding.appBar.btnBack.setOnClickListener { activity.finish() }
    }

    /** 카드들을 다시 그린다. bind에서 카드 하나를 채운다 */
    fun <T> render(
        binding: ActivityTaxipotListBinding,
        state: PagedListState<T>,
        emptyText: String,
        bind: (ItemTaxipotCardBinding, T) -> Unit
    ) {
        binding.progress.visibility = if (state.isLoading && state.items.isEmpty()) View.VISIBLE else View.GONE
        binding.tvError.text = state.errorMessage
        binding.tvError.visibility = if (state.errorMessage != null) View.VISIBLE else View.GONE

        binding.list.removeAllViews()
        val inflater = LayoutInflater.from(binding.root.context)
        state.items.forEach { item ->
            val card = ItemTaxipotCardBinding.inflate(inflater, binding.list, false)
            // 기본은 다 숨기고 bind에서 필요한 것만 켠다
            card.tvChip.visibility = View.GONE
            card.tvDetail.visibility = View.GONE
            card.tvStrong.visibility = View.GONE
            card.btnPrimary.visibility = View.GONE
            card.btnOutline.visibility = View.GONE
            bind(card, item)
            binding.list.addView(card.root)
        }

        binding.tvEmpty.text = emptyText
        binding.tvEmpty.visibility =
            if (state.loaded && !state.isLoading && state.items.isEmpty() && state.errorMessage == null) View.VISIBLE else View.GONE
        binding.tvMore.visibility = if (state.hasMore) View.VISIBLE else View.GONE
        binding.tvMore.text = if (state.isLoading) "불러오는 중..." else "더 보기"
    }
}
