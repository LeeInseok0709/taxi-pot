package com.taxipot.global.error;

/**
 * 담당자가 아직 구현하지 않은 서비스 메서드에서 던진다 → 501 NOT_IMPLEMENTED.
 * <p>구현을 마치면 이 throw 를 지운다. 배포 전에 프로젝트 전체 검색으로 0개인지 확인한다.
 */
public class NotImplementedYetException extends BusinessException {

    public NotImplementedYetException(String what) {
        super(ErrorCode.NOT_IMPLEMENTED, "아직 구현되지 않은 기능입니다: " + what);
    }
}
