package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.AdminReviewPageQueryDTO;
import com.sky.dto.ReviewPageQueryDTO;
import com.sky.entity.DishReview;
import com.sky.vo.AdminReviewPageVO;
import com.sky.vo.DishReviewVO;
import com.sky.vo.UserReviewPageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DishReviewMapper {
    void insert(DishReview review);

    Page<AdminReviewPageVO> pageForAdmin(@Param("queryDTO") AdminReviewPageQueryDTO queryDTO);

    Page<UserReviewPageVO> pageByUserId(@Param("userId") Long userId, @Param("queryDTO") ReviewPageQueryDTO queryDTO);

    Page<DishReviewVO> pageByDishId(@Param("dishId") Long dishId, @Param("currentUserId") Long currentUserId);

    DishReview getById(@Param("id") Long id);

    DishReview getByOrderAndDishAndUser(@Param("orderId") Long orderId, @Param("dishId") Long dishId, @Param("userId") Long userId);

    int countByOrderAndUser(@Param("orderId") Long orderId, @Param("userId") Long userId);

    void deleteById(@Param("id") Long id);

    void updateStatus(@Param("id") Long id, @Param("status") Integer status, @Param("updateTime") java.time.LocalDateTime updateTime);

    void updateLikeCount(@Param("id") Long id, @Param("delta") int delta);
}
