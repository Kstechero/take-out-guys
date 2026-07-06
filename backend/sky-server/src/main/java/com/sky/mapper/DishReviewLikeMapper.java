package com.sky.mapper;

import com.sky.entity.DishReviewLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DishReviewLikeMapper {
    DishReviewLike getByReviewIdAndUserId(@Param("reviewId") Long reviewId, @Param("userId") Long userId);

    void insert(DishReviewLike reviewLike);

    void deleteById(@Param("id") Long id);
}
