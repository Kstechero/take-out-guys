package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

/**
 * 购物车业务接口。
 */
public interface ShoppingCartService {

    /** 添加商品到购物车。 */
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    /** 从购物车中减少一份商品。 */
    void subShoppingCart(ShoppingCartDTO shoppingCartDTO);

    /** 查询当前登录用户的购物车。 */
    List<ShoppingCart> showShoppingCart();

    /** 清空当前登录用户的购物车。 */
    void cleanShoppingCart();
}
