package com.sky.service;

import com.sky.dto.AdminReviewPageQueryDTO;
import com.sky.dto.ReviewPageQueryDTO;
import com.sky.dto.ReviewSubmitDTO;
import com.sky.result.PageResult;

import java.util.Map;

public interface ReviewService {
    PageResult pageForAdmin(AdminReviewPageQueryDTO queryDTO);

    void updateStatusByAdmin(Long id, Integer status);

    void deleteByAdmin(Long id);

    void submit(ReviewSubmitDTO dto);

    PageResult pageByCurrentUser(ReviewPageQueryDTO queryDTO);

    PageResult pageByDishId(Long dishId, Integer page, Integer pageSize);

    void toggleLike(Long id);

    void delete(Long id);

    Map<String, Object> reviewStatus(Long orderId);
}
