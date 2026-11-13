package com.taxipot.domain.settlement.service;

import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.entity.TaxipotMember;
import com.taxipot.domain.pot.repository.TaxipotMemberRepository;
import com.taxipot.domain.pot.repository.TaxipotRepository;
import com.taxipot.domain.settlement.dto.PotSettlementResponse;
import com.taxipot.domain.settlement.entity.Settlement;
import com.taxipot.domain.settlement.repository.SettlementRepository;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.user.repository.UserRepository;
import com.taxipot.global.code.FareClaimStatus;
import com.taxipot.global.code.SettlementStatus;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.file.FileStorage;
import com.taxipot.global.file.StoredFile;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 정산 내역·영수증 조회 (GET /api/pots/{potId}/settlement, /receipt) 단위 테스트 — DB 없이 실행 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SettlementQueryServiceTest {

    private static final Long POT_ID = 100L;
    private static final Long HOST_ID = 1L;
    private static final Long GUEST_ID = 2L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 31, 15, 0);

    @Mock TaxipotRepository taxipotRepository;
    @Mock TaxipotMemberRepository taxipotMemberRepository;
    @Mock SettlementRepository settlementRepository;
    @Mock UserRepository userRepository;
    @Mock FileStorage fileStorage;

    SettlementQueryService service;
    Taxipot pot;

    @BeforeEach
    void setUp() {
        service = new SettlementQueryService(taxipotRepository, taxipotMemberRepository, settlementRepository,
                userRepository, fileStorage);

        pot = mock(Taxipot.class);
        when(pot.getId()).thenReturn(POT_ID);
        when(pot.getExpectedFare()).thenReturn(20_000);
        when(taxipotRepository.findById(POT_ID)).thenReturn(Optional.of(pot));

        User host = user(HOST_ID, "방장");
        User guest = user(GUEST_ID, "동승자");
        when(userRepository.findAllById(anyIterable())).thenReturn(List.of(host, guest));
        TaxipotMember b1 = mock(TaxipotMember.class);
        TaxipotMember b2 = mock(TaxipotMember.class);
        when(taxipotMemberRepository.findAllByPotIdAndStatusInOrderByIdAsc(eq(POT_ID), anyCollection()))
                .thenReturn(List.of(b1, b2));
    }

    private User user(Long id, String nickname) {
        User u = mock(User.class);
        when(u.getId()).thenReturn(id);
        when(u.getNickname()).thenReturn(nickname);
        return u;
    }

    private Settlement settlement(long id, Long memberId) {
        Settlement s = Settlement.hold(POT_ID, memberId, 13_000);
        ReflectionTestUtils.setField(s, "id", id);
        return s;
    }

    private void member(Long userId) {
        when(taxipotMemberRepository.findByPotIdAndMemberId(POT_ID, userId)).thenReturn(Optional.of(mock(TaxipotMember.class)));
    }

    // ===================================================================== 정산 내역

    @Test
    @DisplayName("정산 전(청구 대기): 정산 값은 null, 방장 정산금도 null, 참여자별 예치액·닉네임은 보임")
    void beforeApproval() {
        member(GUEST_ID);
        when(pot.getFareClaimStatus()).thenReturn(FareClaimStatus.PENDING);
        when(pot.getActualFare()).thenReturn(23_000);
        when(pot.getReceiptImgPath()).thenReturn("receipts/a.jpg");
        Settlement s2 = settlement(11, GUEST_ID);
        Settlement s1 = settlement(10, HOST_ID);
        when(settlementRepository.findAllByPotId(POT_ID)).thenReturn(List.of(s2, s1));   // 순서 섞여 와도

        PotSettlementResponse res = service.getPotSettlement(GUEST_ID, POT_ID);

        assertThat(res.hostPayoutAmount()).isNull();
        assertThat(res.boardedCount()).isEqualTo(2);
        assertThat(res.receiptUrl()).isEqualTo("/api/pots/100/receipt");
        assertThat(res.settlements()).extracting(r -> r.id()).containsExactly(10L, 11L);   // id 순 정렬
        assertThat(res.settlements().get(0).nickname()).isEqualTo("방장");
        assertThat(res.settlements().get(0).heldAmount()).isEqualTo(13_000);
        assertThat(res.settlements().get(0).fareShare()).isNull();
        assertThat(res.settlements().get(0).status()).isEqualTo(SettlementStatus.HELD);
    }

    @Test
    @DisplayName("승인 후: 방장 정산금 = 실요금, 참여자별 분담금·수수료·차액이 채워짐")
    void afterApproval() {
        member(HOST_ID);
        when(pot.getFareClaimStatus()).thenReturn(FareClaimStatus.APPROVED);
        when(pot.getActualFare()).thenReturn(23_001);
        Settlement s = settlement(10, HOST_ID);
        s.settle(11_500, 1_000, 500, 0, NOW, null);
        when(settlementRepository.findAllByPotId(POT_ID)).thenReturn(List.of(s));

        PotSettlementResponse res = service.getPotSettlement(HOST_ID, POT_ID);

        assertThat(res.hostPayoutAmount()).isEqualTo(23_001);
        assertThat(res.actualFare()).isEqualTo(23_001);
        assertThat(res.receiptUrl()).isNull();                   // 영수증 경로 없음
        assertThat(res.settlements().get(0).fareShare()).isEqualTo(11_500);
        assertThat(res.settlements().get(0).feeAmount()).isEqualTo(1_000);
        assertThat(res.settlements().get(0).status()).isEqualTo(SettlementStatus.SETTLED);
        assertThat(res.settlements().get(0).settledAt().toLocalDateTime()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("이 방에 참여한 적 없으면 403 NOT_POT_MEMBER")
    void notMember() {
        when(taxipotMemberRepository.findByPotIdAndMemberId(POT_ID, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPotSettlement(99L, POT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_POT_MEMBER);
    }

    // ===================================================================== 영수증

    @Test
    @DisplayName("영수증: 탑승자(BOARDED)면 파일을 돌려준다")
    void receipt() {
        when(taxipotMemberRepository.existsByPotIdAndMemberIdAndStatusIn(eq(POT_ID), eq(GUEST_ID), anyCollection()))
                .thenReturn(true);
        when(pot.getReceiptImgPath()).thenReturn("receipts/a.jpg");
        StoredFile file = new StoredFile(new ByteArrayResource(new byte[]{1, 2, 3}), MediaType.IMAGE_JPEG);
        when(fileStorage.load("receipts/a.jpg")).thenReturn(file);

        assertThat(service.getReceipt(GUEST_ID, POT_ID)).isSameAs(file);
    }

    @Test
    @DisplayName("영수증: 탑승자가 아니면 403 NOT_POT_MEMBER (파일은 열지도 않음)")
    void receipt_notBoarded() {
        when(taxipotMemberRepository.existsByPotIdAndMemberIdAndStatusIn(eq(POT_ID), eq(GUEST_ID), anyCollection()))
                .thenReturn(false);

        assertThatThrownBy(() -> service.getReceipt(GUEST_ID, POT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_POT_MEMBER);
        verify(fileStorage, never()).load(any());
    }
}
