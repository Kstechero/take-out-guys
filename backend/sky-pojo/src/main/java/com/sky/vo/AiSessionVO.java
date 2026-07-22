package com.sky.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AiSessionVO implements Serializable {
    private Long id;
    private String title;
    private String lastMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
