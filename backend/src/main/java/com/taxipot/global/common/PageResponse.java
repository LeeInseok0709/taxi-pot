package com.taxipot.global.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * 명세 XxxPageResponse 공통 형식: { "content": [...], "page": { page, size, totalElements, totalPages } }
 * 예외: 채팅 목록은 커서 방식(messages, nextCursor, hasNext)이라 이걸 쓰지 않는다.
 *
 * return PageResponse.of(potPage, PotSummaryResponse::from);   // Page<Entity> → DTO 변환까지 한 번에
 */
public record PageResponse<T>(List<T> content, PageMeta page) {

    public record PageMeta(int page, int size, long totalElements, int totalPages) {}

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(),
                new PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()));
    }

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return of(page.map(mapper));
    }
}
