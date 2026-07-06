package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.context.BaseContext;
import com.sky.dto.ReviewSubmitDTO;
import com.sky.entity.DishReview;
import com.sky.entity.DishReviewLike;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.exception.BaseException;
import com.sky.mapper.DishReviewLikeMapper;
import com.sky.mapper.DishReviewMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.result.PageResult;
import com.sky.service.ReviewService;
import com.sky.service.SensitiveWordService;
import com.sky.vo.DishReviewVO;
import com.sky.vo.SensitiveWordCheckVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final DishReviewMapper dishReviewMapper;
    private final DishReviewLikeMapper dishReviewLikeMapper;
    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final SensitiveWordService sensitiveWordService;

    public ReviewServiceImpl(DishReviewMapper dishReviewMapper,
                             DishReviewLikeMapper dishReviewLikeMapper,
                             OrderMapper orderMapper,
                             OrderDetailMapper orderDetailMapper,
                             SensitiveWordService sensitiveWordService) {
        this.dishReviewMapper = dishReviewMapper;
        this.dishReviewLikeMapper = dishReviewLikeMapper;
        this.orderMapper = orderMapper;
        this.orderDetailMapper = orderDetailMapper;
        this.sensitiveWordService = sensitiveWordService;
    }

    @Override
    @Transactional
    public void submit(ReviewSubmitDTO dto) {
        if (dto == null || dto.getOrderId() == null || dto.getDishId() == null || dto.getRating() == null
                || !StringUtils.hasText(dto.getContent())) {
            throw new BaseException("评价参数不完整");
        }
        if (dto.getRating() < 1 || dto.getRating() > 5) {
            throw new BaseException("评分必须在1到5之间");
        }
        Long userId = BaseContext.getCurrentId();
        Orders order = orderMapper.getById(dto.getOrderId());
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BaseException("订单不存在");
        }
        if (!Orders.COMPLETED.equals(order.getStatus())) {
            throw new BaseException("仅已完成订单可评价");
        }
        if (dishReviewMapper.getByOrderAndDishAndUser(dto.getOrderId(), dto.getDishId(), userId) != null) {
            throw new BaseException("该菜品已评价");
        }
        List<OrderDetail> details = orderDetailMapper.getByOrderId(dto.getOrderId());
        boolean matched = false;
        for (OrderDetail detail : details) {
            if (dto.getDishId().equals(detail.getDishId())) {
                matched = true;
                break;
            }
        }
        if (!matched) {
            throw new BaseException("订单中未找到该菜品");
        }
        SensitiveWordCheckVO moderation = sensitiveWordService.scanText(dto.getContent());
        if (Boolean.TRUE.equals(moderation.getHit())) {
            throw new BaseException("评价内容包含敏感词：" + String.join("、", moderation.getWords()));
        }
        LocalDateTime now = LocalDateTime.now();
        DishReview review = new DishReview();
        review.setUserId(userId);
        review.setOrderId(dto.getOrderId());
        review.setDishId(dto.getDishId());
        review.setRating(dto.getRating());
        review.setContent(dto.getContent().trim());
        review.setImages(JSON.toJSONString(dto.getImages() == null ? Collections.emptyList() : dto.getImages()));
        review.setLikeCount(0);
        review.setStatus(1);
        review.setAiGenerated(0);
        review.setCreateTime(now);
        review.setUpdateTime(now);
        dishReviewMapper.insert(review);
    }

    @Override
    public PageResult pageByDishId(Long dishId, Integer page, Integer pageSize) {
        if (dishId == null) {
            throw new BaseException("菜品编号不能为空");
        }
        PageHelper.startPage(page == null || page < 1 ? 1 : page, pageSize == null || pageSize < 1 ? 10 : pageSize);
        Page<DishReviewVO> reviewPage = dishReviewMapper.pageByDishId(dishId, BaseContext.getCurrentId());
        for (DishReviewVO review : reviewPage.getResult()) {
            review.setImages(parseImages(review.getImagesJson()));
        }
        return new PageResult(reviewPage.getTotal(), reviewPage.getResult());
    }

    @Override
    @Transactional
    public void toggleLike(Long id) {
        if (id == null) {
            throw new BaseException("评价编号不能为空");
        }
        if (dishReviewMapper.getById(id) == null) {
            throw new BaseException("评价不存在");
        }
        Long userId = BaseContext.getCurrentId();
        DishReviewLike like = dishReviewLikeMapper.getByReviewIdAndUserId(id, userId);
        if (like == null) {
            DishReviewLike reviewLike = new DishReviewLike();
            reviewLike.setReviewId(id);
            reviewLike.setUserId(userId);
            reviewLike.setCreateTime(LocalDateTime.now());
            dishReviewLikeMapper.insert(reviewLike);
            dishReviewMapper.updateLikeCount(id, 1);
            return;
        }
        dishReviewLikeMapper.deleteById(like.getId());
        dishReviewMapper.updateLikeCount(id, -1);
    }

    @Override
    public void delete(Long id) {
        if (id == null) {
            throw new BaseException("评价编号不能为空");
        }
        DishReview review = dishReviewMapper.getById(id);
        if (review == null || !BaseContext.getCurrentId().equals(review.getUserId())) {
            throw new BaseException("评价不存在");
        }
        dishReviewMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> reviewStatus(Long orderId) {
        if (orderId == null) {
            throw new BaseException("订单编号不能为空");
        }
        Map<String, Object> result = new HashMap<>();
        int count = dishReviewMapper.countByOrderAndUser(orderId, BaseContext.getCurrentId());
        result.put("orderId", orderId);
        result.put("reviewed", count > 0);
        result.put("reviewCount", count);
        return result;
    }

    private List<String> parseImages(String imagesJson) {
        if (!StringUtils.hasText(imagesJson)) {
            return Collections.emptyList();
        }
        return JSON.parseArray(imagesJson, String.class);
    }
}
