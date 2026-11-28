package com.example.taxipot.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxipot.core.network.ApiResult
import com.example.taxipot.data.remote.dto.common.PageResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 페이지로 나눠 받는 목록 화면의 공통 상태 */
data class PagedListState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = false,
    /** 한 번이라도 불러왔는지 (빈 목록 안내를 언제 보일지) */
    val loaded: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 목록 화면 공통 ViewModel: 처음 불러오기(refresh) · 더 보기(loadMore)만 구현돼 있다.
 * 사용법: fetch(page, size)에서 safeApiCall { api.getXxx(page = page, size = size) }를 돌려주면 된다.
 */
abstract class PagedListViewModel<T> : ViewModel() {

    private val _state = MutableStateFlow(PagedListState<T>())
    val state: StateFlow<PagedListState<T>> = _state.asStateFlow()

    private var nextPage = 0
    private var job: Job? = null

    protected abstract suspend fun fetch(page: Int, size: Int): ApiResult<PageResponse<T>>

    fun refresh() = load(reset = true)

    fun loadMore() {
        val state = _state.value
        if (state.isLoading || !state.hasMore) return
        load(reset = false)
    }

    /** 목록 안 항목 바꾸기 (예: 알림 읽음 표시) */
    protected fun updateItems(transform: (List<T>) -> List<T>) =
        _state.update { it.copy(items = transform(it.items)) }

    private fun load(reset: Boolean) {
        job?.cancel()
        if (reset) nextPage = 0
        val page = nextPage
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        job = viewModelScope.launch {
            when (val result = fetch(page, PAGE_SIZE)) {
                is ApiResult.Success -> {
                    val meta = result.data.page
                    nextPage = meta.page + 1
                    _state.update {
                        it.copy(
                            items = if (reset) result.data.content else it.items + result.data.content,
                            isLoading = false,
                            loaded = true,
                            hasMore = meta.page + 1 < meta.totalPages
                        )
                    }
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(isLoading = false, loaded = true, errorMessage = result.message)
                }
                is ApiResult.NetworkError -> _state.update {
                    it.copy(isLoading = false, loaded = true, errorMessage = "서버에 연결할 수 없습니다. 인터넷 연결을 확인해 주세요.")
                }
            }
        }
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
