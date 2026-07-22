package com.sky.controller.admin;

import com.sky.dto.CustomerServiceReplyDTO;
import com.sky.dto.CustomerServiceSessionPageQueryDTO;
import com.sky.dto.ServiceSessionEndDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CustomerServiceService;
import com.sky.vo.CustomerServiceMessageVO;
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
@RequestMapping("/admin/service")
@Api(tags = "管理端人工客服")
public class AdminCustomerServiceController {

    private final CustomerServiceService customerServiceService;

    public AdminCustomerServiceController(CustomerServiceService customerServiceService) {
        this.customerServiceService = customerServiceService;
    }

    @GetMapping("/session/page")
    @ApiOperation("分页查询客服会话")
    public Result<PageResult> page(CustomerServiceSessionPageQueryDTO queryDTO) {
        return Result.success(customerServiceService.pageSessions(queryDTO));
    }

    @GetMapping("/message/list")
    @ApiOperation("查询会话消息")
    public Result<List<CustomerServiceMessageVO>> listMessages(@RequestParam Long sessionId,
                                                               @RequestParam(required = false) Long lastMessageId) {
        return Result.success(customerServiceService.listAdminMessages(sessionId, lastMessageId));
    }

    @PostMapping("/message/reply")
    @ApiOperation("回复用户消息")
    public Result<String> reply(@RequestBody CustomerServiceReplyDTO dto) {
        customerServiceService.reply(dto);
        return Result.success();
    }

    @PostMapping("/session/end")
    @ApiOperation("结束客服会话")
    public Result<String> end(@RequestBody ServiceSessionEndDTO dto) {
        customerServiceService.endAdminSession(dto);
        return Result.success();
    }
}
