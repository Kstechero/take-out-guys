package com.sky.service;

import com.sky.dto.AdminAiChatRequestDTO;
import com.sky.dto.UserAiConfirmationRequestDTO;
import com.sky.vo.UserAiChatResponseVO;
import com.sky.vo.AiSessionVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;
import java.util.Map;

public interface AdminAiChatService {
    SseEmitter stream(AdminAiChatRequestDTO request);
    UserAiChatResponseVO resume(UserAiConfirmationRequestDTO request);
    Map<String, Object> health();
    List<AiSessionVO> listSessions();
    List<Map<String, Object>> getSessionMessages(Long sessionId);
    void deleteSession(Long sessionId);
}
