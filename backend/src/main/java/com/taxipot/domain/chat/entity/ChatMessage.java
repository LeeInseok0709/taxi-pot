package com.taxipot.domain.chat.entity;

import com.taxipot.global.code.MsgType;
import com.taxipot.global.time.Kst;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** CHAT_MESSAGE (sent_at 만 있음) */
@Getter
@Entity
@Table(name = "CHAT_MESSAGE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "pot_id", nullable = false)
    private Long potId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "msg_type", nullable = false, length = 10)
    private MsgType msgType;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @PrePersist
    void onCreate() {
        sentAt = Kst.now();
    }

    public static ChatMessage of(Long potId, Long senderId, MsgType msgType, String content) {
        ChatMessage m = new ChatMessage();
        m.potId = potId;
        m.senderId = senderId;
        m.msgType = msgType == null ? MsgType.TEXT : msgType;
        m.content = content;
        return m;
    }
}
