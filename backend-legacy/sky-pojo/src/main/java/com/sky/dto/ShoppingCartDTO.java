package com.sky.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class ShoppingCartDTO implements Serializable {

    /** 购物车记录 id；减少购物车时优先使用，避免规格字符串不一致。 */
    private Long id;
    private Long dishId;
    private Long setmealId;
    private String dishFlavor;

}
