package com.taxipot.user.repository;

import com.taxipot.user.entity.User;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 조회 메서드 추가는 user 담당. 다른 도메인 담당은 조회·락 용도로만 주입 (쓰기는 그 도메인 Manager 경유). */
public interface UserRepository extends JpaRepository<User, Long> {

    /** 락 순서 1번. 명세 '동시성 제어': USERS → WALLET → TAXIPOT */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    /** 여러 회원(정산·방 취소·스케줄러): id 오름차순으로 잠근다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select u from User u where u.id in :ids order by u.id")
    List<User> findAllByIdInForUpdate(@Param("ids") Collection<Long> ids);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);
}
