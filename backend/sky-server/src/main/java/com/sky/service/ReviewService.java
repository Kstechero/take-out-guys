package com.sky.service;

import com.sky.dto.ReviewSubmitDTO;
import com.sky.result.PageResult;

import java.util.Map;

public interface ReviewService {
    void submit(ReviewSubmitDTO dto);

    PageResult pageByDishId(Long dishId, Integer page, Integer pageSize);

    void toggleLike(Long id);

    void delete(Long id);

    Map<String, Object> reviewStatus(Long orderId);
}
