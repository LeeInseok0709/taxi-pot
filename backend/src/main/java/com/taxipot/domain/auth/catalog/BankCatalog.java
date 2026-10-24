package com.taxipot.domain.auth.catalog;

import com.taxipot.domain.auth.dto.BankResponse;

import java.util.Comparator;
import java.util.List;

/** 출금 은행 목록 (서버 상수, 이름순). 코드는 금융결제원 표준 은행 코드 */
public final class BankCatalog {

    public static final List<BankResponse> BANKS = List.of(
            new BankResponse("002", "KDB산업은행"),
            new BankResponse("003", "IBK기업은행"),
            new BankResponse("004", "KB국민은행"),
            new BankResponse("007", "수협은행"),
            new BankResponse("011", "NH농협은행"),
            new BankResponse("020", "우리은행"),
            new BankResponse("023", "SC제일은행"),
            new BankResponse("027", "한국씨티은행"),
            new BankResponse("031", "iM뱅크"),
            new BankResponse("032", "부산은행"),
            new BankResponse("034", "광주은행"),
            new BankResponse("035", "제주은행"),
            new BankResponse("037", "전북은행"),
            new BankResponse("039", "경남은행"),
            new BankResponse("045", "새마을금고"),
            new BankResponse("048", "신협"),
            new BankResponse("071", "우체국"),
            new BankResponse("081", "하나은행"),
            new BankResponse("088", "신한은행"),
            new BankResponse("089", "케이뱅크"),
            new BankResponse("090", "카카오뱅크"),
            new BankResponse("092", "토스뱅크")
    ).stream().sorted(Comparator.comparing(BankResponse::name)).toList();

    private BankCatalog() {
    }

    public static boolean exists(String code) {
        return BANKS.stream().anyMatch(b -> b.code().equals(code));
    }
}
