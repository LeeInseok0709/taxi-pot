package com.taxipot.global.response;

import org.springframework.data.domain.Page;

/**
 * 명세 components.schemas.PageMeta. 목록 응답 = {@code XxxPageResponse(content, page)}.
 * <pre>
 * Page&lt;Notification&gt; result = repository.findBy...(memberId, Paging.of(page, size, Sort.by(DESC, "createdAt", "id")));
 * return new NotificationPageResponse(result.map(this::toResponse).getContent(), PageMeta.from(result));
 * </pre>
 */
public record PageMeta(
        Integer page,
        Integer size,
        Long totalElements,
        Integer totalPages
) {
    public static PageMeta from(Page<?> page) {
        return new PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
