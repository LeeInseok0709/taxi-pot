package com.taxipot.domain.notification.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.taxipot.global.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * PushSender 빈은 여기서 <b>하나만</b> 만든다 (EmailCodeSenderConfig 와 같은 방식).
 * <ul>
 *   <li>app.push.enabled=false (기본) → {@link LogPushSender}: 로그만 출력</li>
 *   <li>app.push.enabled=true → {@link FcmPushSender}: app.push.credentials-path 의 서비스 계정 키로 FCM 발송</li>
 * </ul>
 * 예전처럼 @Component + @ConditionalOnMissingBean 을 쓰면 컴포넌트 스캔 순서에 따라 빈이 2개가 될 수 있어서 바꿨다.
 * 켰는데 키 파일이 없거나 잘못되면 서버 시작 때 바로 실패시킨다 (시연 중에 푸시가 조용히 안 나가는 것보다 낫다).
 */
@Slf4j
@Configuration
public class PushSenderConfig {

    static final String FIREBASE_APP_NAME = "taxipot";

    @Bean
    public PushSender pushSender(AppProperties props) {
        AppProperties.Push push = props.push();
        if (!push.enabled()) {
            log.info("푸시: 로그 출력 모드 (app.push.enabled=false)");
            return new LogPushSender();
        }
        FirebaseApp app = firebaseApp(push.credentialsPath());
        log.info("푸시: FCM 발송 모드 (키 파일 {})", push.credentialsPath());
        return new FcmPushSender(FirebaseMessaging.getInstance(app));
    }

    static FirebaseApp firebaseApp(String credentialsPath) {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            throw new IllegalStateException(
                    "app.push.enabled=true 인데 app.push.credentials-path(TAXIPOT_FCM_CREDENTIALS)가 비어 있습니다");
        }
        // 테스트·devtools 재시작으로 이미 초기화돼 있으면 그대로 쓴다
        for (FirebaseApp existing : FirebaseApp.getApps()) {
            if (existing.getName().equals(FIREBASE_APP_NAME)) {
                return existing;
            }
        }
        Path path = Path.of(credentialsPath);
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Firebase 서비스 계정 키 파일이 없습니다: " + path.toAbsolutePath());
        }
        try (InputStream in = Files.newInputStream(path)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .build();
            return FirebaseApp.initializeApp(options, FIREBASE_APP_NAME);
        } catch (IOException e) {
            throw new IllegalStateException("Firebase 서비스 계정 키를 읽지 못했습니다: " + path.toAbsolutePath(), e);
        }
    }
}
