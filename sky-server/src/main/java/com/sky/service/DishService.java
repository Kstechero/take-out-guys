package com.sky.service;



import com.sky.dto.DishDTO;
import com.sky.result.PageResult;
import com.sky.dto.DishPageQueryDTO;
import java.util.List;

public interface DishService {


    
    /**
     * 新增菜品，同时保存对应的口味数据
     * @param dishDto
     */
    public void saveWithFlavor(DishDTO dishDto);


    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);


    /**
     * 批量删除菜品
     * @param ids
     */
    void deleteBatch(List<Long> ids);
    
}
