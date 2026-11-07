package com.taxipot.domain.settlement.service;

import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.entity.TaxipotMember;
import com.taxipot.domain.pot.repository.TaxipotMemberRepository;
import com.taxipot.domain.pot.repository.TaxipotRepository;
import com.taxipot.domain.settlement.dto.FareClaimPageResponse;
import com.taxipot.domain.settlement.dto.FareClaimResponse;
import com.taxipot.domain.settlement.repository.FareClaimRepository;
import com.taxipot.domain.user.entity.User;
import com.taxipot.domain.user.repository.UserRepository;
import com.taxipot.global.code.FareClaimStatus;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.config.AppProperties;
import com.taxipot.global.file.FileStorage;
import com.taxipot.global.file.StoredFile;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 운영자 실요금 청구 목록·영수증 (GET /api/admin/fare-claims, /api/admin/pots/{potId}/receipt) 단위 테스트 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminFareClaimServiceQueryTest {

    private static final Long POT_ID = 100L;
    private static final LocalDateTime STARTED = LocalDateTime.of(2026, 10, 31, 9, 0);

    @Mock FareClaimRepository fareClaimRepository;
    @Mock TaxipotRepository taxipotRepository;
    @Mock TaxipotMemberRepository taxipotMemberRepository;
    @Mock UserRepository userRepository;
    @Mock AppProperties appProperties;
    @Mock FileStorage fileStorage;

    AdminFareClaimService service;
    Taxipot pot;

    @BeforeEach
    void setUp() {
        FareClaimResponseFactory factory = new FareClaimResponseFactory(taxipotMemberRepository, userRepository, appProperties);
        // 목록·영수증 조회에는 정산용 부품이 필요 없어 null 로 둔다
        service = new AdminFareClaimService(fareClaimRepository, taxipotRepository, factory, fileStorage,
                null, null, null, null, null, null, appProperties, null);
        when(appProperties.fareClaim()).thenReturn(new AppProperties.FareClaim(24));

        pot = mock(Taxipot.class);
        when(pot.getId()).thenReturn(POT_ID);
        when(pot.getHostId()).thenReturn(1L);
        when(pot.getTitle()).thenReturn("두정역 → 본관");
        when(pot.getStatus()).thenReturn(PotStatus.RUNNING);
        when(pot.getFareClaimStatus()).thenReturn(FareClaimStatus.PENDING);
        when(pot.getExpectedFare()).thenReturn(20_000);
        when(pot.getActualFare()).thenReturn(23_001);
        when(pot.getReceiptImgPath()).thenReturn("receipts/a.jpg");
        when(pot.getStartedAt()).thenReturn(STARTED);

        User host = mock(User.class);
        when(host.getNickname()).thenReturn("방장");
        when(userRepository.findById(1L)).thenReturn(Optional.of(host));
        TaxipotMember b = mock(TaxipotMember.class);
        when(taxipotMemberRepository.findAllByPotIdAndStatusInOrderByIdAsc(eq(POT_ID), anyCollection()))
                .thenReturn(List.of(b, b, b));
        when(fareClaimRepository.findAllByFareClaimStatus(any(), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(pot), inv.getArgument(1), 1));
    }

    @Test
    @DisplayName("청구 목록: 상태를 안 고르면 PENDING, 청구 시각 순으로 조회")
    void defaultPending() {
        service.getFareClaims(9L, null, 0, 20);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(fareClaimRepository).findAllByFareClaimStatus(eq(FareClaimStatus.PENDING), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("fareClaimedAt").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("청구 목록: 운영자용 영수증 경로, 실요금 비율(23,001 ÷ 20,000 = 1.15), 청구 기한(탑승 + 24시간)")
    void responseFields() {
        FareClaimPageResponse res = service.getFareClaims(9L, FareClaimStatus.PENDING, 0, 20);

        FareClaimResponse first = res.content().get(0);
        assertThat(first.receiptUrl()).isEqualTo("/api/admin/pots/100/receipt");
        assertThat(first.fareRatio()).isEqualTo(1.15);
        assertThat(first.hostNickname()).isEqualTo("방장");
        assertThat(first.boardedCount()).isEqualTo(3);
        assertThat(first.claimDeadline().toLocalDateTime()).isEqualTo(STARTED.plusHours(24));
    }

    @Test
    @DisplayName("실요금 비율: 청구 전이면 null, 반올림은 소수 둘째 자리")
    void fareRatio() {
        assertThat(FareClaimResponseFactory.fareRatio(null, 20_000)).isNull();
        assertThat(FareClaimResponseFactory.fareRatio(10_000, 0)).isNull();
        assertThat(FareClaimResponseFactory.fareRatio(10_005, 10_000)).isEqualTo(1.0);    // 1.0005 → 1.00
        assertThat(FareClaimResponseFactory.fareRatio(12_345, 10_000)).isEqualTo(1.23);   // 1.2345 → 1.23
        assertThat(FareClaimResponseFactory.fareRatio(12_350, 10_000)).isEqualTo(1.24);   // 1.235 → 1.24
    }

    @Test
    @DisplayName("운영자 영수증: 방의 영수증 파일을 그대로 돌려준다")
    void adminReceipt() {
        when(taxipotRepository.findById(POT_ID)).thenReturn(Optional.of(pot));
        StoredFile file = new StoredFile(new ByteArrayResource(new byte[]{1, 2, 3}), MediaType.IMAGE_JPEG);
        when(fileStorage.load("receipts/a.jpg")).thenReturn(file);

        assertThat(service.getReceipt(9L, POT_ID)).isSameAs(file);
    }
}
