package com.example.taxipot.feature.pot.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taxipot.data.remote.dto.chat.QuickBlockResponse
import com.example.taxipot.databinding.ActivityChatBinding
import com.example.taxipot.feature.pot.main.PotColors
import com.example.taxipot.feature.pot.main.dp
import com.example.taxipot.feature.pot.main.roundedBg
import kotlinx.coroutines.launch

/**
 * RD-02 채팅
 * 이전 메시지(위로 올리면 더 불러오기), 퀵블록(서버 문구), 메시지 보내기, 실시간 받기(STOMP)
 */
class ChatActivity : ComponentActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private val adapter = ChatAdapter()

    // 퀵블록 버튼은 목록이 바뀔 때만 다시 그림
    private var drawnQuickBlocks: List<QuickBlockResponse> = emptyList()

    // 맨 아래로 스크롤한 마지막 번호
    private var lastScrollToken = 0

    companion object {
        private const val EXTRA_POT_ID = "pot_id"

        /** 방 번호를 담아서 채팅 열기 */
        fun newIntent(context: Context, potId: Long): Intent =
            Intent(context, ChatActivity::class.java).putExtra(EXTRA_POT_ID, potId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) XML 화면 붙이기
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 2) 상태바·하단바·키보드 여백 + 상태바 글자를 어두운 색으로
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        WindowCompat.getInsetsController(window, binding.root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        // 3) 디자인
        binding.root.setBackgroundColor(PotColors.BACKGROUND)
        binding.bottomPanel.background = roundedBg(PotColors.CARD, 0f, PotColors.LINE)
        binding.etMessage.background = roundedBg(PotColors.WHITE, 10f, PotColors.LINE)
        binding.btnSend.background = roundedBg(PotColors.ACCENT, 10f)

        // 4) 메시지 목록 (아래부터 쌓이게)
        binding.rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.rvMessages.adapter = adapter
        // 위로 끝까지 올리면 더 오래된 메시지
        binding.rvMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy < 0 && !recyclerView.canScrollVertically(-1)) viewModel.loadMore()
            }
        })

        // 5) 버튼
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSend.setOnClickListener { sendTyped() }
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendTyped()
                true
            } else {
                false
            }
        }

        // 6) 시작
        viewModel.start(intent.getLongExtra(EXTRA_POT_ID, -1L))
        observeViewModel()
    }

    private fun sendTyped() {
        val text = binding.etMessage.text.toString()
        if (text.isBlank()) return
        viewModel.sendText(text)
        binding.etMessage.setText("")
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tvSubtitle.text = state.subtitle
                    adapter.myUserId = state.myUserId

                    // 메시지 목록 → 그린 뒤 필요하면 맨 아래로
                    adapter.submitList(state.messages) {
                        if (state.scrollToBottom != lastScrollToken && state.messages.isNotEmpty()) {
                            lastScrollToken = state.scrollToBottom
                            binding.rvMessages.scrollToPosition(state.messages.size - 1)
                        }
                    }

                    if (state.quickBlocks != drawnQuickBlocks) {
                        drawnQuickBlocks = state.quickBlocks
                        drawQuickBlocks(state.quickBlocks)
                    }

                    binding.btnSend.isEnabled = !state.sending
                    binding.btnSend.text = if (state.sending) "..." else "전송"

                    // 전송 실패한 메시지는 입력칸에 되돌리기
                    state.failedText?.let {
                        if (binding.etMessage.text.isNullOrEmpty()) {
                            binding.etMessage.setText(it)
                            binding.etMessage.setSelection(it.length)
                        }
                        viewModel.failedTextShown()
                    }

                    state.message?.let {
                        showToast(it)
                        viewModel.messageShown()
                    }
                }
            }
        }
    }

    // 퀵블록 버튼을 2개씩 한 줄로
    private fun drawQuickBlocks(blocks: List<QuickBlockResponse>) {
        binding.quickContainer.removeAllViews()
        blocks.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8f).toInt() }
            }
            pair.forEachIndexed { index, block ->
                row.addView(TextView(this).apply {
                    text = block.content
                    gravity = Gravity.CENTER
                    setTextColor(PotColors.TEXT)
                    textSize = 13f
                    background = roundedBg(PotColors.WHITE, 10f, PotColors.LINE)
                    layoutParams = LinearLayout.LayoutParams(0, dp(40f).toInt(), 1f).apply {
                        if (index > 0) marginStart = dp(8f).toInt()
                    }
                    setOnClickListener { viewModel.sendQuick(block) }
                })
            }
            // 마지막 줄에 1개만 있으면 빈칸으로 자리 맞추기
            if (pair.size == 1) {
                row.addView(android.view.View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(40f).toInt(), 1f).apply {
                        marginStart = dp(8f).toInt()
                    }
                })
            }
            binding.quickContainer.addView(row)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
        viewModel.startRealtime()   // 화면이 보이는 동안 실시간 연결
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopRealtime()
    }
}