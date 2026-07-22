package com.sky.controller.admin;

import com.sky.dto.AdminAiChatRequestDTO;
import com.sky.dto.UserAiConfirmationRequestDTO;
import com.sky.service.AdminAiChatService;
import com.sky.result.Result;
import com.sky.vo.UserAiChatResponseVO;
import com.sky.vo.AiSessionVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/ai")
@Api(tags = "管理端 AI Agent 接口")
public class AdminAiController {

    private final AdminAiChatService adminAiChatService;

    public AdminAiController(AdminAiChatService adminAiChatService) {
        this.adminAiChatService = adminAiChatService;
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation("管理端运营 Agent 流式对话")
    public SseEmitter stream(@RequestBody AdminAiChatRequestDTO request) {
        return adminAiChatService.stream(request);
    }

    @PostMapping("/chat/resume")
    @ApiOperation("恢复管理端 Agent 确认流程")
    public Result<UserAiChatResponseVO> resume(@RequestBody UserAiConfirmationRequestDTO request) {
        return Result.success(adminAiChatService.resume(request));
    }

    @GetMapping("/health")
    @ApiOperation("检查 GX10 模型服务状态")
    public Result<Map<String, Object>> health() {
        return Result.success(adminAiChatService.health());
    }

    @GetMapping("/session/list")
    @ApiOperation("查询当前管理员的 Agent 会话")
    public Result<List<AiSessionVO>> listSessions() {
        return Result.success(adminAiChatService.listSessions());
    }

    @GetMapping("/session/{sessionId}/messages")
    @ApiOperation("查询当前管理员的 Agent 会话消息")
    public Result<List<Map<String, Object>>> getSessionMessages(@PathVariable Long sessionId) {
        return Result.success(adminAiChatService.getSessionMessages(sessionId));
    }

    @DeleteMapping("/session/{sessionId}")
    @ApiOperation("删除当前管理员的 Agent 会话")
    public Result<String> deleteSession(@PathVariable Long sessionId) {
        adminAiChatService.deleteSession(sessionId);
        return Result.success();
    }
}
