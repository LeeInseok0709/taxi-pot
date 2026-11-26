package com.taxipot.domain.user.repository;

import com.taxipot.domain.user.entity.User;
import com.taxipot.global.security.AuthUserStatus;
import com.taxipot.global.code.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    /**
     * 스케줄러 5 (정지 만료 해제) 대상 회원 id (락 없음, 오름차순).
     * <p>SUSPENDED 이고 해제 시각이 지난 회원. suspended_until 이 NULL(영구 정지)이면 제외 — 영구 정지는 운영자만 해제한다.
     * 실제 해제는 MemberJobProcessor 가 회원 1명씩 잠근 뒤 조건을 다시 확인하고 한다.
     */
    @Query("select u.id from User u "
            + "where u.status = com.taxipot.global.code.UserStatus.SUSPENDED "
            + "and u.suspendedUntil is not null and u.suspendedUntil <= :now "
            + "order by u.id asc")
    List<Long> findIdsToReleaseSuspension(@Param("now") LocalDateTime now);

    /**
     * 스케줄러 6 (탈퇴 회원 개인정보 파기) 대상 회원 id (락 없음, 오름차순).
     * <p>WITHDRAWN 이고 탈퇴 시각이 기준(now − 14일) 이전이며, 아직 파기 전인 회원.
     * '파기 전'은 email 이 남아 있는지로 판단한다 (탈퇴 전에는 DB CHECK 로 email 이 반드시 있고, 파기하면 NULL 이 된다).
     */
    @Query("select u.id from User u "
            + "where u.status = com.taxipot.global.code.UserStatus.WITHDRAWN "
            + "and u.withdrawnAt is not null and u.withdrawnAt <= :threshold "
            + "and u.email is not null "
            + "order by u.id asc")
    List<Long> findIdsToPurge(@Param("threshold") LocalDateTime threshold);

    /**
     * 운영자 회원 목록 (인석 추가). keyword: 이름·닉네임·이메일·학번 부분 일치, status: 상태 필터. 둘 다 null 이면 전체.
     * <p>전화번호는 암호화 컬럼이라 검색 대상에서 뺀다 (명세도 이름·닉네임·이메일·학번만).
     * keyword 는 서비스에서 '%...%' 로 감싸서 넘긴다. 입력에 섞인 %·_ 는 서비스가 '!' 로 escape 한다.
     */
    @Query("select u from User u "
            + "where (:status is null or u.status = :status) "
            + "and (:keyword is null "
            + "     or u.name like :keyword escape '!' or u.nickname like :keyword escape '!' "
            + "     or u.email like :keyword escape '!' or u.studentNo like :keyword escape '!')")
    Page<User> searchForAdmin(@Param("keyword") String keyword, @Param("status") UserStatus status, Pageable pageable);
}
