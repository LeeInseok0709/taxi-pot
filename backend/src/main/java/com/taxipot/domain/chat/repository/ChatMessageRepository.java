package com.taxipot.domain.chat.repository;

import com.taxipot.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** 커서 방식: 최신부터 (cursor = 마지막으로 받은 id, 그보다 작은 id) */
    List<ChatMessage> findAllByPotIdOrderByIdDesc(Long potId, Pageable pageable);

    List<ChatMessage> findAllByPotIdAndIdLessThanOrderByIdDesc(Long potId, Long cursor, Pageable pageable);
}
