package com.taxipot.domain.chat.dto;

import com.taxipot.global.code.MsgType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ChatMessageRequest — API 명세서 3.9.0 components.schemas.ChatMessageRequest
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record ChatMessageRequest(
        // 선택
        MsgType msgType,
        @NotBlank @Size(max = 1000) String content
) {
}
