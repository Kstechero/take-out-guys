package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单明细数据访问层。
 */
@Mapper
public interface OrderDetailMapper {

    /** 批量新增订单明细。 */
    void insertBatch(@Param("orderDetailList") List<OrderDetail> orderDetailList);

    /** 查询指定订单的全部明细。 */
    @Select("select * from order_detail where order_id = #{orderId}")
    List<OrderDetail> getByOrderId(Long orderId);
}
