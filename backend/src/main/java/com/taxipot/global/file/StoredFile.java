package com.taxipot.global.file;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record StoredFile(Resource resource, MediaType mediaType) {
}
