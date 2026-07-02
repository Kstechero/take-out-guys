package com.sky.mapper;

import com.sky.entity.Orders;
import com.sky.dto.OrdersPageQueryDTO;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.sky.dto.GoodsSalesDTO;

/**
 * 订单数据访问层。
 */
@Mapper
public interface OrderMapper {

    /**
     * 新增订单，并将数据库生成的主键回填到订单对象。
     */
    void insert(Orders orders);

    /** 动态修改订单字段。 */
    void update(Orders orders);

    /** 管理端或用户端分页条件查询。 */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /** 根据主键查询订单。 */
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    /** 统计指定状态的订单数量。 */
    @Select("select count(id) from orders where status = #{status}")
    Integer countByStatus(Integer status);

    /** 根据订单号和用户 id 查询订单，防止支付其他用户的订单。 */
    @Select("select * from orders where number = #{orderNumber} and user_id = #{userId}")
    Orders getByNumberAndUserId(@org.apache.ibatis.annotations.Param("orderNumber") String orderNumber,
                                @org.apache.ibatis.annotations.Param("userId") Long userId);

    /** 微信支付回调根据订单号查询订单。 */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /** 查询指定时间之前仍未支付的订单。 */
    @Select("select * from orders where status = #{status} and order_time < #{time}")
    List<Orders> getByStatusAndOrderTimeBefore(@Param("status") Integer status,
                                                @Param("time") LocalDateTime time);

    /** 查询开始派送时间早于指定时间且仍在派送中的订单。 */
    @Select("select * from orders where status = #{status} and delivery_time < #{time}")
    List<Orders> getByStatusAndDeliveryTimeBefore(@Param("status") Integer status,
                                                   @Param("time") LocalDateTime time);

    Double sumByMap(Map<String, Object> map);
    Integer countByMap(Map<String, Object> map);
    List<GoodsSalesDTO> getSalesTop10(@Param("begin") LocalDateTime begin,
                                      @Param("end") LocalDateTime end);
}
