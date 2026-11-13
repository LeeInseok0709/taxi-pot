package com.taxipot.domain.pot.service;

import com.taxipot.domain.pot.dto.PotPageResponse;
import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.repository.TaxipotRepository;
import com.taxipot.domain.user.entity.User;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.error.Errors;
import com.taxipot.global.response.PageMeta;
import com.taxipot.global.response.Paging;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 담당: 택시팟
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class AdminPotService {

    private final TaxipotRepository taxipotRepository;
    private final PotMapper potMapper;

    /**
     * <b>GET /api/admin/pots</b> — 전체 택시팟 이용 기록 조회
     * <p>
     * status 미지정 시 전체 상태. from·to 는 출발 예정일 기준(양 끝 포함, KST). 최신 출발순.
     * <p>
     * <b>에러 코드</b>
     * - 403: FORBIDDEN (SecurityConfig 가 /api/admin/** 을 ADMIN 으로 막는다)
     */
    @Transactional(readOnly = true)
    public PotPageResponse getPots(Long adminId, PotStatus status, LocalDate from, LocalDate to, int page, int size) {
        if (from != null && to != null && from.isAfter(to)) {
            throw Errors.invalidInput("from 은 to 보다 늦을 수 없습니다.");
        }
        Specification<Taxipot> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("departureTime"), from.atStartOfDay()));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("departureTime"), to.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Taxipot> pots = taxipotRepository.findAll(spec,
                Paging.of(page, size, Sort.by(Sort.Direction.DESC, "departureTime", "id")));

        Map<Long, User> hosts = potMapper.usersById(pots.getContent().stream().map(Taxipot::getHostId).toList());
        return new PotPageResponse(
                pots.map(p -> potMapper.toSummary(p, hosts.get(p.getHostId()), null)).getContent(),
                PageMeta.from(pots));
    }
}
