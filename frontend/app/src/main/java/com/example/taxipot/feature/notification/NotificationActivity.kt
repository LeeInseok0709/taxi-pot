package com.example.taxipot.feature.notification

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.taxipot.data.remote.dto.notification.NotificationResponse
import com.example.taxipot.databinding.ActivityTaxipotListBinding
import com.example.taxipot.databinding.ItemTaxipotCardBinding
import com.example.taxipot.feature.mypage.TaxiPotListScreen
import com.example.taxipot.feature.wallet.WalletFormat
import kotlinx.coroutines.launch

/** MY-05 알림: 안 읽은 알림은 ●, 누르면 읽음 처리 후 관련 화면으로 */
class NotificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaxipotListBinding
    private val viewModel: NotificationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaxipotListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        TaxiPotListScreen.setup(this, binding, "알림")
        binding.tvMore.setOnClickListener { viewModel.loadMore() }

        binding.btnTopAction.text = "모두 읽음"
        binding.btnTopAction.visibility = View.VISIBLE
        binding.btnTopAction.setOnClickListener { viewModel.markAllRead() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    TaxiPotListScreen.render(binding, state, "새 알림이 없어요.") { card, n -> bind(card, n) }
                    binding.btnTopAction.isEnabled = state.items.any { !it.isRead }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun bind(card: ItemTaxipotCardBinding, n: NotificationResponse) {
        card.tvTitle.text = (if (n.isRead) "" else "● ") + (n.title ?: "알림")
        card.tvTitle.setTypeface(null, if (n.isRead) Typeface.NORMAL else Typeface.BOLD)
        card.tvDetail.text = listOfNotNull(
            n.content,
            WalletFormat.shortDateTime(n.createdAt).ifEmpty { null }
        ).joinToString("\n")
        card.tvDetail.visibility = View.VISIBLE
        card.root.setOnClickListener {
            if (!n.isRead) viewModel.markRead(n.id)
            NotificationRouter.open(this, n)
        }
    }
}
