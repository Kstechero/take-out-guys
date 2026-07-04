package com.sky.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class CouponPageQueryDTO implements Serializable {
    private int page;
    private int pageSize;
    private String name;
    private Integer status;
}
