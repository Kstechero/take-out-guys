package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;


@Mapper
public interface SetmealDishMapper {
    /**
     * 根据菜品id查询对应的套餐id
     * @param dishIds
     * @return
     */
    //select setmeal_id from setmeal_dish where dish_id in (1,2,3)
    List<Long> getSetmealIdsByDishIds(@Param("dishIds") List<Long> dishIds);

    void insertBatch(@Param("setmealDishes") List<SetmealDish> setmealDishes);

    @Delete("delete from setmeal_dish where setmeal_id = #{setmealId}")
    void deleteBySetmealId(Long setmealId);

    void deleteBySetmealIds(@Param("setmealIds") List<Long> setmealIds);

    @Select("select * from setmeal_dish where setmeal_id = #{setmealId}")
    List<SetmealDish> getBySetmealId(Long setmealId);

    Integer countBySetmealIdAndDishStatus(@Param("setmealId") Long setmealId, @Param("status") Integer status);

    
}
