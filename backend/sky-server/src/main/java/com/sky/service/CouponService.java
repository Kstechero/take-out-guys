package com.sky.service;

import com.sky.dto.CouponDTO;
import com.sky.dto.CouponPageQueryDTO;
import com.sky.entity.Coupon;
import com.sky.result.PageResult;
import com.sky.vo.UserCouponVO;
import java.math.BigDecimal;
import java.util.List;

public interface CouponService {
    void save(CouponDTO couponDTO);
    PageResult pageQuery(CouponPageQueryDTO query);
    void update(Long id, CouponDTO couponDTO);
    void delete(Long id);
    PageResult available(int page, int pageSize);
    void receive(Long couponId);
    List<UserCouponVO> myCoupons(Integer status);
    List<Coupon> availableForOrder(BigDecimal amount, Long orderId);
}
