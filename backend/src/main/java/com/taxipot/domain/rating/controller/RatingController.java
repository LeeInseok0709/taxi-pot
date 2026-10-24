package com.taxipot.domain.rating.controller;

import com.taxipot.domain.rating.dto.RatingCreateRequest;
import com.taxipot.domain.rating.dto.RatingResponse;
import com.taxipot.domain.rating.dto.RatingTargetResponse;
import com.taxipot.domain.rating.service.RatingService;
import com.taxipot.global.security.LoginUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 명세 tag: Rating — 자동 생성 (tools/gen_api.py). 컨트롤러에는 로직을 넣지 않는다:
 * 요청 검증(@Valid)·서비스 호출·응답 변환만. 경로·파라미터가 명세와 다르면 명세를 먼저 고친다.
 */
@Tag(name = "Rating")
@RestController
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @Operation(summary = "평가 대상 동승자 목록 조회")
    @GetMapping("/api/pots/{potId}/ratings/targets")
    public List<RatingTargetResponse> getRatingTargets(@LoginUserId Long userId, @PathVariable Long potId) {
        return ratingService.getRatingTargets(userId, potId);
    }

    @Operation(summary = "동승자 평가 제출 (여러 명 한 번에)")
    @PostMapping("/api/pots/{potId}/ratings")
    public List<RatingResponse> submitRatings(@LoginUserId Long userId, @PathVariable Long potId, @Valid @RequestBody RatingCreateRequest request) {
        return ratingService.submitRatings(userId, potId, request);
    }
}
