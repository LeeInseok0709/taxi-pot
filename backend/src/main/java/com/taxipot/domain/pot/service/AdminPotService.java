package com.taxipot.domain.pot.service;

import com.taxipot.domain.pot.dto.PotPageResponse;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.error.NotImplementedYetException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 담당: 택시팟
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class AdminPotService {

    /**
     * <b>GET /api/admin/pots</b> — 전체 택시팟 이용 기록 조회
     * <p>
     * <b>에러 코드</b>
     * - 403: FORBIDDEN
     */
    @Transactional(readOnly = true)
    public PotPageResponse getPots(Long adminId, PotStatus status, LocalDate from, LocalDate to, int page, int size) {
        // TODO(택시팟): 위 명세대로 구현
        throw new NotImplementedYetException("GET /api/admin/pots");
    }
}
