package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.CouponPageQueryDTO;
import com.sky.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface CouponMapper {
    void insert(Coupon coupon);
    Page<Coupon> pageQuery(CouponPageQueryDTO query);
    Coupon getById(Long id);
    Coupon getByIdForUpdate(Long id);
    void update(Coupon coupon);
    int decreaseStock(Long id);
    int updateStatusIfExpected(@Param("id") Long id,
                               @Param("expectedStatus") Integer expectedStatus,
                               @Param("targetStatus") Integer targetStatus,
                               @Param("updateUser") Long updateUser);
    void deleteById(Long id);
    Page<Coupon> availableForReceive(@Param("userId") Long userId);
    List<Coupon> availableForOrder(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
