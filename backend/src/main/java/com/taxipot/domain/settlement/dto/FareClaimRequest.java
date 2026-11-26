package com.taxipot.domain.settlement.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * FareClaimRequest — API 명세서 3.9.0 components.schemas.FareClaimRequest
 * <p>multipart/form-data (3.3.0)
 * <p>자동 생성 파일: 직접 수정하지 말고 명세 변경 후 tools/gen_dto.py 로 재생성 (docs/CONVENTIONS.md 4장)
 */
public record FareClaimRequest(
        // 영수증 기준 실제 택시 요금(원), 텍스트 파트 (3.5.0 실요금 청구 요청)
        @NotNull @Min(0) @Max(500000) Integer actualFare,
        // 택시 결제내역(영수증) 사진 jpg/png, 10MB 이하 (필수)
        @NotNull MultipartFile receiptImage
) {
}
