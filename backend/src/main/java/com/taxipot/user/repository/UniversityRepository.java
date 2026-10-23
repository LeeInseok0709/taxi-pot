package com.taxipot.user.repository;

import com.taxipot.user.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/** 조회 메서드 추가는 user 담당. 다른 도메인 담당은 조회·락 용도로만 주입 (쓰기는 그 도메인 Manager 경유). */
public interface UniversityRepository extends JpaRepository<University, Long> {

    Optional<University> findByName(String name);

    List<University> findByIsActiveTrueOrderByNameAsc();
}
