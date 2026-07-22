package com.sky.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class AiReviewWriteVO implements Serializable {
    private String content;
    private Boolean flagged;
    private List<String> sensitiveWords;
}
