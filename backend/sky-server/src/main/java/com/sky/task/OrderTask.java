package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserCouponMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单定时任务。
 *
 * <p>每分钟检查一次支付超时和派送超时订单。</p>
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserCouponMapper userCouponMapper;

    /**
     * 待付款订单超过 15 分钟后自动取消。
     */
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void cancelUnpaidOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(15);
        List<Orders> timeoutOrders = orderMapper.getByStatusAndOrderTimeBefore(
                Orders.PENDING_PAYMENT, deadline);

        if (timeoutOrders.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (Orders order : timeoutOrders) {
            Orders update = Orders.builder()
                    .id(order.getId())
                    .status(Orders.CANCELLED)
                    .cancelReason("支付超时，订单自动取消")
                    .cancelTime(now)
                    .build();
            orderMapper.update(update);
            userCouponMapper.restoreByOrderId(order.getId());
        }
        log.info("Spring Task 已自动取消 {} 个支付超时订单", timeoutOrders.size());
    }

    /**
     * 开始派送后超过 1 小时仍未点击完成的订单自动完成。
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void completeDeliveryOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusHours(1);
        List<Orders> timeoutOrders = orderMapper.getByStatusAndDeliveryTimeBefore(
                Orders.DELIVERY_IN_PROGRESS, deadline);

        if (timeoutOrders.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (Orders order : timeoutOrders) {
            Orders update = Orders.builder()
                    .id(order.getId())
                    .status(Orders.COMPLETED)
                    // 自动完成时将 deliveryTime 更新为实际完成时间。
                    .deliveryTime(now)
                    .build();
            orderMapper.update(update);
        }
        log.info("Spring Task 已自动完成 {} 个派送超时订单", timeoutOrders.size());
    }
}
