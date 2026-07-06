package com.sky.controller.user;

import com.sky.dto.ReviewSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.ReviewService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/user")
@Api(tags = "用户端评价")
public class UserReviewController {

    private final ReviewService reviewService;

    public UserReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/dish/{id}/reviews")
    @ApiOperation("分页查询菜品评价")
    public Result<PageResult> pageByDish(@PathVariable Long id,
                                         @RequestParam(required = false) Integer page,
                                         @RequestParam(required = false) Integer pageSize) {
        return Result.success(reviewService.pageByDishId(id, page, pageSize));
    }

    @PostMapping("/review")
    @ApiOperation("提交订单菜品评价")
    public Result<String> submit(@RequestBody ReviewSubmitDTO dto) {
        reviewService.submit(dto);
        return Result.success();
    }

    @PostMapping("/review/{id}/like")
    @ApiOperation("点赞或取消点赞评价")
    public Result<String> toggleLike(@PathVariable Long id) {
        reviewService.toggleLike(id);
        return Result.success();
    }

    @DeleteMapping("/review/{id}")
    @ApiOperation("删除本人评价")
    public Result<String> delete(@PathVariable Long id) {
        reviewService.delete(id);
        return Result.success();
    }

    @GetMapping("/order/{orderId}/review/status")
    @ApiOperation("查询订单评价状态")
    public Result<Map<String, Object>> reviewStatus(@PathVariable Long orderId) {
        return Result.success(reviewService.reviewStatus(orderId));
    }
}
