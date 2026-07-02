package com.sky.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import com.sky.entity.Dish;
import com.sky.dto.DishDTO;
import com.sky.result.Result;
import com.sky.vo.DishVO;
import com.sky.service.DishService;
import com.sky.result.PageResult;
import com.sky.dto.DishPageQueryDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Set;


@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result<String> save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品：{}", dishDTO);
        dishService.saveWithFlavor(dishDTO);
        // 清理该菜品所属分类的缓存数据
        cleanCache("dish_" + dishDTO.getCategoryId());
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品分页查询：{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 删除菜品
     * @param ids
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> list(Long categoryId) {
        log.info("根据分类id查询菜品：{}", categoryId);
        List<Dish> list = dishService.list(categoryId);
        return Result.success(list);
    }

    @DeleteMapping
    @ApiOperation("删除菜品")
    @CacheEvict(cacheNames = "setmealDish", allEntries = true)
    public Result delete(@RequestParam(required = false) List<Long> ids) {
        log.info("批量删除菜品：{}", ids);
        dishService.deleteBatch(ids);
        // 删除菜品后清理所有菜品缓存
        cleanCache("dish_*");
        return Result.success();
    }

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id) {
        log.info("根据id查询菜品：{}", id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 修改菜品信息
     * @param dishDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改菜品信息")
    @CacheEvict(cacheNames = "setmealDish", allEntries = true)
    public Result<DishDTO> getByIdWithFlavor(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品信息：{}", dishDTO);
        dishService.updateWithFlavor(dishDTO);
        // 菜品可能更换分类，因此清理所有菜品缓存
        cleanCache("dish_*");
        return Result.success(dishDTO);
    }

    /**
     * 菜品起售、停售
     *
     * @param status 菜品状态：1为起售，0为停售
     * @param id 菜品id
     * @return 操作结果
     */
    @PostMapping("/status/{status}")
    @ApiOperation("菜品起售、停售")
    public Result<String> startOrStop(@PathVariable Integer status, @RequestParam Long id) {
        log.info("菜品起售、停售：status={}, id={}", status, id);
        dishService.startOrStop(status, id);
        // 状态变更会影响用户端可查询的菜品，需要清理菜品缓存
        cleanCache("dish_*");
        return Result.success();
    }

    /**
     * 根据键的匹配模式清理 Redis 中的菜品缓存
     *
     * @param pattern Redis 键的匹配模式
     */
    private void cleanCache(String pattern) {
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }
}
