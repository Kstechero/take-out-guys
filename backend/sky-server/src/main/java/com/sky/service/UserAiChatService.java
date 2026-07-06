package com.sky.service;

import com.sky.dto.AiRecommendRequestDTO;
import com.sky.dto.AiReviewWriteRequestDTO;
import com.sky.dto.UserAiChatRequestDTO;
import com.sky.vo.AiReviewWriteVO;
import com.sky.vo.AiSessionVO;
import com.sky.vo.AiRecommendItemVO;
import com.sky.vo.UserAiChatResponseVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface UserAiChatService {
    UserAiChatResponseVO chat(UserAiChatRequestDTO request);

    List<AiRecommendItemVO> recommend(AiRecommendRequestDTO request);

    AiReviewWriteVO writeReview(AiReviewWriteRequestDTO request);

    SseEmitter stream(Long sessionId, String message);

    List<AiSessionVO> listSessions();

    void deleteSession(Long sessionId);
}
