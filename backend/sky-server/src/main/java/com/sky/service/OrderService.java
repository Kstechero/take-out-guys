package com.sky.service;

import com.sky.dto.OrdersSubmitDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.result.PageResult;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;

/**
 * 订单业务接口。
 */
public interface OrderService {

    /**
     * 当前登录用户提交订单。
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /** 本地模拟支付，不调用微信支付接口。 */
    void payment(OrdersPaymentDTO ordersPaymentDTO);

    /** 处理微信支付成功回调。 */
    void paySuccess(String orderNumber);

    /** 用户催单并通过 WebSocket 通知管理端。 */
    void reminder(Long id);

    /** 用户端分页查询历史订单。 */
    PageResult pageQueryForUser(int page, int pageSize, Integer status);

    /** 查询订单详情；用户端调用时校验订单归属。 */
    OrderVO details(Long id, boolean checkOwner);

    /** 用户取消自己的订单。 */
    void userCancelById(Long id) throws Exception;

    /** 将历史订单中的商品重新加入当前用户购物车。 */
    void repetition(Long id);

    /** 管理端按条件分页搜索订单。 */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /** 统计待接单、待派送和派送中的订单数量。 */
    OrderStatisticsVO statistics();

    /** 商家接单。 */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    /** 商家拒单。 */
    void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception;

    /** 商家取消订单。 */
    void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception;

    /** 订单开始派送。 */
    void delivery(Long id);

    /** 完成订单。 */
    void complete(Long id);

    /** Atomically advances an order when its current status matches the reviewed preview. */
    void transitionByAgent(Long id, Integer expectedStatus, Integer targetStatus);
}
