package com.sky.controller.admin;

import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端订单管理接口。
 */
@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Api(tags = "管理端订单接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /** 根据订单号、手机号、状态和时间范围分页搜索订单。 */
    @GetMapping("/conditionSearch")
    @ApiOperation("订单搜索")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO queryDTO) {
        return Result.success(orderService.conditionSearch(queryDTO));
    }

    /** 统计待接单、待派送和派送中的订单数量。 */
    @GetMapping("/statistics")
    @ApiOperation("订单数量统计")
    public Result<OrderStatisticsVO> statistics() {
        return Result.success(orderService.statistics());
    }

    /** 管理端查询订单及其明细。 */
    @GetMapping("/details/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> details(@PathVariable Long id) {
        return Result.success(orderService.details(id, false));
    }

    /** 商家接单。 */
    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result<String> confirm(@RequestBody OrdersConfirmDTO dto) {
        orderService.confirm(dto);
        return Result.success();
    }

    /** 商家拒单。 */
    @PutMapping("/rejection")
    @ApiOperation("拒单")
    public Result<String> rejection(@RequestBody OrdersRejectionDTO dto) throws Exception {
        orderService.rejection(dto);
        return Result.success();
    }

    /** 商家取消订单。 */
    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public Result<String> cancel(@RequestBody OrdersCancelDTO dto) throws Exception {
        orderService.cancel(dto);
        return Result.success();
    }

    /** 将已接单订单改为派送中。 */
    @PutMapping("/delivery/{id}")
    @ApiOperation("派送订单")
    public Result<String> delivery(@PathVariable Long id) {
        orderService.delivery(id);
        return Result.success();
    }

    /** 将派送中的订单改为已完成。 */
    @PutMapping("/complete/{id}")
    @ApiOperation("完成订单")
    public Result<String> complete(@PathVariable Long id) {
        orderService.complete(id);
        return Result.success();
    }
}
