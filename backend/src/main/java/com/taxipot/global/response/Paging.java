package com.taxipot.global.response;

import com.taxipot.global.error.Errors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * page·size 검사 + PageRequest 생성.
 * <p>컨트롤러에서 Spring 의 Pageable 파라미터를 받지 않는다: 명세의 sort 값(DEPARTURE_TIME 등)을
 * Entity 필드명으로 해석하려다 500 이 나고, size 상한도 없다. 정렬은 서비스가 Sort 로 직접 정한다.
 */
public final class Paging {

    public static final int MAX_SIZE = 100;

    private Paging() {
    }

    public static Pageable of(int page, int size, Sort sort) {
        if (page < 0) {
            throw Errors.invalidInput("page 는 0 이상이어야 합니다.");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw Errors.invalidInput("size 는 1~" + MAX_SIZE + " 사이여야 합니다.");
        }
        return PageRequest.of(page, size, sort);
    }
}
