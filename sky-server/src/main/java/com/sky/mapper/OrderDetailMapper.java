package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 订单明细数据访问层。
 */
@Mapper
public interface OrderDetailMapper {

    /** 批量新增订单明细。 */
    void insertBatch(List<OrderDetail> orderDetailList);
}
