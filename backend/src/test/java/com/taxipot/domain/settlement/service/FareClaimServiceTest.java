package com.taxipot.domain.settlement.service;

import com.taxipot.domain.notification.service.NotificationSender;
import com.taxipot.domain.pot.entity.Taxipot;
import com.taxipot.domain.pot.realtime.PotRealtimePublisher;
import com.taxipot.domain.pot.repository.TaxipotRepository;
import com.taxipot.domain.settlement.dto.FareClaimRequest;
import com.taxipot.domain.settlement.dto.FareClaimResponse;
import com.taxipot.global.code.FareClaimStatus;
import com.taxipot.global.code.NotificationType;
import com.taxipot.global.code.PotStatus;
import com.taxipot.global.error.BusinessException;
import com.taxipot.global.error.ErrorCode;
import com.taxipot.global.file.FileStorage;
import com.taxipot.global.time.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 방장 실요금 청구 (POST /api/pots/{potId}/fare-claim) 단위 테스트 — DB·파일 없이 실행 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FareClaimServiceTest {

    private static final Long POT_ID = 100L;
    private static final Long HOST_ID = 1L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 18, 0);

    @Mock TaxipotRepository taxipotRepository;
    @Mock FileStorage fileStorage;
    @Mock NotificationSender notificationSender;
    @Mock PotRealtimePublisher potRealtimePublisher;
    @Mock FareClaimResponseFactory fareClaimResponseFactory;
    @Mock TimeProvider timeProvider;

    FareClaimService service;
    Taxipot pot;
    FareClaimResponse response;

    @BeforeEach
    void setUp() {
        service = new FareClaimService(taxipotRepository, fileStorage, notificationSender, potRealtimePublisher,
                fareClaimResponseFactory, timeProvider);

        pot = mock(Taxipot.class);
        when(pot.getId()).thenReturn(POT_ID);
        when(pot.getHostId()).thenReturn(HOST_ID);
        when(pot.getTitle()).thenReturn("두정역 → 본관");
        when(pot.getStatus()).thenReturn(PotStatus.RUNNING);
        when(pot.getFareClaimStatus()).thenReturn(FareClaimStatus.NONE);
        when(taxipotRepository.findByIdForUpdate(POT_ID)).thenReturn(Optional.of(pot));

        when(timeProvider.now()).thenReturn(NOW);
        when(fileStorage.saveImage(any(), anyString())).thenReturn("receipts/100/new.jpg");
        response = new FareClaimResponse(POT_ID, "두정역 → 본관", HOST_ID, "방장", PotStatus.RUNNING, FareClaimStatus.PENDING,
                20_000, 19_300, 0.97, 3, "/api/pots/100/receipt", null, null, null, null, null);
        when(fareClaimResponseFactory.of(eq(pot), anyString())).thenReturn(response);
    }

    private FareClaimRequest request(int fare) {
        return new FareClaimRequest(fare, new MockMultipartFile("receiptImage", "r.jpg", "image/jpeg", new byte[]{1, 2, 3}));
    }

    @Test
    @DisplayName("첫 청구: 영수증을 receipts/{potId}에 저장하고 PENDING으로, 운영자 알림 + 탑승자 화면 갱신")
    void claim_first() {
        FareClaimResponse res = service.claimFare(HOST_ID, POT_ID, request(19_300));

        assertThat(res).isSameAs(response);
        verify(fileStorage).saveImage(any(), eq("receipts/100"));
        verify(pot).claimFare(19_300, "receipts/100/new.jpg", NOW);
        verify(fileStorage, never()).deleteAfterCommit(any());               // 이전 사진 없음
        verify(notificationSender).sendToAdmins(eq(POT_ID), eq(NotificationType.SYSTEM), eq("정산 청구 확인 요청"),
                contains("19,300원"));
        verify(potRealtimePublisher).potUpdated(POT_ID);
        verify(fareClaimResponseFactory).of(pot, "/api/pots/");               // 방장용 영수증 경로
    }

    @Test
    @DisplayName("반려 후 재청구: 새 사진 저장, 이전 사진은 커밋 뒤 삭제")
    void claim_afterRejected() {
        when(pot.getFareClaimStatus()).thenReturn(FareClaimStatus.REJECTED);
        when(pot.getReceiptImgPath()).thenReturn("receipts/100/old.jpg");

        service.claimFare(HOST_ID, POT_ID, request(18_000));

        verify(fileStorage).deleteAfterCommit("receipts/100/old.jpg");
        verify(pot).claimFare(18_000, "receipts/100/new.jpg", NOW);
    }

    @Test
    @DisplayName("기한 지난(OVERDUE) 방도 늦게라도 청구할 수 있다")
    void claim_overdue() {
        when(pot.getFareClaimStatus()).thenReturn(FareClaimStatus.OVERDUE);

        service.claimFare(HOST_ID, POT_ID, request(19_300));

        verify(pot).claimFare(anyInt(), anyString(), eq(NOW));
    }

    @Test
    @DisplayName("사진이 비어 있으면 400 RECEIPT_REQUIRED (방을 잠그지도 않음)")
    void claim_noReceipt() {
        FareClaimRequest empty = new FareClaimRequest(19_300, new MockMultipartFile("receiptImage", new byte[0]));

        assertThatThrownBy(() -> service.claimFare(HOST_ID, POT_ID, empty))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECEIPT_REQUIRED);
        verify(taxipotRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("방장이 아니면 403 NOT_POT_HOST (사진 저장 안 함)")
    void claim_notHost() {
        assertThatThrownBy(() -> service.claimFare(2L, POT_ID, request(19_300)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_POT_HOST);
        verify(fileStorage, never()).saveImage(any(), anyString());
    }

    @Test
    @DisplayName("이미 확인 대기(PENDING)면 409 FARE_CLAIM_IN_REVIEW")
    void claim_inReview() {
        when(pot.getFareClaimStatus()).thenReturn(FareClaimStatus.PENDING);

        assertThatThrownBy(() -> service.claimFare(HOST_ID, POT_ID, request(19_300)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FARE_CLAIM_IN_REVIEW);
    }

    @Test
    @DisplayName("이미 정산(APPROVED)됐으면 409 ALREADY_SETTLED (방 상태 COMPLETED여도 이 코드로 안내)")
    void claim_alreadySettled() {
        when(pot.getFareClaimStatus()).thenReturn(FareClaimStatus.APPROVED);
        when(pot.getStatus()).thenReturn(PotStatus.COMPLETED);

        assertThatThrownBy(() -> service.claimFare(HOST_ID, POT_ID, request(19_300)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_SETTLED);
    }

    @Test
    @DisplayName("탑승 시작 전(모집중)이면 409 INVALID_POT_STATUS")
    void claim_notRunning() {
        when(pot.getStatus()).thenReturn(PotStatus.RECRUITING);

        assertThatThrownBy(() -> service.claimFare(HOST_ID, POT_ID, request(19_300)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_POT_STATUS);
        verify(fileStorage, never()).saveImage(any(), anyString());
    }
}
