package com.sky.service;

import com.sky.dto.AdminAiChatRequestDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.Map;

public interface AdminAiChatService {
    SseEmitter stream(AdminAiChatRequestDTO request);
    Map<String, Object> health();
}
