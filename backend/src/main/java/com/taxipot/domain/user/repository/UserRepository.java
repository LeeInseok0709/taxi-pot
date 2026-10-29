package com.taxipot.domain.user.repository;

import com.taxipot.domain.user.entity.User;
import com.taxipot.global.security.AuthUserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    boolean existsByUniversityAndStudentNo(String university, String studentNo);

    /** 락 순서 1단계: USERS (본인 또는 대상 회원) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    /** 여러 회원을 다룰 때: 반드시 id 오름차순으로 잠근다 (데드락 방지) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id in :ids order by u.id asc")
    List<User> findAllByIdInForUpdate(@Param("ids") Collection<Long> ids);

    /** 인증 필터가 매 요청 호출 (가벼운 조회) */
    @Query("select new com.taxipot.global.security.AuthUserStatus(u.id, u.role, u.status, u.tokenVersion) "
            + "from User u where u.id = :id")
    Optional<AuthUserStatus> findAuthStatus(@Param("id") Long id);
}
