package com.sky.controller.admin;

import com.sky.dto.AdminReviewPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.ReviewService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/review")
@Api(tags = "管理端评价管理")
public class AdminReviewController {

    private final ReviewService reviewService;

    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/page")
    @ApiOperation("分页查询评价")
    public Result<PageResult> page(AdminReviewPageQueryDTO queryDTO) {
        return Result.success(reviewService.pageForAdmin(queryDTO));
    }

    @PutMapping("/{id}/status/{status}")
    @ApiOperation("处理评价状态")
    public Result<String> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        reviewService.updateStatusByAdmin(id, status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除评价")
    public Result<String> delete(@PathVariable Long id) {
        reviewService.deleteByAdmin(id);
        return Result.success();
    }
}
