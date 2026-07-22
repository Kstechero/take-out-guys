package com.sky.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SensitiveWordCheckVO implements Serializable {
    private Boolean hit;
    private String content;
    private List<String> words;
}
