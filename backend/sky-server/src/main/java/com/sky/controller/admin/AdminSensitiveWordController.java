package com.sky.controller.admin;

import com.sky.dto.SensitiveWordCheckDTO;
import com.sky.dto.SensitiveWordDTO;
import com.sky.dto.SensitiveWordPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SensitiveWordService;
import com.sky.vo.SensitiveWordCheckVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/sensitive-word")
@Api(tags = "敏感词管理")
public class AdminSensitiveWordController {

    private final SensitiveWordService sensitiveWordService;

    public AdminSensitiveWordController(SensitiveWordService sensitiveWordService) {
        this.sensitiveWordService = sensitiveWordService;
    }

    @PostMapping
    @ApiOperation("新增敏感词")
    public Result<String> save(@RequestBody SensitiveWordDTO dto) {
        sensitiveWordService.save(dto);
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("分页查询敏感词")
    public Result<PageResult> page(SensitiveWordPageQueryDTO queryDTO) {
        return Result.success(sensitiveWordService.pageQuery(queryDTO));
    }

    @PutMapping("/{id}")
    @ApiOperation("按ID修改敏感词")
    public Result<String> update(@PathVariable Long id, @RequestBody SensitiveWordDTO dto) {
        sensitiveWordService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/batch")
    @ApiOperation("批量删除敏感词")
    public Result<String> delete(@RequestParam Long[] ids) {
        sensitiveWordService.deleteBatch(ids);
        return Result.success();
    }

    @PostMapping("/check")
    @ApiOperation("检测文本中的敏感词")
    public Result<SensitiveWordCheckVO> check(@RequestBody SensitiveWordCheckDTO dto) {
        return Result.success(sensitiveWordService.check(dto));
    }
}
