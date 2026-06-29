package com.sky.mapper;

import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单数据访问层。
 */
@Mapper
public interface OrderMapper {

    /**
     * 新增订单，并将数据库生成的主键回填到订单对象。
     */
    void insert(Orders orders);
}
