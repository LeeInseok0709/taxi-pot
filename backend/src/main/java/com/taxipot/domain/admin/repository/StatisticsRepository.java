package com.taxipot.domain.admin.repository;

import com.taxipot.domain.pot.entity.Taxipot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 담당: 회원·운영 — 운영자 통계 집계 쿼리 (인석 구현)
 * <p>TAXIPOT 을 읽기만 하는 집계 전용 저장소. 택시팟 담당의 TaxipotRepository 를 건드리지 않으려고 따로 두었다
 * (같은 Entity 에 Repository 를 여러 개 두는 것은 Spring Data 에서 허용된다).
 * <p>기간은 출발 시각(departure_time) 기준 [from, to) 반열린 구간. 결과 행은 Object[] {이름 또는 값, 건수(Long)}.
 */
public interface StatisticsRepository extends Repository<Taxipot, Long> {

    /** 상태별 방 개수 (CANCELED 포함 전체) → {PotStatus, Long} */
    @Query("select p.status, count(p) from Taxipot p "
            + "where p.departureTime >= :from and p.departureTime < :to "
            + "group by p.status")
    List<Object[]> countByStatus(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** 주요 출발지 순위 (CANCELED 제외, 많은 순 → 이름순). 개수는 Pageable 로 자른다 → {String, Long} */
    @Query("select p.departureName, count(p) from Taxipot p "
            + "where p.departureTime >= :from and p.departureTime < :to "
            + "and p.status <> com.taxipot.global.code.PotStatus.CANCELED "
            + "group by p.departureName "
            + "order by count(p) desc, p.departureName asc")
    List<Object[]> topDepartures(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable limit);

    /** 선호 목적지 순위 (CANCELED 제외) → {String, Long} */
    @Query("select p.destinationName, count(p) from Taxipot p "
            + "where p.departureTime >= :from and p.departureTime < :to "
            + "and p.status <> com.taxipot.global.code.PotStatus.CANCELED "
            + "group by p.destinationName "
            + "order by count(p) desc, p.destinationName asc")
    List<Object[]> topDestinations(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable limit);

    /** 출발 시각의 '시'(0~23)별 방 개수 (CANCELED 제외). 방이 없는 시간대는 행이 없다 → {Integer, Long} */
    @Query("select extract(hour from p.departureTime), count(p) from Taxipot p "
            + "where p.departureTime >= :from and p.departureTime < :to "
            + "and p.status <> com.taxipot.global.code.PotStatus.CANCELED "
            + "group by extract(hour from p.departureTime)")
    List<Object[]> countByHour(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
