package com.taxipot.domain.chat.catalog;

import com.taxipot.domain.chat.dto.QuickBlockResponse;

import java.util.List;
import java.util.Optional;

/**
 * 원터치 퀵블록 문구 (서버 상수 — 앱 표시와 서버 검증을 한 곳에서). msgType = QUICK 이면 content 가 이 목록에 있어야 함.
 * <p>명세: '도착했어요', '조금 늦어요', '곧 도착해요', '곧 출발해요' 4개. 문구를 바꾸려면 명세에 먼저 적고 여기에 넣는다.
 * <p>앱은 버튼을 직접 박지 않고 GET /api/quick-blocks 로 받아서 그린다.
 */
public final class QuickBlockCatalog {

    public static final List<QuickBlockResponse> BLOCKS = List.of(
            new QuickBlockResponse("ARRIVED", "도착했어요"),
            new QuickBlockResponse("LATE", "조금 늦어요"),
            new QuickBlockResponse("ARRIVING_SOON", "곧 도착해요"),
            new QuickBlockResponse("DEPARTING_SOON", "곧 출발해요")
    );

    private QuickBlockCatalog() {
    }

    public static Optional<QuickBlockResponse> findByContent(String content) {
        return BLOCKS.stream().filter(b -> b.content().equals(content)).findFirst();
    }
}
