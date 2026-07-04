package com.sky.controller.admin;

import com.sky.dto.CouponDTO;
import com.sky.dto.CouponPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CouponService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("adminCouponController")
@RequestMapping("/admin/coupon")
@Api(tags = "管理端优惠券")
public class CouponController {
    @Autowired private CouponService couponService;

    @PostMapping
    @ApiOperation("新增优惠券")
    public Result<String> save(@RequestBody CouponDTO couponDTO) {
        couponService.save(couponDTO);
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("分页查询优惠券")
    public Result<PageResult> page(CouponPageQueryDTO query) {
        return Result.success(couponService.pageQuery(query));
    }

    @PutMapping("/{id}")
    @ApiOperation("按ID修改优惠券")
    public Result<String> update(@PathVariable Long id, @RequestBody CouponDTO couponDTO) {
        couponService.update(id, couponDTO);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除优惠券")
    public Result<String> delete(@PathVariable Long id) {
        couponService.delete(id);
        return Result.success();
    }
}
