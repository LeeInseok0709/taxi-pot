package com.example.taxipot.feature.auth.verification

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxipot.core.network.ApiClient
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.core.network.safeApiCall
import com.example.taxipot.data.remote.api.VerificationApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

data class CaptureUiState(
    /** 화면에 보여줄 캡처 미리보기 (화면을 돌려도 유지되도록 여기 보관) */
    val preview: Bitmap? = null,
    val imageError: String? = null,
    val studentNoError: String? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

sealed interface CaptureEvent {
    /** 제출 완료(또는 이미 심사 중) → 인증 상태(AU-04)로 */
    data object Submitted : CaptureEvent
    /** 이메일 인증이 아직이면 AU-02로 */
    data object NeedEmailVerification : CaptureEvent
}

class CaptureVerificationViewModel : ViewModel() {

    private val api = ApiClient.create(VerificationApi::class.java)

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    private val _events = Channel<CaptureEvent>(Channel.BUFFERED)
    val events: Flow<CaptureEvent> = _events.receiveAsFlow()

    /** 서버로 보낼 JPEG */
    private var captureJpeg: ByteArray? = null

    fun setCapture(preview: Bitmap, jpeg: ByteArray) {
        captureJpeg = jpeg
        _uiState.update { it.copy(preview = preview, imageError = null, errorMessage = null) }
    }

    fun showImageError(message: String) = _uiState.update { it.copy(imageError = message) }

    fun clearStudentNoError() = _uiState.update {
        if (it.studentNoError != null || it.errorMessage != null) {
            it.copy(studentNoError = null, errorMessage = null)
        } else {
            it
        }
    }

    fun submit(rawStudentNo: String) {
        if (_uiState.value.isSubmitting) return

        val studentNo = rawStudentNo.trim()
        val jpeg = captureJpeg
        val imageError = if (jpeg == null) "캡처 이미지를 선택해 주세요." else null
        val studentNoError = if (studentNo.isEmpty()) "학번을 입력해 주세요." else null
        if (jpeg == null || imageError != null || studentNoError != null) {
            _uiState.update { it.copy(imageError = imageError, studentNoError = studentNoError) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val image = MultipartBody.Part.createFormData(
                "captureImage", "capture.jpg", jpeg.toRequestBody("image/jpeg".toMediaType())
            )
            val studentNoBody = studentNo.toRequestBody("text/plain".toMediaType())

            when (val result = safeApiCall { api.submitCapture(image, studentNoBody) }) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _events.send(CaptureEvent.Submitted)
                }

                is ApiResult.Failure -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    when (result.code) {
                        "STUDENT_NO_MISMATCH" -> _uiState.update {
                            it.copy(studentNoError = "가입할 때 입력한 학번과 달라요.")
                        }
                        "EMAIL_NOT_VERIFIED" -> _events.send(CaptureEvent.NeedEmailVerification)
                        // 이미 제출해서 심사 중이거나 승인됨 → 상태 화면에서 확인
                        "REVIEW_IN_PROGRESS", "ALREADY_VERIFIED" -> _events.send(CaptureEvent.Submitted)
                        else -> _uiState.update { it.copy(errorMessage = result.message) }
                    }
                }

                is ApiResult.NetworkError -> _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = "서버에 연결할 수 없습니다. 인터넷 연결을 확인해 주세요."
                    )
                }
            }
        }
    }
}
