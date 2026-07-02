package com.sky.controller.user;

import com.sky.dto.OrdersSubmitDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.result.Result;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户端订单接口。
 */
@RestController("userOrderController")
@RequestMapping("/user/order")
@Api(tags = "用户端订单接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 提交订单。
     *
     * @param ordersSubmitDTO 前端提交的地址、金额、配送和餐具等信息
     * @return 新订单的编号、金额和下单时间
     */
    @PostMapping("/submit")
    @ApiOperation("用户下单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {
        log.info("用户下单，参数：{}", ordersSubmitDTO);
        return Result.success(orderService.submitOrder(ordersSubmitDTO));
    }

    /**
     * 本地模拟支付：不调用微信支付，直接将订单改为已支付、待接单。
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付（本地模拟）")
    public Result<String> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) {
        orderService.payment(ordersPaymentDTO);
        return Result.success();
    }

    /** 分页查询当前用户的历史订单。 */
    @GetMapping("/historyOrders")
    @ApiOperation("查询历史订单")
    public Result<PageResult> historyOrders(int page, int pageSize, Integer status) {
        return Result.success(orderService.pageQueryForUser(page, pageSize, status));
    }

    /** 查询当前用户的订单详情。 */
    @GetMapping("/orderDetail/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> details(@PathVariable Long id) {
        return Result.success(orderService.details(id, true));
    }

    /** 取消当前用户的订单。 */
    @PutMapping("/cancel/{id}")
    @ApiOperation("取消订单")
    public Result<String> cancel(@PathVariable Long id) throws Exception {
        orderService.userCancelById(id);
        return Result.success();
    }

    /** 将历史订单商品重新加入购物车。 */
    @PostMapping("/repetition/{id}")
    @ApiOperation("再来一单")
    public Result<String> repetition(@PathVariable Long id) {
        orderService.repetition(id);
        return Result.success();
    }

    /** 用户催单，通过 WebSocket 向管理端发送提醒。 */
    @GetMapping("/reminder/{id}")
    @ApiOperation("客户催单")
    public Result<String> reminder(@PathVariable Long id) {
        orderService.reminder(id);
        return Result.success();
    }
}
