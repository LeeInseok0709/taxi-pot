package com.taxipot.user.service;

import com.taxipot.user.dto.BankResponse;
import com.taxipot.user.dto.UniversityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * ReferenceController 전용 API 서비스. 담당: 기성
 * - 메서드 시그니처(이름·파라미터·반환 타입) 변경 금지. 본문만 구현한다. private 메서드는 자유롭게 추가.
 * - 주입 가능: 모든 도메인의 XxxManager, Repository(다른 도메인은 조회·락만), AppProperties, FileStorage
 * - 주입 금지: 다른 XxxService (API 서비스끼리 주입하면 순환 참조로 서버가 안 뜰 수 있음)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReferenceService {

    /**
     * GET /api/universities — 가입 가능 대학교 목록 조회 (3.3.0) (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * UNIVERSITY에서 is_active = 1인 학교를 이름순으로 반환. 가입 화면의 학교 선택 목록으로 사용하고,
     * SignupRequest.university에는 응답의 name을 그대로 보낸다. 이메일 입력란 뒤에 '@' + emailDomain을 안내하면 도메인 오류를 줄일 수 있다.
     */
    public List<UniversityResponse> getUniversityList() {
        throw new UnsupportedOperationException("TODO(기성): GET /api/universities");
    }

    /**
     * GET /api/banks — 출금 계좌 은행 목록 조회 (3.5.0) (담당 기성)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 가입·계좌 변경 화면의 은행 선택 목록. 서버 상수(은행 코드·이름)를 이름순으로 반환
     */
    public List<BankResponse> getBankList() {
        throw new UnsupportedOperationException("TODO(기성): GET /api/banks");
    }
}
