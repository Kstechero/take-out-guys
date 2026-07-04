package com.sky.controller.user;

import com.sky.dto.UserAiChatRequestDTO;
import com.sky.result.Result;
import com.sky.service.UserAiChatService;
import com.sky.vo.AiSessionVO;
import com.sky.vo.UserAiChatResponseVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/user/ai")
@Api(tags = "用户端 AI")
public class UserAiController {

    private final UserAiChatService userAiChatService;

    public UserAiController(UserAiChatService userAiChatService) {
        this.userAiChatService = userAiChatService;
    }

    @PostMapping("/chat")
    @ApiOperation("AI 对话")
    public Result<UserAiChatResponseVO> chat(@RequestBody UserAiChatRequestDTO request) {
        return Result.success(userAiChatService.chat(request));
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation("AI 流式对话（SSE）")
    public SseEmitter stream(@RequestParam(required = false) Long sessionId, @RequestParam String message) {
        return userAiChatService.stream(sessionId, message);
    }

    @GetMapping("/session/list")
    @ApiOperation("查询当前用户的 AI 会话")
    public Result<List<AiSessionVO>> listSessions() {
        return Result.success(userAiChatService.listSessions());
    }

    @DeleteMapping("/session/{sessionId}")
    @ApiOperation("删除 AI 会话")
    public Result<String> deleteSession(@PathVariable Long sessionId) {
        userAiChatService.deleteSession(sessionId);
        return Result.success();
    }
}
