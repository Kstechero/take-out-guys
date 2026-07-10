package com.sky.controller.admin;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;


/**
 * 分类管理
 */
@RestController
@RequestMapping("/admin/category")
@Api(tags = "分类相关接口")
@Slf4j 
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增分类
     * @param categoryDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增分类")
    public Result<String> save(@RequestBody CategoryDTO categoryDTO){
        log.info("新增分类：{}",categoryDTO);
        categoryService.save(categoryDTO);
        // 分类数据发生变化，清理分页查询缓存
        cleanCache("category_page_*");
        return Result.success();
    }

    /**
     * 分类分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("分类分页查询")
    public Result<PageResult> page(CategoryPageQueryDTO categoryPageQueryDTO){
        log.info("分页查询：{}", categoryPageQueryDTO);

        // 将所有查询条件组合成唯一的 Redis 键
        String key = "category_page_"
                + categoryPageQueryDTO.getPage() + "_"
                + categoryPageQueryDTO.getPageSize() + "_"
                + categoryPageQueryDTO.getName() + "_"
                + categoryPageQueryDTO.getType();

        // 优先从 Redis 读取分页数据
        PageResult pageResult = (PageResult) redisTemplate.opsForValue().get(key);
        if (pageResult != null) {
            return Result.success(pageResult);
        }

        pageResult = categoryService.pageQuery(categoryPageQueryDTO);
        // 数据库查询结果写入 Redis
        redisTemplate.opsForValue().set(key, pageResult);
        return Result.success(pageResult);
    }

    /**
     * 删除分类
     * @param id
     * @return
     */
    @DeleteMapping
    @ApiOperation("删除分类")
    public Result<String> deleteById(Long id){
        log.info("删除分类：{}", id);
        categoryService.deleteById(id);
        // 分类数据发生变化，清理分页查询缓存
        cleanCache("category_page_*");
        return Result.success();
    }
    /**
     * 修改分类
     * @param categoryDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改分类")
    public Result<String> update(@RequestBody CategoryDTO categoryDTO){
        categoryService.update(categoryDTO);
        // 分类数据发生变化，清理分页查询缓存
        cleanCache("category_page_*");
        return Result.success();
    }

    /**
     * 启用、禁用分类
     *
     * @param status 分类状态：1为启用，0为禁用
     * @param id 分类id
     * @return 操作结果
     */
    @PostMapping("/status/{status}")
    @ApiOperation("启用禁用分类")
    public Result<String> startOrStop(@PathVariable Integer status, @RequestParam Long id){
        log.info("启用、禁用分类：status={}, id={}", status, id);
        categoryService.startOrStop(status, id);
        // 分类状态发生变化，清理分页查询缓存
        cleanCache("category_page_*");
        return Result.success();
    }

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据类型查询分类")
    public Result<List<Category>> list(Integer type){
        List<Category> list = categoryService.list(type);
        return Result.success(list);
    }

    /**
     * 根据键的匹配模式清理 Redis 缓存
     *
     * @param pattern Redis 键的匹配模式
     */
    private void cleanCache(String pattern) {
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }


}
