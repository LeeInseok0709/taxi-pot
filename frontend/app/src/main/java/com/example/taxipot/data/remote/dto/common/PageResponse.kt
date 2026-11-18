package com.example.taxipot.data.remote.dto.common

/** 목록 API 공통 페이지 응답 (page는 0부터, size 기본 20) */
data class PageResponse<T>(
    val content: List<T>,
    val page: PageMeta
)

data class PageMeta(
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
