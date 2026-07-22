package com.sky.controller.user;

import com.sky.entity.Coupon;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CouponService;
import com.sky.vo.UserCouponVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController("userCouponController")
@RequestMapping("/user/coupon")
@Api(tags = "用户端优惠券")
public class CouponController {
    @Autowired private CouponService couponService;

    @GetMapping("/available")
    @ApiOperation("查询可领取优惠券")
    public Result<PageResult> available(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(couponService.available(page, pageSize));
    }

    @PostMapping("/receive/{couponId}")
    @ApiOperation("领取优惠券")
    public Result<String> receive(@PathVariable Long couponId) {
        couponService.receive(couponId);
        return Result.success();
    }

    @GetMapping("/my")
    @ApiOperation("查询我的优惠券")
    public Result<List<UserCouponVO>> myCoupons(@RequestParam(required = false) Integer status) {
        return Result.success(couponService.myCoupons(status));
    }

    @GetMapping("/order/available")
    @ApiOperation("查询当前订单可用优惠券")
    public Result<List<Coupon>> availableForOrder(@RequestParam BigDecimal amount,
                                                   @RequestParam(required = false) Long orderId) {
        return Result.success(couponService.availableForOrder(amount, orderId));
    }
}
