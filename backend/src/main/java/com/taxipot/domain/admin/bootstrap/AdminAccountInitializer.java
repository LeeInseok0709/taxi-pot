package com.taxipot.domain.admin.bootstrap;

import com.taxipot.domain.university.entity.University;
import com.taxipot.domain.university.repository.UniversityRepository;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.user.repository.UserRepository;
import com.taxipot.domain.wallet.service.WalletLedger;
import com.taxipot.global.config.AppProperties;
import com.taxipot.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영자 계정이 없으면 app.admin.email/password 로 만든다 (명세: 운영자 계정 1개).
 * 휴대폰·계좌는 서버 키로 암호화돼야 하므로 SQL 로 넣지 않고 서버가 생성한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private final AppProperties props;
    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final WalletLedger walletLedger;
    private final PasswordEncoder passwordEncoder;
    private final TimeProvider timeProvider;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = props.admin().email();
        if (email == null || email.isBlank() || userRepository.existsByEmail(email)) {
            return;
        }
        University university = universityRepository.findAllByIsActiveTrueOrderByNameAsc().stream().findFirst().orElse(null);
        if (university == null) {
            log.warn("UNIVERSITY 가 비어 있어 운영자 계정을 만들지 않았습니다 (db/schema.sql 의 INSERT 확인)");
            return;
        }
        User admin = userRepository.save(User.createAdmin(email, passwordEncoder.encode(props.admin().password()),
                university.getName(), timeProvider.now()));
        walletLedger.open(admin.getId());
        log.info("운영자 계정 생성: {}", email);
    }
}
