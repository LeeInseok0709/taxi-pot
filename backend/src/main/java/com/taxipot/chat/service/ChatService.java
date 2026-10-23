package com.taxipot.chat.service;

import com.taxipot.chat.dto.ChatMessagePageResponse;
import com.taxipot.chat.dto.ChatMessageRequest;
import com.taxipot.chat.dto.ChatMessageResponse;
import com.taxipot.chat.dto.QuickBlockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * ChatController 전용 API 서비스. 담당: 인석
 * - 메서드 시그니처(이름·파라미터·반환 타입) 변경 금지. 본문만 구현한다. private 메서드는 자유롭게 추가.
 * - 주입 가능: 모든 도메인의 XxxManager, Repository(다른 도메인은 조회·락만), AppProperties, FileStorage
 * - 주입 금지: 다른 XxxService (API 서비스끼리 주입하면 순환 참조로 서버가 안 뜰 수 있음)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    /**
     * GET /api/pots/{potId}/messages — 채팅 메시지 조회 (이전 메시지, 무한 스크롤) (담당 인석)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 커서 기반 페이징 (최신순). 방 참여자(JOINED·BOARDED)만 조회 가능, 이용 정지 중에도 조회 가능.
     * 실시간 수신은 STOMP `/topic/pots/{potId}` 구독 후 `CHAT_MESSAGE` 메시지. 자체 채팅 대신 오픈채팅(open_chat_url)으로 대체 가능.
     */
    public ChatMessagePageResponse getMessages(Long userId, Long potId, Long cursor, Integer size) {
        throw new UnsupportedOperationException("TODO(인석): GET /api/pots/{potId}/messages");
    }

    /**
     * POST /api/pots/{potId}/messages — 채팅 메시지·퀵블록 전송 (담당 인석)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * CHAT_MESSAGE 저장 후 STOMP `/topic/pots/{potId}`로 브로드캐스트.
     * - msgType = QUICK이면 `/api/quick-blocks`에 정의된 문구만 허용
     * - 이용 정지 중에도 전송 가능, 방 상태 COMPLETED·CANCELED이면 전송 불가
     */
    @Transactional
    public ChatMessageResponse sendMessage(Long userId, Long potId, ChatMessageRequest request) {
        throw new UnsupportedOperationException("TODO(인석): POST /api/pots/{potId}/messages");
    }

    /**
     * GET /api/quick-blocks — 퀵블록 문구 목록 조회 (담당 인석)
     * 아래는 명세 3.9.0 설명 원문. 이대로 구현한다.
     * 서버 상수로 관리하는 원터치 문구 ('1분 늦어요' ~ '5분 늦어요', '도착했어요' 등). 앱과 서버 검증 목록을 한 곳에서 관리
     */
    public List<QuickBlockResponse> getQuickBlockList(Long userId) {
        throw new UnsupportedOperationException("TODO(인석): GET /api/quick-blocks");
    }
}
