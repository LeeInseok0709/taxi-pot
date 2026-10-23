package com.taxipot.rating.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 명세 3.9.0 스키마 RatingCreateRequest (Request — @Valid로 검증).
 * 자동 생성 — 필드 이름·타입·순서 변경 금지 (앱 데이터 클래스와 1:1). 정적 팩토리(from 등)는 추가 가능.
 */
public record RatingCreateRequest(
        @NotNull @Size(min = 1, max = 3) List<@Valid RatingItem> ratings
) {}
