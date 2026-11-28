package com.example.taxipot.data.remote.dto.report

/** POST /api/reports 신고 접수 */
data class ReportCreateRequest(
    val reportedId: Long,
    val potId: Long,
    /** NO_SHOW(노쇼) / ABUSIVE(불쾌한 언행) / ETC(기타) */
    val type: String,
    /** 255자 이하 */
    val reason: String
)

data class ReportResponse(
    val id: Long,
    val reportedId: Long,
    val reportedNickname: String?,
    val potId: Long,
    val type: String,
    val reason: String?,
    /** 처리 상태 */
    val status: String?,
    val processedAt: String?,
    val createdAt: String?
)
