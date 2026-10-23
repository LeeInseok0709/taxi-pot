package com.taxipot.user.controller;

import com.taxipot.user.dto.BankResponse;
import com.taxipot.user.dto.UniversityResponse;
import com.taxipot.user.service.ReferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 자동 생성 (명세 3.9.0). 담당: 기성
 * 컨트롤러는 수정하지 않는다 — 경로·파라미터·응답 타입이 명세와 1:1. 로직은 ReferenceService에만 작성.
 */
@Tag(name = "Auth")
@RestController
@RequiredArgsConstructor
public class ReferenceController {

    private final ReferenceService referenceService;

    /** GET /api/universities — 담당 기성 */
    @Operation(summary = "가입 가능 대학교 목록 조회 (3.3.0)")
    @GetMapping(value = "/api/universities")
    public ResponseEntity<List<UniversityResponse>> getUniversityList() {
        return ResponseEntity.ok(referenceService.getUniversityList());
    }

    /** GET /api/banks — 담당 기성 */
    @Operation(summary = "출금 계좌 은행 목록 조회 (3.5.0)")
    @GetMapping(value = "/api/banks")
    public ResponseEntity<List<BankResponse>> getBankList() {
        return ResponseEntity.ok(referenceService.getBankList());
    }
}
