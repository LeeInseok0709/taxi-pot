package com.taxipot.domain.chat.catalog;

import com.taxipot.domain.chat.dto.QuickBlockResponse;

import java.util.List;
import java.util.Optional;

/**
 * 원터치 퀵블록 문구 (서버 상수 — 앱 표시와 서버 검증을 한 곳에서). msgType = QUICK 이면 content 가 이 목록에 있어야 함.
 * <p>명세: '1분 늦어요' ~ '5분 늦어요', '도착했어요' 등. 문구를 추가하려면 명세에 먼저 적고 여기에 넣는다.
 * code 값(LATE_1 등)은 명세에 정해지지 않아 틀 작성 시 붙인 이름이다.
 */
public final class QuickBlockCatalog {

    public static final List<QuickBlockResponse> BLOCKS = List.of(
            new QuickBlockResponse("LATE_1", "1분 늦어요"),
            new QuickBlockResponse("LATE_2", "2분 늦어요"),
            new QuickBlockResponse("LATE_3", "3분 늦어요"),
            new QuickBlockResponse("LATE_4", "4분 늦어요"),
            new QuickBlockResponse("LATE_5", "5분 늦어요"),
            new QuickBlockResponse("ARRIVED", "도착했어요")
    );

    private QuickBlockCatalog() {
    }

    public static Optional<QuickBlockResponse> findByContent(String content) {
        return BLOCKS.stream().filter(b -> b.content().equals(content)).findFirst();
    }
}
