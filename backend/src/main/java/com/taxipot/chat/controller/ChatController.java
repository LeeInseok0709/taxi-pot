package com.taxipot.chat.controller;

import com.taxipot.chat.dto.ChatMessagePageResponse;
import com.taxipot.chat.dto.ChatMessageRequest;
import com.taxipot.chat.dto.ChatMessageResponse;
import com.taxipot.chat.dto.QuickBlockResponse;
import com.taxipot.chat.service.ChatService;
import com.taxipot.global.auth.LoginUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 자동 생성 (명세 3.9.0). 담당: 인석
 * 컨트롤러는 수정하지 않는다 — 경로·파라미터·응답 타입이 명세와 1:1. 로직은 ChatService에만 작성.
 */
@Tag(name = "Chat")
@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** GET /api/pots/{potId}/messages — 담당 인석 */
    @Operation(summary = "채팅 메시지 조회 (이전 메시지, 무한 스크롤)")
    @GetMapping(value = "/api/pots/{potId}/messages")
    public ResponseEntity<ChatMessagePageResponse> getMessages(
            @LoginUserId Long userId,
            @PathVariable Long potId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "30") Integer size) {
        return ResponseEntity.ok(chatService.getMessages(userId, potId, cursor, size));
    }

    /** POST /api/pots/{potId}/messages — 담당 인석 */
    @Operation(summary = "채팅 메시지·퀵블록 전송")
    @PostMapping(value = "/api/pots/{potId}/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @LoginUserId Long userId,
            @PathVariable Long potId,
            @Valid @RequestBody ChatMessageRequest request) {
        return ResponseEntity.ok(chatService.sendMessage(userId, potId, request));
    }

    /** GET /api/quick-blocks — 담당 인석 */
    @Operation(summary = "퀵블록 문구 목록 조회")
    @GetMapping(value = "/api/quick-blocks")
    public ResponseEntity<List<QuickBlockResponse>> getQuickBlockList(
            @LoginUserId Long userId) {
        return ResponseEntity.ok(chatService.getQuickBlockList(userId));
    }
}
