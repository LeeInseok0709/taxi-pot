package com.taxipot.domain.chat.dto;

import java.util.List;

/**
 * ChatMessagePageResponse — API 명세서 3.9.0 components.schemas.ChatMessagePageResponse
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record ChatMessagePageResponse(
        List<ChatMessageResponse> messages,
        // nullable / 다음 조회용 커서, 더 없으면 null
        Long nextCursor,
        Boolean hasNext
) {
}
