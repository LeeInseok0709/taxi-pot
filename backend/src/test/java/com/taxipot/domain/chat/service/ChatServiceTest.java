package com.taxipot.domain.chat.service;

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
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 채팅 조회·전송·퀵블록 (GET·POST /api/pots/{potId}/messages, GET /api/quick-blocks) 단위 테스트 — DB 없이 실행 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceTest {

    private static final Long POT_ID = 3L;
    private static final Long SENDER_ID = 7L;
    private static final Long OTHER_A = 8L;
    private static final Long OTHER_B = 9L;
    private static final String POT_TITLE = "정문 → 터미널";

    @Mock ChatMessageRepository chatMessageRepository;
    @Mock TaxipotRepository taxipotRepository;
    @Mock TaxipotMemberRepository memberRepository;
    @Mock UserRepository userRepository;
    @Mock PotRealtimePublisher realtimePublisher;
    @Mock ChatPushNotifier chatPushNotifier;

    ChatService service;
    Taxipot pot;

    @BeforeEach
    void setUp() {
        service = new ChatService(chatMessageRepository, taxipotRepository, memberRepository, userRepository,
                realtimePublisher, chatPushNotifier);

        pot = mock(Taxipot.class);
        when(pot.getStatus()).thenReturn(PotStatus.RECRUITING);
        when(pot.getTitle()).thenReturn(POT_TITLE);
        when(taxipotRepository.findById(POT_ID)).thenReturn(Optional.of(pot));

        // 기본: 보낸 사람은 참여자, 방에는 3명
        when(memberRepository.existsByPotIdAndMemberIdAndStatusIn(eq(POT_ID), eq(SENDER_ID), anyCollection())).thenReturn(true);
        when(memberRepository.findMemberIds(eq(POT_ID), anyCollection())).thenReturn(List.of(SENDER_ID, OTHER_A, OTHER_B));

        User sender = user(SENDER_ID, "서연");
        when(userRepository.findById(SENDER_ID)).thenReturn(Optional.of(sender));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ===== 전송 =====

    @Test
    @DisplayName("TEXT 전송: 저장하고 방송하고, 보낸 사람을 뺀 참여자에게 푸시한다")
    void sendText() {
        ChatMessageResponse res = service.sendMessage(SENDER_ID, POT_ID, new ChatMessageRequest(MsgType.TEXT, "정문 앞이에요"));

        assertThat(res.potId()).isEqualTo(POT_ID);
        assertThat(res.senderId()).isEqualTo(SENDER_ID);
        assertThat(res.senderNickname()).isEqualTo("서연");
        assertThat(res.msgType()).isEqualTo(MsgType.TEXT);
        assertThat(res.content()).isEqualTo("정문 앞이에요");

        verify(chatMessageRepository).save(any(ChatMessage.class));
        verify(realtimePublisher).chatMessage(POT_ID, res);
        verify(chatPushNotifier).notifyNewMessage(List.of(OTHER_A, OTHER_B), POT_ID, POT_TITLE);
    }

    @Test
    @DisplayName("msgType 을 생략하면 TEXT 로 저장하고, 앞뒤 공백은 잘라낸다")
    void sendDefaultsToText() {
        ChatMessageResponse res = service.sendMessage(SENDER_ID, POT_ID, new ChatMessageRequest(null, "  곧 가요  "));

        assertThat(res.msgType()).isEqualTo(MsgType.TEXT);
        assertThat(res.content()).isEqualTo("곧 가요");
    }

    @Test
    @DisplayName("QUICK 전송: 퀵블록 목록에 있는 문구 4개는 모두 허용")
    void sendQuickAllowed() {
        for (String content : List.of("도착했어요", "조금 늦어요", "곧 도착해요", "곧 출발해요")) {
            ChatMessageResponse res = service.sendMessage(SENDER_ID, POT_ID, new ChatMessageRequest(MsgType.QUICK, content));
            assertThat(res.msgType()).isEqualTo(MsgType.QUICK);
            assertThat(res.content()).isEqualTo(content);
        }
    }

    @Test
    @DisplayName("QUICK 전송: 목록에 없는 문구(예전 '3분 늦어요' 포함)는 400 INVALID_QUICK_MESSAGE, 저장·방송·푸시 없음")
    void sendQuickRejected() {
        for (String content : List.of("3분 늦어요", "어디세요?", "아무 말")) {
            assertThatThrownBy(() -> service.sendMessage(SENDER_ID, POT_ID, new ChatMessageRequest(MsgType.QUICK, content)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_QUICK_MESSAGE);
        }
        verifyNothingSent();
    }

    @Test
    @DisplayName("전송: 방 참여자(JOINED·BOARDED)가 아니면 403 NOT_POT_MEMBER")
    void sendNotMember() {
        when(memberRepository.existsByPotIdAndMemberIdAndStatusIn(eq(POT_ID), eq(SENDER_ID), anyCollection())).thenReturn(false);

        assertThatThrownBy(() -> service.sendMessage(SENDER_ID, POT_ID, new ChatMessageRequest(MsgType.TEXT, "안녕하세요")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_POT_MEMBER);
        verifyNothingSent();
    }

    @Test
    @DisplayName("전송: 방 상태가 COMPLETED·CANCELED 이면 409 INVALID_POT_STATUS")
    void sendClosedPot() {
        for (PotStatus status : List.of(PotStatus.COMPLETED, PotStatus.CANCELED)) {
            when(pot.getStatus()).thenReturn(status);

            assertThatThrownBy(() -> service.sendMessage(SENDER_ID, POT_ID, new ChatMessageRequest(MsgType.TEXT, "안녕하세요")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_POT_STATUS);
        }
        verifyNothingSent();
    }

    @Test
    @DisplayName("전송: 모집 중·마감·운행 중인 방에서는 보낼 수 있다")
    void sendOpenPot() {
        for (PotStatus status : List.of(PotStatus.RECRUITING, PotStatus.CLOSED, PotStatus.RUNNING)) {
            when(pot.getStatus()).thenReturn(status);
            assertThat(service.sendMessage(SENDER_ID, POT_ID, new ChatMessageRequest(MsgType.TEXT, "안녕하세요")).content())
                    .isEqualTo("안녕하세요");
        }
    }

    @Test
    @DisplayName("전송: 없는 방이면 404 NOT_FOUND")
    void sendPotNotFound() {
        when(taxipotRepository.findById(POT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendMessage(SENDER_ID, POT_ID, new ChatMessageRequest(MsgType.TEXT, "안녕하세요")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verifyNothingSent();
    }

    @Test
    @DisplayName("전송: 내용이 비어 있으면 400 INVALID_INPUT")
    void sendBlank() {
        assertThatThrownBy(() -> service.sendMessage(SENDER_ID, POT_ID, new ChatMessageRequest(MsgType.TEXT, "   ")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNothingSent();
    }

    @Test
    @DisplayName("전송: 방에 혼자면 푸시 받을 사람이 없다 (빈 목록 전달)")
    void sendAlone() {
        when(memberRepository.findMemberIds(eq(POT_ID), anyCollection())).thenReturn(List.of(SENDER_ID));

        service.sendMessage(SENDER_ID, POT_ID, new ChatMessageRequest(MsgType.TEXT, "아무도 없나요"));

        verify(chatPushNotifier).notifyNewMessage(List.of(), POT_ID, POT_TITLE);
    }

    // ===== 조회 =====

    @Test
    @DisplayName("조회: 첫 페이지 — 한 건 더 읽어 hasNext 를 판단하고 nextCursor 는 마지막 메시지 id")
    void getFirstPageHasNext() {
        // size=3 요청 → 4건 조회 (id 10, 9, 8, 7)
        List<ChatMessage> rows = messages(10L, 9L, 8L, 7L);
        when(chatMessageRepository.findAllByPotIdOrderByIdDesc(eq(POT_ID), any(Pageable.class))).thenReturn(rows);
        List<User> found = List.of(user(SENDER_ID, "서연"));
        when(userRepository.findAllById(any())).thenReturn(found);

        ChatMessagePageResponse page = service.getMessages(SENDER_ID, POT_ID, null, 3);

        assertThat(page.messages()).extracting(ChatMessageResponse::id).containsExactly(10L, 9L, 8L);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextCursor()).isEqualTo(8L);
        assertThat(page.messages()).extracting(ChatMessageResponse::senderNickname).containsOnly("서연");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(chatMessageRepository).findAllByPotIdOrderByIdDesc(eq(POT_ID), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(4);
    }

    @Test
    @DisplayName("조회: 커서가 있으면 그보다 작은 id 만 읽고, 마지막 페이지면 hasNext=false·nextCursor=null")
    void getLastPageWithCursor() {
        List<ChatMessage> rows = messages(7L, 6L);
        when(chatMessageRepository.findAllByPotIdAndIdLessThanOrderByIdDesc(eq(POT_ID), eq(8L), any(Pageable.class))).thenReturn(rows);
        List<User> found = List.of(user(SENDER_ID, "서연"));
        when(userRepository.findAllById(any())).thenReturn(found);

        ChatMessagePageResponse page = service.getMessages(SENDER_ID, POT_ID, 8L, 3);

        assertThat(page.messages()).extracting(ChatMessageResponse::id).containsExactly(7L, 6L);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
        verify(chatMessageRepository, never()).findAllByPotIdOrderByIdDesc(anyLong(), any(Pageable.class));
    }

    @Test
    @DisplayName("조회: 메시지가 없으면 빈 목록")
    void getEmpty() {
        when(chatMessageRepository.findAllByPotIdOrderByIdDesc(eq(POT_ID), any(Pageable.class))).thenReturn(List.of());

        ChatMessagePageResponse page = service.getMessages(SENDER_ID, POT_ID, null, 30);

        assertThat(page.messages()).isEmpty();
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    @DisplayName("조회: size 는 1~100 으로 맞춘다")
    void getClampsSize() {
        when(chatMessageRepository.findAllByPotIdOrderByIdDesc(eq(POT_ID), any(Pageable.class))).thenReturn(List.of());

        service.getMessages(SENDER_ID, POT_ID, null, 0);
        service.getMessages(SENDER_ID, POT_ID, null, 5000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(chatMessageRepository, org.mockito.Mockito.times(2)).findAllByPotIdOrderByIdDesc(eq(POT_ID), pageable.capture());
        assertThat(pageable.getAllValues()).extracting(Pageable::getPageSize).containsExactly(2, 101);
    }

    @Test
    @DisplayName("조회: 탈퇴 등으로 닉네임을 찾지 못한 보낸 사람은 '(알 수 없음)'")
    void getUnknownSender() {
        List<ChatMessage> rows = messages(5L);
        when(chatMessageRepository.findAllByPotIdOrderByIdDesc(eq(POT_ID), any(Pageable.class))).thenReturn(rows);
        when(userRepository.findAllById(any())).thenReturn(List.of());

        ChatMessagePageResponse page = service.getMessages(SENDER_ID, POT_ID, null, 30);

        assertThat(page.messages().get(0).senderNickname()).isEqualTo("(알 수 없음)");
    }

    @Test
    @DisplayName("조회: 방 참여자가 아니면 403 NOT_POT_MEMBER")
    void getNotMember() {
        when(memberRepository.existsByPotIdAndMemberIdAndStatusIn(eq(POT_ID), eq(SENDER_ID), anyCollection())).thenReturn(false);

        assertThatThrownBy(() -> service.getMessages(SENDER_ID, POT_ID, null, 30))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_POT_MEMBER);
    }

    @Test
    @DisplayName("조회: 끝난 방(COMPLETED)이어도 참여자는 기록을 볼 수 있다")
    void getCompletedPot() {
        when(pot.getStatus()).thenReturn(PotStatus.COMPLETED);
        List<ChatMessage> rows = messages(5L);
        when(chatMessageRepository.findAllByPotIdOrderByIdDesc(eq(POT_ID), any(Pageable.class))).thenReturn(rows);
        List<User> found = List.of(user(SENDER_ID, "서연"));
        when(userRepository.findAllById(any())).thenReturn(found);

        assertThat(service.getMessages(SENDER_ID, POT_ID, null, 30).messages()).hasSize(1);
    }

    // ===== 퀵블록 =====

    @Test
    @DisplayName("퀵블록 목록: 4개")
    void quickBlocks() {
        List<QuickBlockResponse> blocks = service.getQuickBlocks(SENDER_ID);

        assertThat(blocks).extracting(QuickBlockResponse::content)
                .containsExactly("도착했어요", "조금 늦어요", "곧 도착해요", "곧 출발해요");
        assertThat(blocks).extracting(QuickBlockResponse::code).doesNotHaveDuplicates();
    }

    // ===== 도우미 =====

    private void verifyNothingSent() {
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
        verify(realtimePublisher, never()).chatMessage(anyLong(), any());
        verify(chatPushNotifier, never()).notifyNewMessage(anyCollection(), anyLong(), any());
    }

    private static User user(Long id, String nickname) {
        User u = mock(User.class);
        when(u.getId()).thenReturn(id);
        when(u.getNickname()).thenReturn(nickname);
        return u;
    }

    private static List<ChatMessage> messages(Long... ids) {
        List<ChatMessage> list = new ArrayList<>();
        for (Long id : ids) {
            ChatMessage m = mock(ChatMessage.class);
            when(m.getId()).thenReturn(id);
            when(m.getPotId()).thenReturn(POT_ID);
            when(m.getSenderId()).thenReturn(SENDER_ID);
            when(m.getMsgType()).thenReturn(MsgType.TEXT);
            when(m.getContent()).thenReturn("메시지 " + id);
            when(m.getSentAt()).thenReturn(LocalDateTime.of(2026, 11, 13, 14, 0));
            list.add(m);
        }
        return list;
    }
}
