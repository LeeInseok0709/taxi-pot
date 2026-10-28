package com.taxipot.domain.chat.service;

import com.taxipot.domain.chat.catalog.QuickBlockCatalog;
import com.taxipot.domain.chat.dto.ChatMessagePageResponse;
import com.taxipot.domain.chat.dto.ChatMessageRequest;
import com.taxipot.domain.chat.dto.ChatMessageResponse;
import com.taxipot.domain.chat.dto.QuickBlockResponse;
import com.taxipot.global.error.NotImplementedYetException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 담당: 택시팟
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    /**
     * <b>GET /api/pots/{potId}/messages</b> — 채팅 메시지 조회 (이전 메시지, 무한 스크롤)
     * <p>
     * 커서 기반 페이징 (최신순). 방 참여자(JOINED·BOARDED)만 조회 가능, 이용 정지 중에도 조회 가능.
     * 실시간 수신은 STOMP `/topic/pots/{potId}` 구독 후 `CHAT_MESSAGE` 메시지. 자체 채팅 대신 오픈채팅(open_chat_url)으로 대체 가능.
     * <p>
     * <b>에러 코드</b>
     * - 403: NOT_POT_MEMBER
     */
    @Transactional(readOnly = true)
    public ChatMessagePageResponse getMessages(Long userId, Long potId, Long cursor, int size) {
        // TODO(택시팟): 위 명세대로 구현
        throw new NotImplementedYetException("GET /api/pots/{potId}/messages");
    }

    /**
     * <b>POST /api/pots/{potId}/messages</b> — 채팅 메시지·퀵블록 전송
     * <p>
     * CHAT_MESSAGE 저장 후 STOMP `/topic/pots/{potId}`로 브로드캐스트.
     * - msgType = QUICK이면 `/api/quick-blocks`에 정의된 문구만 허용
     * - 이용 정지 중에도 전송 가능, 방 상태 COMPLETED·CANCELED이면 전송 불가
     * <p>
     * <b>에러 코드</b>
     * - 400: INVALID_QUICK_MESSAGE
     * - 403: NOT_POT_MEMBER
     * - 409: INVALID_POT_STATUS
     */
    @Transactional
    public ChatMessageResponse sendMessage(Long userId, Long potId, ChatMessageRequest request) {
        // TODO(택시팟): 위 명세대로 구현
        throw new NotImplementedYetException("POST /api/pots/{potId}/messages");
    }

    /**
     * <b>GET /api/quick-blocks</b> — 퀵블록 문구 목록 조회
     * <p>
     * 서버 상수로 관리하는 원터치 문구 ('1분 늦어요' ~ '5분 늦어요', '도착했어요' 등). 앱과 서버 검증 목록을 한 곳에서 관리
     */
    @Transactional(readOnly = true)
    public List<QuickBlockResponse> getQuickBlocks(Long userId) {
        return QuickBlockCatalog.BLOCKS;
    }
}
