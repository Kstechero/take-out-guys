package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.OrderService;
import com.sky.result.PageResult;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单业务实现。
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;

    /** 开发环境开启时，不实际调用微信退款接口。 */
    @Value("${sky.payment.mock-enabled:true}")
    private boolean mockPaymentEnabled;

    /**
     * 用户下单。
     * 订单、订单明细和购物车清理必须作为一个事务执行，避免产生残缺订单。
     */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        Long userId = BaseContext.getCurrentId();

        // 校验地址存在，并确保该地址属于当前登录用户。
        AddressBook addressBook = ordersSubmitDTO == null || ordersSubmitDTO.getAddressBookId() == null
                ? null : addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null || !userId.equals(addressBook.getUserId())) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 查询当前用户购物车，空购物车不能下单。
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.listByUserId(userId);
        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 将提交参数与收货信息组装成订单主表数据。
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setUserId(userId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(addressBook.getProvinceName()
                + addressBook.getCityName()
                + addressBook.getDistrictName()
                + addressBook.getDetail());
        orderMapper.insert(orders);

        // 每一条购物车记录对应一条订单明细，并关联刚生成的订单 id。
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart shoppingCart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCart, orderDetail);
            orderDetail.setId(null);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);

        // 下单成功后清空当前用户购物车。
        shoppingCartMapper.deleteByUserId(userId);

        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
    }

    /**
     * 本地模拟支付。
     * 校验订单属于当前用户且仍为待付款后，直接更新支付状态和订单状态。
     */
    @Override
    public void payment(OrdersPaymentDTO paymentDTO) {
        if (paymentDTO == null || paymentDTO.getOrderNumber() == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        Orders orders = orderMapper.getByNumberAndUserId(
                paymentDTO.getOrderNumber(), BaseContext.getCurrentId());
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 前端可能因重复点击或网络重试多次调用支付接口。
        // 订单已经完成模拟支付时直接返回成功，保证接口幂等。
        if (Orders.TO_BE_CONFIRMED.equals(orders.getStatus())
                && Orders.PAID.equals(orders.getPayStatus())) {
            return;
        }

        if (!Orders.PENDING_PAYMENT.equals(orders.getStatus())
                || !Orders.UN_PAID.equals(orders.getPayStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders update = Orders.builder()
                .id(orders.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .payMethod(paymentDTO.getPayMethod())
                .checkoutTime(LocalDateTime.now())
                .build();
        orderMapper.update(update);

        // 模拟支付没有微信回调，因此在这里直接向管理端发送来单提醒。
        pushOrderMessage(1, orders.getId(), "订单号：" + orders.getNumber());
    }

    /**
     * 处理真实微信支付成功回调。重复回调时直接返回，避免重复推送。
     */
    @Override
    @Transactional
    public void paySuccess(String orderNumber) {
        Orders orders = orderMapper.getByNumber(orderNumber);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (Orders.PAID.equals(orders.getPayStatus())
                && Orders.TO_BE_CONFIRMED.equals(orders.getStatus())) {
            return;
        }
        if (!Orders.PENDING_PAYMENT.equals(orders.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders update = Orders.builder()
                .id(orders.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();
        orderMapper.update(update);
        pushOrderMessage(1, orders.getId(), "订单号：" + orderNumber);
    }

    /**
     * 当前用户只能催促自己的待接单、已接单或派送中订单。
     */
    @Override
    public void reminder(Long id) {
        Orders orders = getOwnedOrder(id);
        Integer status = orders.getStatus();
        if (!Orders.TO_BE_CONFIRMED.equals(status)
                && !Orders.CONFIRMED.equals(status)
                && !Orders.DELIVERY_IN_PROGRESS.equals(status)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        pushOrderMessage(2, id, "订单号：" + orders.getNumber());
    }

    /**
     * 查询当前登录用户的历史订单，并为每个订单装配订单明细。
     */
    @Override
    public PageResult pageQueryForUser(int page, int pageSize, Integer status) {
        PageHelper.startPage(page, pageSize);
        OrdersPageQueryDTO queryDTO = new OrdersPageQueryDTO();
        queryDTO.setUserId(BaseContext.getCurrentId());
        queryDTO.setStatus(status);

        Page<Orders> orderPage = orderMapper.pageQuery(queryDTO);
        List<OrderVO> records = buildOrderVOList(orderPage.getResult(), true);
        return new PageResult(orderPage.getTotal(), records);
    }

    /**
     * 查询订单详情。用户端调用时额外校验订单必须属于当前用户。
     */
    @Override
    public OrderVO details(Long id, boolean checkOwner) {
        Orders orders = getExistingOrder(id);
        if (checkOwner && !BaseContext.getCurrentId().equals(orders.getUserId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailMapper.getByOrderId(id));
        orderVO.setOrderDishes(buildOrderDishes(orderVO.getOrderDetailList()));
        return orderVO;
    }

    /**
     * 用户只能取消自己的待付款或待接单订单；已支付订单取消时申请退款。
     */
    @Override
    @Transactional
    public void userCancelById(Long id) throws Exception {
        Orders orders = getOwnedOrder(id);
        if (orders.getStatus() > Orders.TO_BE_CONFIRMED) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders update = new Orders();
        update.setId(id);
        if (Orders.PAID.equals(orders.getPayStatus())) {
            refundIfNecessary(orders);
            update.setPayStatus(Orders.REFUND);
        }
        update.setStatus(Orders.CANCELLED);
        update.setCancelReason("用户取消");
        update.setCancelTime(LocalDateTime.now());
        orderMapper.update(update);
    }

    /**
     * 将本人历史订单的全部明细复制到购物车。
     */
    @Override
    @Transactional
    public void repetition(Long id) {
        getOwnedOrder(id);
        List<OrderDetail> details = orderDetailMapper.getByOrderId(id);
        if (details == null || details.isEmpty()) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Long userId = BaseContext.getCurrentId();
        List<ShoppingCart> carts = details.stream().map(detail -> {
            ShoppingCart cart = new ShoppingCart();
            BeanUtils.copyProperties(detail, cart);
            cart.setId(null);
            cart.setUserId(userId);
            cart.setCreateTime(LocalDateTime.now());
            return cart;
        }).collect(Collectors.toList());
        shoppingCartMapper.insertBatch(carts);
    }

    /**
     * 管理端多条件分页搜索订单，同时返回“菜品名*数量”的摘要。
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO queryDTO) {
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize());
        Page<Orders> orderPage = orderMapper.pageQuery(queryDTO);
        return new PageResult(orderPage.getTotal(), buildOrderVOList(orderPage.getResult(), false));
    }

    /** 统计管理端各待处理状态的订单数。 */
    @Override
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO vo = new OrderStatisticsVO();
        vo.setToBeConfirmed(orderMapper.countByStatus(Orders.TO_BE_CONFIRMED));
        vo.setConfirmed(orderMapper.countByStatus(Orders.CONFIRMED));
        vo.setDeliveryInProgress(orderMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS));
        return vo;
    }

    /** 只有待接单订单可以接单。 */
    @Override
    public void confirm(OrdersConfirmDTO dto) {
        Orders orders = getExistingOrder(dto == null ? null : dto.getId());
        if (!Orders.TO_BE_CONFIRMED.equals(orders.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        orderMapper.update(Orders.builder().id(orders.getId()).status(Orders.CONFIRMED).build());
    }

    /** 只有待接单订单可以拒单，已支付时先申请退款。 */
    @Override
    @Transactional
    public void rejection(OrdersRejectionDTO dto) throws Exception {
        Orders orders = getExistingOrder(dto == null ? null : dto.getId());
        if (!Orders.TO_BE_CONFIRMED.equals(orders.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders update = new Orders();
        update.setId(orders.getId());
        if (Orders.PAID.equals(orders.getPayStatus())) {
            refundIfNecessary(orders);
            update.setPayStatus(Orders.REFUND);
        }
        update.setStatus(Orders.CANCELLED);
        update.setRejectionReason(dto.getRejectionReason());
        update.setCancelTime(LocalDateTime.now());
        orderMapper.update(update);
    }

    /** 商家取消未完成订单；已支付时先申请退款。 */
    @Override
    @Transactional
    public void cancel(OrdersCancelDTO dto) throws Exception {
        Orders orders = getExistingOrder(dto == null ? null : dto.getId());
        if (Orders.COMPLETED.equals(orders.getStatus()) || Orders.CANCELLED.equals(orders.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders update = new Orders();
        update.setId(orders.getId());
        if (Orders.PAID.equals(orders.getPayStatus())) {
            refundIfNecessary(orders);
            update.setPayStatus(Orders.REFUND);
        }
        update.setStatus(Orders.CANCELLED);
        update.setCancelReason(dto.getCancelReason());
        update.setCancelTime(LocalDateTime.now());
        orderMapper.update(update);
    }

    /** 已接单订单才能开始派送。 */
    @Override
    public void delivery(Long id) {
        Orders orders = getExistingOrder(id);
        if (!Orders.CONFIRMED.equals(orders.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        orderMapper.update(Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                // 派送中阶段暂用 deliveryTime 记录开始派送时间，供超时任务计算。
                .deliveryTime(LocalDateTime.now())
                .build());
    }

    /** 派送中的订单才能标记为完成。 */
    @Override
    public void complete(Long id) {
        Orders orders = getExistingOrder(id);
        if (!Orders.DELIVERY_IN_PROGRESS.equals(orders.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        orderMapper.update(Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build());
    }

    /** 查询订单，不存在时统一抛出业务异常。 */
    private Orders getExistingOrder(Long id) {
        Orders orders = id == null ? null : orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return orders;
    }

    /** 查询当前用户自己的订单。 */
    private Orders getOwnedOrder(Long id) {
        Orders orders = getExistingOrder(id);
        if (!BaseContext.getCurrentId().equals(orders.getUserId())) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return orders;
    }

    /**
     * 模拟支付环境只修改退款状态；关闭模拟开关后才调用真实微信退款。
     */
    private void refundIfNecessary(Orders orders) throws Exception {
        if (!mockPaymentEnabled) {
            weChatPayUtil.refund(orders.getNumber(), orders.getNumber(),
                    orders.getAmount(), orders.getAmount());
        }
    }

    /**
     * 推送订单消息：type=1 表示来单提醒，type=2 表示客户催单。
     */
    private void pushOrderMessage(int type, Long orderId, String content) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type);
        message.put("orderId", orderId);
        message.put("content", content);
        webSocketServer.sendToAllClient(JSON.toJSONString(message));
    }

    /** 将订单集合转换成前端需要的订单 VO。 */
    private List<OrderVO> buildOrderVOList(List<Orders> ordersList, boolean includeDetails) {
        List<OrderVO> result = new ArrayList<>();
        for (Orders orders : ordersList) {
            List<OrderDetail> details = orderDetailMapper.getByOrderId(orders.getId());
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(orders, vo);
            vo.setOrderDishes(buildOrderDishes(details));
            if (includeDetails) {
                vo.setOrderDetailList(details);
            }
            result.add(vo);
        }
        return result;
    }

    /** 按“菜品名*数量;”格式生成订单商品摘要。 */
    private String buildOrderDishes(List<OrderDetail> details) {
        return details.stream()
                .map(detail -> detail.getName() + "*" + detail.getNumber() + ";")
                .collect(Collectors.joining());
    }
}
