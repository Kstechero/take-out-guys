package com.sky.service;

import com.sky.dto.UserAiChatRequestDTO;
import com.sky.vo.AiSessionVO;
import com.sky.vo.UserAiChatResponseVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface UserAiChatService {
    UserAiChatResponseVO chat(UserAiChatRequestDTO request);

    SseEmitter stream(Long sessionId, String message);

    List<AiSessionVO> listSessions();

    void deleteSession(Long sessionId);
}
