package com.sky.controller.admin;

import com.sky.dto.AdminAiChatRequestDTO;
import com.sky.service.AdminAiChatService;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
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

    @GetMapping("/health")
    @ApiOperation("检查 GX10 模型服务状态")
    public Result<Map<String, Object>> health() {
        return Result.success(adminAiChatService.health());
    }
}
