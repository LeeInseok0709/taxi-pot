package com.taxipot.global.security;

/**
 * STOMP /topic/pots/{potId} 구독 허용 검사. 구현: domain.pot (참여자 여부 + 미수금 이용 제한).
 * 허용하지 않으면 BusinessException 을 던진다.
 */
public interface PotSubscriptionGuard {

    void checkSubscribe(Long userId, Long potId);
}
