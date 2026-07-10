package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.context.BaseContext;
import com.sky.dto.SensitiveWordCheckDTO;
import com.sky.dto.SensitiveWordDTO;
import com.sky.dto.SensitiveWordPageQueryDTO;
import com.sky.entity.SensitiveWord;
import com.sky.exception.BaseException;
import com.sky.mapper.SensitiveWordMapper;
import com.sky.result.PageResult;
import com.sky.service.SensitiveWordService;
import com.sky.vo.SensitiveWordCheckVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class SensitiveWordServiceImpl implements SensitiveWordService {

    private final SensitiveWordMapper sensitiveWordMapper;

    public SensitiveWordServiceImpl(SensitiveWordMapper sensitiveWordMapper) {
        this.sensitiveWordMapper = sensitiveWordMapper;
    }

    @Override
    public void save(SensitiveWordDTO dto) {
        validate(dto);
        if (sensitiveWordMapper.getByWord(dto.getWord().trim()) != null) {
            throw new BaseException("敏感词已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        SensitiveWord word = new SensitiveWord();
        word.setWord(dto.getWord().trim());
        word.setLevel(dto.getLevel() == null ? 1 : dto.getLevel());
        word.setReplacement(normalizeReplacement(dto.getReplacement()));
        word.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        word.setHitCount(0);
        word.setCreateTime(now);
        word.setUpdateTime(now);
        word.setCreateUser(BaseContext.getCurrentId());
        word.setUpdateUser(BaseContext.getCurrentId());
        sensitiveWordMapper.insert(word);
    }

    @Override
    public void update(Long id, SensitiveWordDTO dto) {
        if (id == null) {
            throw new BaseException("敏感词编号不能为空");
        }
        validate(dto);
        SensitiveWord existing = sensitiveWordMapper.getById(id);
        if (existing == null) {
            throw new BaseException("敏感词不存在");
        }
        SensitiveWord duplicate = sensitiveWordMapper.getByWord(dto.getWord().trim());
        if (duplicate != null && !id.equals(duplicate.getId())) {
            throw new BaseException("敏感词已存在");
        }
        SensitiveWord word = new SensitiveWord();
        word.setId(id);
        word.setWord(dto.getWord().trim());
        word.setLevel(dto.getLevel() == null ? existing.getLevel() : dto.getLevel());
        word.setReplacement(normalizeReplacement(dto.getReplacement()));
        word.setStatus(dto.getStatus() == null ? existing.getStatus() : dto.getStatus());
        word.setUpdateTime(LocalDateTime.now());
        word.setUpdateUser(BaseContext.getCurrentId());
        sensitiveWordMapper.update(word);
    }

    @Override
    public void deleteBatch(Long[] ids) {
        if (ids == null || ids.length == 0) {
            throw new BaseException("请选择要删除的敏感词");
        }
        sensitiveWordMapper.deleteBatch(ids);
    }

    @Override
    public PageResult pageQuery(SensitiveWordPageQueryDTO queryDTO) {
        if (queryDTO == null || queryDTO.getPage() == null || queryDTO.getPageSize() == null
                || queryDTO.getPage() < 1 || queryDTO.getPageSize() < 1) {
            throw new BaseException("分页参数不正确");
        }
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize());
        Page<SensitiveWord> page = sensitiveWordMapper.pageQuery(queryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public SensitiveWordCheckVO check(SensitiveWordCheckDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getContent())) {
            throw new BaseException("检测内容不能为空");
        }
        return scanText(dto.getContent());
    }

    @Override
    public SensitiveWordCheckVO scanText(String content) {
        if (!StringUtils.hasText(content)) {
            SensitiveWordCheckVO result = new SensitiveWordCheckVO();
            result.setHit(false);
            result.setContent(content);
            result.setWords(Collections.emptyList());
            return result;
        }

        String normalized = content.trim();
        String masked = normalized;
        List<String> hits = new ArrayList<>();
        List<Long> hitIds = new ArrayList<>();
        for (SensitiveWord word : sensitiveWordMapper.listActive()) {
            if (word == null || !StringUtils.hasText(word.getWord())) {
                continue;
            }
            if (masked.contains(word.getWord())) {
                hits.add(word.getWord());
                hitIds.add(word.getId());
                masked = masked.replace(word.getWord(), normalizeReplacement(word.getReplacement()));
            }
        }
        if (!hitIds.isEmpty()) {
            sensitiveWordMapper.increaseHitCount(hitIds);
        }
        SensitiveWordCheckVO result = new SensitiveWordCheckVO();
        result.setHit(!hits.isEmpty());
        result.setContent(masked);
        result.setWords(hits);
        return result;
    }

    private void validate(SensitiveWordDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getWord())) {
            throw new BaseException("敏感词不能为空");
        }
        if (dto.getStatus() != null && dto.getStatus() != 0 && dto.getStatus() != 1) {
            throw new BaseException("状态只能为0或1");
        }
    }

    private String normalizeReplacement(String replacement) {
        return StringUtils.hasText(replacement) ? replacement.trim() : "***";
    }
}
