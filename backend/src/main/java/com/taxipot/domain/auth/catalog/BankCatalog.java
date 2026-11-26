package com.taxipot.domain.auth.catalog;

import com.taxipot.domain.auth.dto.BankResponse;

import java.util.Comparator;
import java.util.List;

/** 출금 은행 목록 (서버 상수, 이름순). 지원 은행 6곳, 코드는 금융결제원 표준 은행 코드 */
public final class BankCatalog {

    public static final List<BankResponse> BANKS = List.of(
            new BankResponse("004", "KB국민은행"),
            new BankResponse("011", "NH농협은행"),
            new BankResponse("088", "신한은행"),
            new BankResponse("020", "우리은행"),
            new BankResponse("090", "카카오뱅크"),
            new BankResponse("092", "토스뱅크")
    ).stream().sorted(Comparator.comparing(BankResponse::name)).toList();

    private BankCatalog() {
    }

    public static boolean exists(String code) {
        return BANKS.stream().anyMatch(b -> b.code().equals(code));
    }
}
