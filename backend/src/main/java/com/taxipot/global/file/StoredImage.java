package com.taxipot.global.file;

import org.springframework.core.io.Resource;

/** 이미지 조회 API(캡처본·영수증) 서비스 반환 타입. 만들기: fileStorage.loadImage(path) */
public record StoredImage(Resource resource, String contentType) {
}
