package com.taxipot.domain.chat.service;

import com.taxipot.domain.chat.catalog.QuickBlockCatalog;
import com.taxipot.domain.chat.dto.ChatMessagePageResponse;
import com.taxipot.domain.chat.dto.ChatMessageRequest;
import com.taxipot.domain.chat.dto.ChatMessageResponse;
import com.taxipot.domain.chat.dto.QuickBlockResponse;
import com.taxipot.domain.chat.entity.ChatMessage;
import com.taxipot.domain.chat.repository.ChatMessageRepository;
import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.realtime.PotRealtimePublisher;
import com.taxipot.domain.pot.repository.TaxipotMemberRepository;
import com.taxipot.domain.pot.repository.TaxipotRepository;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.user.repository.UserRepository;
import com.taxipot.global.code.MsgType;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.error.Errors;
import com.taxipot.global.time.Kst;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 담당: 택시팟
 * <p>메서드 주석 = 명세 3.12.0 의 설명·에러 코드.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 * <p>미수금 이용 제한(409 UNPAID_DEBT_EXISTS)은 DebtRestrictionInterceptor 가 처리하므로 여기서 검사하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    /** 채팅 조회·전송이 가능한 참여 상태 */
    private static final List<PotMemberStatus> CHAT_MEMBER = List.of(PotMemberStatus.JOINED, PotMemberStatus.BOARDED);
    /** 전송이 막히는 방 상태 */
    private static final Set<PotStatus> CLOSED_FOR_CHAT = Set.of(PotStatus.COMPLETED, PotStatus.CANCELED);
    /** 닉네임을 찾지 못한 보낸 사람 표시 (응답의 senderNickname 은 필수) */
    private static final String UNKNOWN_NICKNAME = "(알 수 없음)";
    /** size 상한 (컨트롤러 기본값 30) */
    private static final int MAX_PAGE_SIZE = 100;

    private final ChatMessageRepository chatMessageRepository;
    private final TaxipotRepository taxipotRepository;
    private final TaxipotMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final PotRealtimePublisher realtimePublisher;
    private final ChatPushNotifier chatPushNotifier;

    /**
     * <b>GET /api/pots/{potId}/messages</b> — 채팅 메시지 조회 (이전 메시지, 무한 스크롤)
     * <p>
     * 커서 기반 페이징 (최신순). 방 참여자(JOINED·BOARDED)만 조회 가능, 이용 정지 중에도 조회 가능.
     * 실시간 수신은 STOMP `/topic/pots/{potId}` 구독 후 `CHAT_MESSAGE` 메시지.
     * <p>
     * <b>에러 코드</b>
     * - 403: NOT_POT_MEMBER
     */
    @Transactional(readOnly = true)
    public ChatMessagePageResponse getMessages(Long userId, Long potId, Long cursor, int size) {
        taxipotRepository.findById(potId).orElseThrow(() -> Errors.notFound("택시팟"));
        checkMember(userId, potId);

        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        // 한 건 더 읽어서 다음 페이지가 있는지 판단
        PageRequest page = PageRequest.of(0, pageSize + 1);
        List<ChatMessage> rows = cursor == null
                ? chatMessageRepository.findAllByPotIdOrderByIdDesc(potId, page)
                : chatMessageRepository.findAllByPotIdAndIdLessThanOrderByIdDesc(potId, cursor, page);

        boolean hasNext = rows.size() > pageSize;
        List<ChatMessage> messages = hasNext ? rows.subList(0, pageSize) : rows;
        Long nextCursor = hasNext ? messages.get(messages.size() - 1).getId() : null;

        Map<Long, String> nicknames = nicknamesOf(messages.stream().map(ChatMessage::getSenderId).collect(Collectors.toSet()));
        List<ChatMessageResponse> body = messages.stream()
                .map(m -> toResponse(m, nicknames.get(m.getSenderId())))
                .toList();
        return new ChatMessagePageResponse(body, nextCursor, hasNext);
    }

    /**
     * <b>POST /api/pots/{potId}/messages</b> — 채팅 메시지·퀵블록 전송
     * <p>
     * CHAT_MESSAGE 저장 후 STOMP `/topic/pots/{potId}`로 브로드캐스트.
     * - msgType = QUICK이면 `/api/quick-blocks`에 정의된 문구만 허용
     * - 이용 정지 중에도 전송 가능, 방 상태 COMPLETED·CANCELED이면 전송 불가
     * - 보낸 사람을 제외한 참여자(JOINED·BOARDED) 전원에게 FCM 푸시 '새 메시지가 있어요' (알림 목록에는 저장하지 않음)
     * <p>
     * <b>에러 코드</b>
     * - 400: INVALID_QUICK_MESSAGE
     * - 403: NOT_POT_MEMBER
     * - 409: INVALID_POT_STATUS
     */
    @Transactional
    public ChatMessageResponse sendMessage(Long userId, Long potId, ChatMessageRequest request) {
        Taxipot pot = taxipotRepository.findById(potId).orElseThrow(() -> Errors.notFound("택시팟"));
        checkMember(userId, potId);
        if (CLOSED_FOR_CHAT.contains(pot.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_POT_STATUS);
        }

        MsgType type = request.msgType() == null ? MsgType.TEXT : request.msgType();
        String content = request.content() == null ? "" : request.content().strip();
        if (content.isEmpty()) {
            throw Errors.invalidInput("메시지 내용을 입력해 주세요.");
        }
        if (type == MsgType.QUICK && QuickBlockCatalog.findByContent(content).isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_QUICK_MESSAGE);
        }

        ChatMessage saved = chatMessageRepository.save(ChatMessage.of(potId, userId, type, content));
        String nickname = userRepository.findById(userId).map(User::getNickname).orElse(null);
        ChatMessageResponse response = toResponse(saved, nickname);

        // 커밋된 뒤 방송 (PotRealtimePublisher 가 afterCommit 으로 처리)
        realtimePublisher.chatMessage(potId, response);

        // 앱을 내려둔 참여자용 푸시 — 메시지마다 보냄, 알림 목록에는 저장하지 않음 (커밋 후 발송)
        List<Long> receivers = memberRepository.findMemberIds(potId, CHAT_MEMBER).stream()
                .filter(id -> !id.equals(userId))
                .toList();
        chatPushNotifier.notifyNewMessage(receivers, potId, pot.getTitle());
        return response;
    }

    /**
     * <b>GET /api/quick-blocks</b> — 퀵블록 문구 목록 조회
     * <p>
     * 서버 상수로 관리하는 원터치 문구 4개 ('도착했어요', '조금 늦어요', '곧 도착해요', '곧 출발해요'). 앱과 서버 검증 목록을 한 곳에서 관리
     */
    @Transactional(readOnly = true)
    public List<QuickBlockResponse> getQuickBlocks(Long userId) {
        return QuickBlockCatalog.BLOCKS;
    }

    private void checkMember(Long userId, Long potId) {
        if (!memberRepository.existsByPotIdAndMemberIdAndStatusIn(potId, userId, CHAT_MEMBER)) {
            throw new BusinessException(ErrorCode.NOT_POT_MEMBER);
        }
    }

    private Map<Long, String> nicknamesOf(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(new HashSet<>(userIds)).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));
    }

    private ChatMessageResponse toResponse(ChatMessage m, String senderNickname) {
        return new ChatMessageResponse(
                m.getId(),
                m.getPotId(),
                m.getSenderId(),
                senderNickname == null ? UNKNOWN_NICKNAME : senderNickname,
                m.getMsgType(),
                m.getContent(),
                Kst.toOffset(m.getSentAt())
        );
    }
}
