package com.taxipot.global.file;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;

/** 이미지 조회 API(캡처본·영수증) 응답: JSON 이 아니라 바이너리 + Content-Type */
public final class ImageResponses {

    private ImageResponses() {
    }

    public static ResponseEntity<Resource> of(StoredFile file) {
        return ResponseEntity.ok()
                .contentType(file.mediaType())
                .cacheControl(CacheControl.noStore())
                .body(file.resource());
    }
}
