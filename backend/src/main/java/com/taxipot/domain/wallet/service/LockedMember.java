package com.taxipot.domain.wallet.service;

import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.wallet.entity.Wallet;

/** 락을 잡은 회원 행 + 지갑 행 */
public record LockedMember(User user, Wallet wallet) {
}
