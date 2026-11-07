package com.taxipot.domain.settlement.service;

import com.taxipot.domain.notification.service.NotificationSender;
import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.entity.TaxipotMember;
import com.taxipot.domain.pot.realtime.PotRealtimePublisher;
import com.taxipot.domain.pot.repository.TaxipotMemberRepository;
import com.taxipot.domain.pot.repository.TaxipotRepository;
import com.taxipot.domain.settlement.dto.BatchStatusItem;
import com.taxipot.domain.settlement.dto.BatchStatusRequest;
import com.taxipot.domain.settlement.entity.Settlement;
import com.taxipot.domain.settlement.repository.SettlementRepository;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.wallet.entity.Wallet;
import com.taxipot.domain.wallet.repository.PointHistoryRepository;
import com.taxipot.domain.wallet.repository.WalletRepository;
import com.taxipot.domain.wallet.service.LockedMember;
import com.taxipot.domain.wallet.service.MemberWalletLocker;
import com.taxipot.domain.wallet.service.WalletLedger;
import com.taxipot.global.code.NotificationType;
import com.taxipot.global.code.PotMemberStatus;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.code.PreferredSeat;
import com.taxipot.global.code.SettlementStatus;
import com.taxipot.global.config.AppProperties;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.time.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 장애 복구 상태 일괄 입력 (POST /api/admin/pots/batch-status) 단위 테스트 — DB 없이 실행 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminBatchStatusServiceTest {

    private static final Long POT_ID = 100L;
    private static final Long HOST = 1L;
    private static final Long GUEST = 2L;
    private static final Long ABSENT = 3L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 7, 22, 0);

    @Mock TaxipotRepository taxipotRepository;
    @Mock TaxipotMemberRepository taxipotMemberRepository;
    @Mock SettlementRepository settlementRepository;
    @Mock MemberWalletLocker memberWalletLocker;
    @Mock NotificationSender notificationSender;
    @Mock PotRealtimePublisher potRealtimePublisher;
    @Mock AppProperties props;
    @Mock TimeProvider timeProvider;
    @Mock WalletRepository walletRepository;
    @Mock PointHistoryRepository pointHistoryRepository;

    AdminBatchStatusService service;
    Taxipot pot;
    Map<Long, Wallet> wallets = new LinkedHashMap<>();
    Map<Long, Settlement> settlements = new LinkedHashMap<>();
    Map<Long, TaxipotMember> members = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        WalletLedger ledger = new WalletLedger(walletRepository, pointHistoryRepository, props);
        SettlementExecutor executor = new SettlementExecutor(settlementRepository, ledger, notificationSender, props);
        service = new AdminBatchStatusService(taxipotRepository, taxipotMemberRepository, settlementRepository,
                memberWalletLocker, ledger, executor, notificationSender, potRealtimePublisher, props, timeProvider);

        when(props.pot()).thenReturn(new AppProperties.Pot(15, 24, 5, 10, 30, 3, 3, 12, 10, 1_000, 15, 1_000, 500_000));
        when(props.debt()).thenReturn(new AppProperties.Debt(3));
        when(props.wallet()).thenReturn(new AppProperties.Wallet(500_000, 10_000, 50_000));
        when(timeProvider.now()).thenReturn(NOW);

        pot = mock(Taxipot.class);
        when(pot.getId()).thenReturn(POT_ID);
        when(pot.getHostId()).thenReturn(HOST);
        when(pot.getTitle()).thenReturn("두정역 → 본관");
        when(pot.getStatus()).thenReturn(PotStatus.RUNNING);
        when(taxipotRepository.findByIdForUpdate(POT_ID)).thenReturn(Optional.of(pot));

        // 3명 모두 예치 5,500원, 잔액 20,000원
        for (Long id : List.of(HOST, GUEST, ABSENT)) {
            Wallet w = Wallet.open(id);
            w.deposit(25_500);
            w.hold(5_500);
            wallets.put(id, w);
            settlements.put(id, Settlement.hold(POT_ID, id, 5_500));
            members.put(id, id.equals(HOST)
                    ? TaxipotMember.host(POT_ID, id, PreferredSeat.NONE, NOW.minusHours(2))
                    : TaxipotMember.guest(POT_ID, id, PreferredSeat.NONE, NOW.minusHours(2)));
        }

        when(settlementRepository.findAllByPotIdAndStatus(POT_ID, SettlementStatus.HELD))
                .thenAnswer(inv -> settlements.values().stream().filter(s -> s.getStatus() == SettlementStatus.HELD).toList());
        when(settlementRepository.findAllByPotId(POT_ID)).thenAnswer(inv -> List.copyOf(settlements.values()));
        when(taxipotMemberRepository.findAllByPotIdAndStatusInOrderByIdAsc(eq(POT_ID), anyCollection()))
                .thenAnswer(inv -> List.copyOf(members.values()));
        when(memberWalletLocker.lockAll(anyCollection())).thenAnswer(inv -> {
            Map<Long, LockedMember> locked = new LinkedHashMap<>();
            for (Long id : new TreeSet<>((Collection<Long>) inv.getArgument(0))) {
                User u = mock(User.class);
                when(u.getReportCount()).thenReturn(0);
                locked.put(id, new LockedMember(u, wallets.get(id)));
            }
            return locked;
        });
    }

    private void apply(BatchStatusItem... items) {
        service.applyBatchStatus(9L, new BatchStatusRequest(List.of(items)));
    }

    @Test
    @DisplayName("COMPLETED: 오프라인으로 함께 탄 사람(JOINED)은 BOARDED로 바꾸고 정산, 방은 청구 승인·종료 처리")
    void completed_allRode() {
        apply(new BatchStatusItem(POT_ID, PotStatus.COMPLETED, 21_000, null));

        assertThat(members.values()).extracting(TaxipotMember::getStatus).containsOnly(PotMemberStatus.BOARDED);
        assertThat(settlements.values()).extracting(Settlement::getStatus).containsOnly(SettlementStatus.SETTLED);
        assertThat(settlements.get(GUEST).getFareShare()).isEqualTo(7_000);      // 21,000 ÷ 3
        assertThat(wallets.get(GUEST).getBalance()).isEqualTo(20_000 + 5_500 - 7_000 - 1_000);
        assertThat(wallets.get(HOST).getBalance()).isEqualTo(20_000 + 21_000 + 5_500 - 7_000 - 1_000);
        verify(pot).approveFareClaim(21_000, NOW);
        verify(potRealtimePublisher).potUpdated(POT_ID);
    }

    @Test
    @DisplayName("COMPLETED + 한 명 노쇼 지정: 노쇼한 사람은 예치금 전액 환불, 남은 2명끼리 1/2 정산")
    void completed_withNoShow() {
        apply(new BatchStatusItem(POT_ID, PotStatus.COMPLETED, 20_000,
                List.of(new BatchStatusItem.Member(ABSENT, PotMemberStatus.NO_SHOW))));

        assertThat(members.get(ABSENT).getStatus()).isEqualTo(PotMemberStatus.NO_SHOW);
        assertThat(settlements.get(ABSENT).getStatus()).isEqualTo(SettlementStatus.REFUNDED);
        assertThat(wallets.get(ABSENT).getBalance()).isEqualTo(25_500);         // 예치 전으로 돌아감
        assertThat(wallets.get(ABSENT).getHeldBalance()).isZero();
        assertThat(settlements.get(GUEST).getFareShare()).isEqualTo(10_000);    // 20,000 ÷ 2
        assertThat(wallets.get(GUEST).getBalance()).isEqualTo(20_000 + 5_500 - 10_000 - 1_000);
    }

    @Test
    @DisplayName("CANCELED: 예치 중인 전원 전액 환불, 방 취소, 취소 알림")
    void canceled() {
        apply(new BatchStatusItem(POT_ID, PotStatus.CANCELED, null, null));

        assertThat(settlements.values()).extracting(Settlement::getStatus).containsOnly(SettlementStatus.REFUNDED);
        assertThat(wallets.values()).extracting(Wallet::getBalance).containsOnly(25_500);
        verify(pot).cancel();
        verify(pot, never()).approveFareClaim(anyInt(), any());
        verify(notificationSender).sendAll(eq(List.of(HOST, GUEST, ABSENT)), eq(POT_ID), eq(NotificationType.POT),
                eq("택시팟 취소"), anyString());
    }

    @Test
    @DisplayName("COMPLETED인데 actualFare가 없으면 400, 아무것도 안 바뀜")
    void completed_withoutFare() {
        assertThatThrownBy(() -> apply(new BatchStatusItem(POT_ID, PotStatus.COMPLETED, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verify(memberWalletLocker, never()).lockAll(any());
    }

    @Test
    @DisplayName("결과 상태가 아닌 값(RUNNING) · 같은 방 중복 · 이미 끝난 방은 400")
    void invalidInputs() {
        assertThatThrownBy(() -> apply(new BatchStatusItem(POT_ID, PotStatus.RUNNING, null, null)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> apply(new BatchStatusItem(POT_ID, PotStatus.CANCELED, null, null),
                new BatchStatusItem(POT_ID, PotStatus.CANCELED, null, null)))
                .isInstanceOf(BusinessException.class);

        when(pot.getStatus()).thenReturn(PotStatus.COMPLETED);
        assertThatThrownBy(() -> apply(new BatchStatusItem(POT_ID, PotStatus.CANCELED, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
