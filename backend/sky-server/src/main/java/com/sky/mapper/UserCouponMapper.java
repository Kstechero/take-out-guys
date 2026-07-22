package com.sky.mapper;

import com.sky.entity.UserCoupon;
import com.sky.vo.UserCouponVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface UserCouponMapper {
    @Select("select count(*) from user_coupon where user_id=#{userId} and coupon_id=#{couponId}")
    int countByUserAndCoupon(@Param("userId") Long userId, @Param("couponId") Long couponId);

    @Select("select count(*) from user_coupon where coupon_id=#{couponId}")
    int countByCouponId(Long couponId);

    @Insert("insert into user_coupon(user_id,coupon_id,status,receive_time,expire_time) values(#{userId},#{couponId},#{status},#{receiveTime},#{expireTime})")
    void insert(UserCoupon userCoupon);

    List<UserCouponVO> listByUser(@Param("userId") Long userId, @Param("status") Integer status);

    UserCoupon getUnusedForUpdate(@Param("userId") Long userId, @Param("couponId") Long couponId);
    int markUsed(@Param("id") Long id, @Param("orderId") Long orderId,
                 @Param("useTime") java.time.LocalDateTime useTime);
    int restoreByOrderId(Long orderId);
}
