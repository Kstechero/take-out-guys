package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.entity.DishReview;
import com.sky.vo.DishReviewVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DishReviewMapper {
    void insert(DishReview review);

    Page<DishReviewVO> pageByDishId(@Param("dishId") Long dishId, @Param("currentUserId") Long currentUserId);

    DishReview getById(@Param("id") Long id);

    DishReview getByOrderAndDishAndUser(@Param("orderId") Long orderId, @Param("dishId") Long dishId, @Param("userId") Long userId);

    int countByOrderAndUser(@Param("orderId") Long orderId, @Param("userId") Long userId);

    void deleteById(@Param("id") Long id);

    void updateLikeCount(@Param("id") Long id, @Param("delta") int delta);
}
