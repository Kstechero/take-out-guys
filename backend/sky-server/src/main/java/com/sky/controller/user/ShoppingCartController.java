package com.sky.controller.user;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端购物车接口。
 */
@RestController
@RequestMapping("/user/shoppingCart")
@Api(tags = "用户端购物车接口")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    /** 添加一份菜品或套餐到购物车。 */
    @PostMapping("/add")
    @ApiOperation("添加购物车")
    public Result<String> add(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        shoppingCartService.addShoppingCart(shoppingCartDTO);
        return Result.success();
    }

    /** 从购物车中减少一份菜品或套餐。 */
    @PostMapping("/sub")
    @ApiOperation("减少购物车商品")
    public Result<String> sub(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        shoppingCartService.subShoppingCart(shoppingCartDTO);
        return Result.success();
    }

    /** 查询当前登录用户的购物车列表。 */
    @GetMapping("/list")
    @ApiOperation("查看购物车")
    public Result<List<ShoppingCart>> list() {
        return Result.success(shoppingCartService.showShoppingCart());
    }

    /** 清空当前登录用户的购物车。 */
    @DeleteMapping("/clean")
    @ApiOperation("清空购物车")
    public Result<String> clean() {
        shoppingCartService.cleanShoppingCart();
        return Result.success();
    }
}
