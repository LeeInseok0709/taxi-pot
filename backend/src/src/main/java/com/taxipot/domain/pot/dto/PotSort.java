package com.taxipot.domain.pot.dto;

/** GET /api/pots ?sort= (명세 인라인 enum). DISTANCE 는 lat·lng 가 있을 때만 의미 있음 */
public enum PotSort {
    DEPARTURE_TIME,
    DISTANCE,
    HOST_RATING
}
