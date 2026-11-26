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
import com.taxipot.global.error.Errors;
import com.taxipot.global.file.FileStorage;
import com.taxipot.global.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 담당: 지갑·정산
 * <p>메서드 주석 = 명세 3.9.0 의 설명·에러 코드. 구현하면 NotImplementedYetException 줄을 지운다.
 * <p>다른 도메인 Service 를 부를 때는 docs/CONVENTIONS.md 5장 의존 방향을 지킨다.
 */
@Service
@RequiredArgsConstructor
public class FareClaimService {

    /** 방장 화면에서 영수증을 여는 경로 */
    private static final String HOST_RECEIPT_PREFIX = "/api/pots/";

    private final TaxipotRepository taxipotRepository;
    private final FileStorage fileStorage;
    private final NotificationSender notificationSender;
    private final PotRealtimePublisher potRealtimePublisher;
    private final FareClaimResponseFactory fareClaimResponseFactory;
    private final TimeProvider timeProvider;

    /**
     * <b>POST /api/pots/{potId}/fare-claim</b> — 실요금 청구 (방장 전용, 3.5.0)
     * <p>
     * 하차 후 방장이 결제내역(영수증) 사진과 실제 요금으로 정산을 청구한다. 정산(돈 이동)은 운영자가 승인할 때 실행된다 (3.5.0, 기존 즉시 정산 API 대체).
     * `multipart/form-data` — 파트 `actualFare`(텍스트, 정수) + `receiptImage`(jpg/png 파일)
     * - Kotlin: `@Multipart` + `@Part("actualFare") actualFare: RequestBody`(`"19300".toRequestBody("text/plain".toMediaType())`) + `@Part receiptImage: MultipartBody.Part`
     * - Spring: `@RequestParam("actualFare") Integer actualFare, @RequestPart("receiptImage") MultipartFile receiptImage`
     * - 사진이 없거나 비어 있으면 400 RECEIPT_REQUIRED, 형식 오류는 400 INVALID_INPUT
     * - 가능 조건: TAXIPOT status = RUNNING이고 fare_claim_status가 NONE·REJECTED·OVERDUE
     *   - PENDING이면 409 FARE_CLAIM_IN_REVIEW, APPROVED면 409 ALREADY_SETTLED
     * - 처리: 영수증 파일 저장(`{app.upload-dir}/receipts/{potId}/{UUID}`, 재청구면 이전 파일 삭제), actual_fare·receipt_img_path 저장,
     *   fare_claim_status = PENDING, fare_claimed_at = now, fare_reject_reason = NULL
     * - 운영자(role = ADMIN)에게 SYSTEM 알림('정산 청구 확인 요청'), 탑승자에게 STOMP `POT_UPDATED`
     * - 청구 접수 후 탑승자는 '진행 중인 다른 방'에서 제외되어 새 방을 이용할 수 있다
     * <p>
     * <b>에러 코드</b>
     * - 400: INVALID_INPUT(금액 범위·이미지 형식) / RECEIPT_REQUIRED
     * - 403: NOT_POT_HOST
     * - 409: INVALID_POT_STATUS / FARE_CLAIM_IN_REVIEW / ALREADY_SETTLED
     */
    @Transactional
    public FareClaimResponse claimFare(Long userId, Long potId, FareClaimRequest request) {
        // 0. 영수증 사진은 필수 (금액 범위 0~500,000 은 컨트롤러 @Valid 가 먼저 검사)
        MultipartFile receiptImage = request.receiptImage();
        if (receiptImage == null || receiptImage.isEmpty()) {
            throw new BusinessException(ErrorCode.RECEIPT_REQUIRED);
        }

        // 1. 방 행을 잠근다 — 같은 방에 청구와 운영자 심사가 동시에 들어와도 순서대로 처리 (돈 이동이 없어 회원·지갑 락은 필요 없음)
        Taxipot pot = taxipotRepository.findByIdForUpdate(potId)
                .orElseThrow(() -> Errors.notFound("택시팟"));
        if (!pot.getHostId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_POT_HOST);
        }

        // 2. 청구 상태 먼저 본다 (APPROVED 방은 이미 COMPLETED 라, 상태보다 먼저 봐야 ALREADY_SETTLED 로 안내됨)
        if (pot.getFareClaimStatus() == FareClaimStatus.PENDING) {
            throw new BusinessException(ErrorCode.FARE_CLAIM_IN_REVIEW);
        }
        if (pot.getFareClaimStatus() == FareClaimStatus.APPROVED) {
            throw new BusinessException(ErrorCode.ALREADY_SETTLED);
        }
        if (pot.getStatus() != PotStatus.RUNNING) {   // 탑승 시작 전이거나 취소된 방
            throw new BusinessException(ErrorCode.INVALID_POT_STATUS);
        }

        // 3. 영수증 저장 (receipts/{potId}/{UUID}.jpg). 재청구면 이전 사진은 커밋된 뒤에 지운다
        String oldReceipt = pot.getReceiptImgPath();
        String newReceipt = fileStorage.saveImage(receiptImage, FileStorage.DIR_RECEIPT + "/" + potId);
        if (oldReceipt != null) {
            fileStorage.deleteAfterCommit(oldReceipt);
        }

        // 4. 청구 기록: 실요금·영수증 경로, PENDING, 청구 시각, 반려 사유 초기화
        int actualFare = request.actualFare();
        pot.claimFare(actualFare, newReceipt, timeProvider.now());

        // 5. 운영자에게 확인 요청 알림, 탑승자 화면 새로고침 신호
        notificationSender.sendToAdmins(potId, NotificationType.SYSTEM, "정산 청구 확인 요청",
                "'%s' 방장이 실요금 %,d원을 청구했습니다.".formatted(pot.getTitle(), actualFare));
        potRealtimePublisher.potUpdated(potId);

        return fareClaimResponseFactory.of(pot, HOST_RECEIPT_PREFIX);
    }
}
