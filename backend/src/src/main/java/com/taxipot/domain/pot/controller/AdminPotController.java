package com.taxipot.domain.pot.controller;

import com.taxipot.domain.pot.dto.PotPageResponse;
import com.taxipot.domain.pot.service.AdminPotService;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.security.LoginUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 명세 tag: Admin — 자동 생성 (tools/gen_api.py). 컨트롤러에는 로직을 넣지 않는다:
 * 요청 검증(@Valid)·서비스 호출·응답 변환만. 경로·파라미터가 명세와 다르면 명세를 먼저 고친다.
 */
@Tag(name = "Admin")
@RestController
@RequiredArgsConstructor
public class AdminPotController {

    private final AdminPotService adminPotService;

    @Operation(summary = "전체 택시팟 이용 기록 조회")
    @GetMapping("/api/admin/pots")
    public PotPageResponse getPots(@LoginUserId Long adminId, @RequestParam(required = false) PotStatus status, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return adminPotService.getPots(adminId, status, from, to, page, size);
    }
}
