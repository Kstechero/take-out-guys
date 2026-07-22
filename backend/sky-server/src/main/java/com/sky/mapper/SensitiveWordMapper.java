package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.SensitiveWordPageQueryDTO;
import com.sky.entity.SensitiveWord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SensitiveWordMapper {
    void insert(SensitiveWord word);

    void update(SensitiveWord word);

    SensitiveWord getById(@Param("id") Long id);

    SensitiveWord getByWord(@Param("word") String word);

    Page<SensitiveWord> pageQuery(SensitiveWordPageQueryDTO queryDTO);

    List<SensitiveWord> listActive();

    void deleteBatch(@Param("ids") Long[] ids);

    void increaseHitCount(@Param("ids") List<Long> ids);
}
