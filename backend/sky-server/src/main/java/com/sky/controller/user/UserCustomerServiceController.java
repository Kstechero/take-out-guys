package com.sky.controller.user;

import com.sky.dto.CustomerServiceMessageDTO;
import com.sky.dto.CustomerServiceSessionCreateDTO;
import com.sky.dto.ServiceSessionEndDTO;
import com.sky.result.Result;
import com.sky.service.CustomerServiceService;
import com.sky.vo.CustomerServiceMessageVO;
import com.sky.vo.CustomerServiceSessionVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user/service")
@Api(tags = "用户端人工客服")
public class UserCustomerServiceController {

    private final CustomerServiceService customerServiceService;

    public UserCustomerServiceController(CustomerServiceService customerServiceService) {
        this.customerServiceService = customerServiceService;
    }

    @PostMapping("/session")
    @ApiOperation("创建人工客服会话")
    public Result<CustomerServiceSessionVO> createSession(@RequestBody(required = false) CustomerServiceSessionCreateDTO dto) {
        return Result.success(customerServiceService.createOrGetSession(dto));
    }

    @GetMapping("/session/current")
    @ApiOperation("查询当前客服会话")
    public Result<CustomerServiceSessionVO> current() {
        return Result.success(customerServiceService.currentSession());
    }

    @PostMapping("/message")
    @ApiOperation("发送客服消息")
    public Result<String> send(@RequestBody CustomerServiceMessageDTO dto) {
        customerServiceService.sendUserMessage(dto);
        return Result.success();
    }

    @GetMapping("/message/list")
    @ApiOperation("查询客服消息列表")
    public Result<List<CustomerServiceMessageVO>> listMessages(@RequestParam Long sessionId,
                                                               @RequestParam(required = false) Long lastMessageId) {
        return Result.success(customerServiceService.listUserMessages(sessionId, lastMessageId));
    }

    @PostMapping("/session/end")
    @ApiOperation("结束当前客服会话")
    public Result<String> end(@RequestBody ServiceSessionEndDTO dto) {
        customerServiceService.endUserSession(dto);
        return Result.success();
    }
}
