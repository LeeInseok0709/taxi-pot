package com.taxipot.chat.entity;

import com.taxipot.global.code.MsgType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

/**
 * CHAT_MESSAGE (DB 설계서 13차). 컬럼·타입은 설계서와 1:1 — 필드 추가·삭제·타입 변경 금지.
 * - 다른 테이블 FK는 전부 Long id 필드 (연관관계 매핑 안 함)
 * - DB DEFAULT가 있는 컬럼은 Java 필드 초기값으로도 같은 값을 넣어 둠 (JPA는 NULL을 명시적으로 INSERT하므로)
 * - 상태 변경은 아래 '상태 변경 메서드' 영역에 의미 있는 이름으로 추가 (Setter 금지). 추가는 chat 담당만.
 */
@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "CHAT_MESSAGE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 택시팟 번호 */
    @Column(name = "pot_id", nullable = false)
    private Long potId;

    /** 발신 회원번호 */
    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    /** 메시지 유형 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "msg_type", nullable = false, length = 10)
    private MsgType msgType = MsgType.TEXT;

    /** 메시지 내용 */
    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    /** 전송 일시 */
    @CreatedDate
    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @Builder
    private ChatMessage(Long potId, Long senderId, MsgType msgType, String content) {
        this.potId = potId;
        this.senderId = senderId;
        if (msgType != null) {
            this.msgType = msgType;
        }
        this.content = content;
    }

    // ===== 상태 변경 메서드 (chat 담당이 추가) =====
}
