package com.taxipot.domain.verification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

/**
 * CaptureSubmitRequest — API 명세서 3.9.0 components.schemas.CaptureSubmitRequest
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record CaptureSubmitRequest(
        // 종합정보시스템 학생 기본 정보 캡처 이미지 (jpg/png)
        @NotNull MultipartFile captureImage,
        @NotBlank @Size(max = 20) String studentNo
) {
}
