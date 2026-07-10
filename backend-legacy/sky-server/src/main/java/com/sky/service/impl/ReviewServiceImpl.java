package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.context.BaseContext;
import com.sky.dto.AdminReviewPageQueryDTO;
import com.sky.dto.ReviewPageQueryDTO;
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
import com.sky.vo.AdminReviewPageVO;
import com.sky.vo.DishReviewVO;
import com.sky.vo.SensitiveWordCheckVO;
import com.sky.vo.UserReviewPageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public PageResult pageForAdmin(AdminReviewPageQueryDTO queryDTO) {
        int page = queryDTO == null || queryDTO.getPage() == null || queryDTO.getPage() < 1 ? 1 : queryDTO.getPage();
        int pageSize = queryDTO == null || queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1 ? 10 : queryDTO.getPageSize();
        PageHelper.startPage(page, pageSize);
        Page<AdminReviewPageVO> reviewPage = dishReviewMapper.pageForAdmin(queryDTO);
        for (AdminReviewPageVO review : reviewPage.getResult()) {
            review.setImages(parseImages(review.getImagesJson()));
        }
        return new PageResult(reviewPage.getTotal(), reviewPage.getResult());
    }

    @Override
    @Transactional
    public void updateStatusByAdmin(Long id, Integer status) {
        if (id == null) {
            throw new BaseException("Review id cannot be null");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BaseException("Review status is invalid");
        }
        if (dishReviewMapper.getById(id) == null) {
            throw new BaseException("Review does not exist");
        }
        dishReviewMapper.updateStatus(id, status, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void deleteByAdmin(Long id) {
        if (id == null) {
            throw new BaseException("Review id cannot be null");
        }
        if (dishReviewMapper.getById(id) == null) {
            throw new BaseException("Review does not exist");
        }
        dishReviewLikeMapper.deleteByReviewId(id);
        dishReviewMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void submit(ReviewSubmitDTO dto) {
        if (dto == null || dto.getOrderId() == null || dto.getDishId() == null || dto.getRating() == null
                || !StringUtils.hasText(dto.getContent())) {
            throw new BaseException("Review parameters are incomplete");
        }
        if (dto.getRating() < 1 || dto.getRating() > 5) {
            throw new BaseException("Rating must be between 1 and 5");
        }

        Long userId = BaseContext.getCurrentId();
        Orders order = orderMapper.getById(dto.getOrderId());
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BaseException("Order does not exist");
        }
        if (!Orders.COMPLETED.equals(order.getStatus())) {
            throw new BaseException("Only completed orders can be reviewed");
        }
        if (dishReviewMapper.getByOrderAndDishAndUser(dto.getOrderId(), dto.getDishId(), userId) != null) {
            throw new BaseException("This dish has already been reviewed");
        }

        List<OrderDetail> details = orderDetailMapper.getByOrderId(dto.getOrderId());
        boolean matched = false;
        for (OrderDetail detail : details) {
            if (detail != null && dto.getDishId().equals(detail.getDishId())) {
                matched = true;
                break;
            }
        }
        if (!matched) {
            throw new BaseException("The dish was not found in the order");
        }

        SensitiveWordCheckVO moderation = sensitiveWordService.scanText(dto.getContent());
        if (Boolean.TRUE.equals(moderation.getHit())) {
            throw new BaseException("Review content contains sensitive words: " + String.join(", ", moderation.getWords()));
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
    public PageResult pageByCurrentUser(ReviewPageQueryDTO queryDTO) {
        int page = queryDTO == null || queryDTO.getPage() == null || queryDTO.getPage() < 1 ? 1 : queryDTO.getPage();
        int pageSize = queryDTO == null || queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1 ? 10 : queryDTO.getPageSize();
        PageHelper.startPage(page, pageSize);
        Page<UserReviewPageVO> reviewPage = dishReviewMapper.pageByUserId(BaseContext.getCurrentId(), queryDTO);
        for (UserReviewPageVO review : reviewPage.getResult()) {
            review.setImages(parseImages(review.getImagesJson()));
        }
        return new PageResult(reviewPage.getTotal(), reviewPage.getResult());
    }

    @Override
    public PageResult pageByDishId(Long dishId, Integer page, Integer pageSize) {
        if (dishId == null) {
            throw new BaseException("Dish id cannot be null");
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
            throw new BaseException("Review id cannot be null");
        }
        if (dishReviewMapper.getById(id) == null) {
            throw new BaseException("Review does not exist");
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
    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new BaseException("Review id cannot be null");
        }

        DishReview review = dishReviewMapper.getById(id);
        if (review == null || !BaseContext.getCurrentId().equals(review.getUserId())) {
            throw new BaseException("Review does not exist");
        }

        dishReviewLikeMapper.deleteByReviewId(id);
        dishReviewMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> reviewStatus(Long orderId) {
        if (orderId == null) {
            throw new BaseException("Order id cannot be null");
        }

        Long userId = BaseContext.getCurrentId();
        Orders order = orderMapper.getById(orderId);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BaseException("Order does not exist");
        }

        int count = dishReviewMapper.countByOrderAndUser(orderId, userId);
        int reviewableDishCount = collectReviewableDishIds(orderDetailMapper.getByOrderId(orderId)).size();
        boolean fullyReviewed = reviewableDishCount > 0 && count >= reviewableDishCount;

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("reviewed", fullyReviewed);
        result.put("partiallyReviewed", count > 0 && !fullyReviewed);
        result.put("reviewCount", count);
        result.put("reviewableDishCount", reviewableDishCount);
        result.put("pendingReviewCount", Math.max(reviewableDishCount - count, 0));
        return result;
    }

    private Set<Long> collectReviewableDishIds(List<OrderDetail> details) {
        if (details == null || details.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> result = new LinkedHashSet<>();
        for (OrderDetail detail : details) {
            if (detail != null && detail.getDishId() != null) {
                result.add(detail.getDishId());
            }
        }
        return result;
    }

    private List<String> parseImages(String imagesJson) {
        if (!StringUtils.hasText(imagesJson)) {
            return Collections.emptyList();
        }
        List<String> images = JSON.parseArray(imagesJson, String.class);
        return images == null ? new ArrayList<>() : images;
    }
}
