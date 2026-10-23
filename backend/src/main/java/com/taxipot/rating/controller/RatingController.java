package com.taxipot.rating.controller;

import com.taxipot.global.auth.LoginUserId;
import com.taxipot.rating.dto.RatingCreateRequest;
import com.taxipot.rating.dto.RatingResponse;
import com.taxipot.rating.dto.RatingTargetResponse;
import com.taxipot.rating.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 자동 생성 (명세 3.9.0). 담당: 효우
 * 컨트롤러는 수정하지 않는다 — 경로·파라미터·응답 타입이 명세와 1:1. 로직은 RatingService에만 작성.
 */
@Tag(name = "Rating")
@RestController
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    /** GET /api/pots/{potId}/ratings/targets — 담당 효우 */
    @Operation(summary = "평가 대상 동승자 목록 조회")
    @GetMapping(value = "/api/pots/{potId}/ratings/targets")
    public ResponseEntity<List<RatingTargetResponse>> getRatingTargetList(
            @LoginUserId Long userId,
            @PathVariable Long potId) {
        return ResponseEntity.ok(ratingService.getRatingTargetList(userId, potId));
    }

    /** POST /api/pots/{potId}/ratings — 담당 효우 */
    @Operation(summary = "동승자 평가 제출 (여러 명 한 번에)")
    @PostMapping(value = "/api/pots/{potId}/ratings")
    public ResponseEntity<List<RatingResponse>> createRatings(
            @LoginUserId Long userId,
            @PathVariable Long potId,
            @Valid @RequestBody RatingCreateRequest request) {
        return ResponseEntity.ok(ratingService.createRatings(userId, potId, request));
    }
}
