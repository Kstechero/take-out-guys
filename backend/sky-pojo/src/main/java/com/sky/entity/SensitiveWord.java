package com.sky.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SensitiveWord implements Serializable {
    private Long id;
    private String word;
    private Integer level;
    private String replacement;
    private Integer status;
    private Integer hitCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createUser;
    private Long updateUser;
}
