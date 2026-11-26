package com.taxipot.domain.chat.controller;

import com.taxipot.domain.chat.dto.ChatMessagePageResponse;
import com.taxipot.domain.chat.dto.ChatMessageRequest;
import com.taxipot.domain.chat.dto.ChatMessageResponse;
import com.taxipot.domain.chat.dto.QuickBlockResponse;
import com.taxipot.domain.chat.service.ChatService;
import com.taxipot.global.security.LoginUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 명세 tag: Chat — 자동 생성 (tools/gen_api.py). 컨트롤러에는 로직을 넣지 않는다:
 * 요청 검증(@Valid)·서비스 호출·응답 변환만. 경로·파라미터가 명세와 다르면 명세를 먼저 고친다.
 */
@Tag(name = "Chat")
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "채팅 메시지 조회 (이전 메시지, 무한 스크롤)")
    @GetMapping("/api/pots/{potId}/messages")
    public ChatMessagePageResponse getMessages(@LoginUserId Long userId, @PathVariable Long potId, @RequestParam(required = false) Long cursor, @RequestParam(defaultValue = "30") int size) {
        return chatService.getMessages(userId, potId, cursor, size);
    }

    @Operation(summary = "채팅 메시지·퀵블록 전송")
    @PostMapping("/api/pots/{potId}/messages")
    public ChatMessageResponse sendMessage(@LoginUserId Long userId, @PathVariable Long potId, @Valid @RequestBody ChatMessageRequest request) {
        return chatService.sendMessage(userId, potId, request);
    }

    @Operation(summary = "퀵블록 문구 목록 조회")
    @GetMapping("/api/quick-blocks")
    public List<QuickBlockResponse> getQuickBlocks(@LoginUserId Long userId) {
        return chatService.getQuickBlocks(userId);
    }
}
