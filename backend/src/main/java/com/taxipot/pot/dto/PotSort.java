package com.taxipot.pot.dto;

/** GET /api/pots의 sort 값 (명세 3.9.0). DISTANCE는 lat·lng를 같이 보낼 때만 사용 가능 */
public enum PotSort {
    DEPARTURE_TIME,
    DISTANCE,
    HOST_RATING
}
