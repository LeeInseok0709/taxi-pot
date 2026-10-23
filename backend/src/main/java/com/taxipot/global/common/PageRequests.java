package com.taxipot.global.common;

import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * 컨트롤러에서 Pageable 파라미터를 직접 받지 않는다. 이유:
 * GET /api/pots의 sort=DEPARTURE_TIME|DISTANCE|HOST_RATING 같은 명세 값을 Spring이 Entity 필드명으로 해석해서
 * PropertyReferenceException(500)이 난다.
 *
 * @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size
 * → PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
 */
public final class PageRequests {

    public static final int MAX_SIZE = 100;

    private PageRequests() {
    }

    public static PageRequest of(int page, int size) {
        return of(page, size, Sort.unsorted());
    }

    public static PageRequest of(int page, int size, Sort sort) {
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "page는 0 이상, size는 1~100이어야 합니다.", null);
        }
        return PageRequest.of(page, size, sort);
    }
}
