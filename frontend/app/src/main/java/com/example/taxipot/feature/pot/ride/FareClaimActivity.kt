package com.example.taxipot.feature.pot.ride

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.databinding.ActivityFareClaimBinding
import com.example.taxipot.feature.pot.data.PotRepository
import com.example.taxipot.feature.pot.main.PotColors
import com.example.taxipot.feature.pot.main.roundedBg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * RD-05 실요금 청구 (방장)
 * 결제내역 사진(JPEG로 줄여서) + 실제 요금 → POST /api/pots/{potId}/fare-claim (multipart)
 */
class FareClaimActivity : ComponentActivity() {

    private lateinit var binding: ActivityFareClaimBinding
    private var potId = -1L

    // 고른 사진 (JPEG 바이트)
    private var receiptJpeg: ByteArray? = null
    private var sending = false

    companion object {
        private const val EXTRA_POT_ID = "pot_id"
        private const val MAX_SIDE = 1600   // 사진 긴 변 최대 px
        private const val MAX_FARE = 500_000

        fun newIntent(context: Context, potId: Long): Intent =
            Intent(context, FareClaimActivity::class.java).putExtra(EXTRA_POT_ID, potId)
    }

    // 안드로이드 사진 선택기 (권한 필요 없음)
    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) loadPhoto(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFareClaimBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        WindowCompat.getInsetsController(window, binding.root).isAppearanceLightStatusBars = true

        binding.photoBox.background = roundedBg(PotColors.CARD, 12f, PotColors.LINE)
        binding.etFare.background = roundedBg(PotColors.WHITE, 10f, PotColors.LINE)
        binding.tvInfo.background = roundedBg(PotColors.CARD, 12f, PotColors.LINE)
        binding.btnSubmit.background = roundedBg(PotColors.ACCENT, 12f)

        potId = intent.getLongExtra(EXTRA_POT_ID, -1L)
        binding.btnBack.setOnClickListener { finish() }
        binding.photoBox.setOnClickListener {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.btnSubmit.setOnClickListener { submit() }

        // 위에 경로 표시
        lifecycleScope.launch {
            val pot = (PotRepository.getPot(potId) as? ApiResult.Success)?.data ?: return@launch
            binding.tvRoute.text = "${pot.departureName} → ${pot.destinationName} · 예상 %,d원".format(pot.expectedFare)
        }
    }

    // 사진 읽기 → 긴 변 1600px로 줄이기 → JPEG
    private fun loadPhoto(uri: Uri) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val original = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                        ?: return@withContext null
                    val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(original.width, original.height))
                    val bitmap = if (scale < 1f) {
                        Bitmap.createScaledBitmap(original, (original.width * scale).toInt(), (original.height * scale).toInt(), true)
                    } else {
                        original
                    }
                    val out = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    bitmap to out.toByteArray()
                } catch (e: Exception) {
                    null
                }
            }
            if (result == null) {
                showToast("사진을 읽지 못했어요. 다른 사진을 골라 주세요")
                return@launch
            }
            receiptJpeg = result.second
            binding.ivReceipt.setImageBitmap(result.first)
            binding.ivReceipt.visibility = View.VISIBLE
            binding.tvPhotoHint.visibility = View.GONE
        }
    }

    private fun submit() {
        if (sending) return
        val jpeg = receiptJpeg
        val fare = binding.etFare.text.toString().toIntOrNull()
        when {
            jpeg == null -> return showToast("결제내역 사진을 골라 주세요")
            fare == null || fare <= 0 -> return showToast("실제 요금을 입력해 주세요")
            fare > MAX_FARE -> return showToast("요금은 %,d원까지 입력할 수 있어요".format(MAX_FARE))
        }

        sending = true
        binding.btnSubmit.text = "보내는 중..."
        lifecycleScope.launch {
            val result = PotRepository.claimFare(potId, fare!!, jpeg!!)
            sending = false
            if (result is ApiResult.Success) {
                showToast("청구했어요! 확인이 끝나면 정산돼요")
                startActivity(Intent(this@FareClaimActivity, com.example.taxipot.feature.pot.main.PotMainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                finish()
            } else {
                binding.btnSubmit.text = "청구하기"
                showToast(PotRepository.errorMessage(result))
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}