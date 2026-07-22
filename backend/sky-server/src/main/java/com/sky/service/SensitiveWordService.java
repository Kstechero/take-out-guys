package com.sky.service;

import com.sky.dto.SensitiveWordCheckDTO;
import com.sky.dto.SensitiveWordDTO;
import com.sky.dto.SensitiveWordPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.SensitiveWordCheckVO;

public interface SensitiveWordService {
    void save(SensitiveWordDTO dto);

    void update(Long id, SensitiveWordDTO dto);

    void deleteBatch(Long[] ids);

    PageResult pageQuery(SensitiveWordPageQueryDTO queryDTO);

    SensitiveWordCheckVO check(SensitiveWordCheckDTO dto);

    SensitiveWordCheckVO scanText(String content);
}
