package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 购物车业务实现。
 */
@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加商品：相同菜品（包含相同口味）或相同套餐已存在时，只增加数量。
     */
    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = buildQueryCondition(shoppingCartDTO);
        ShoppingCart existing = shoppingCartMapper.getByUserIdAndGoods(shoppingCart);
        if (existing != null) {
            existing.setNumber(existing.getNumber() + 1);
            shoppingCartMapper.updateNumber(existing);
            return;
        }

        // 首次加入购物车时，从菜品或套餐表补全展示信息。
        if (shoppingCartDTO.getDishId() != null) {
            Dish dish = dishMapper.getById(shoppingCartDTO.getDishId());
            if (dish == null) {
                throw new ShoppingCartBusinessException("菜品不存在");
            }
            shoppingCart.setName(dish.getName());
            shoppingCart.setImage(dish.getImage());
            shoppingCart.setAmount(dish.getPrice());
        } else if (shoppingCartDTO.getSetmealId() != null) {
            Setmeal setmeal = setmealMapper.getById(shoppingCartDTO.getSetmealId());
            if (setmeal == null) {
                throw new ShoppingCartBusinessException("套餐不存在");
            }
            shoppingCart.setName(setmeal.getName());
            shoppingCart.setImage(setmeal.getImage());
            shoppingCart.setAmount(setmeal.getPrice());
        } else {
            throw new ShoppingCartBusinessException("请选择菜品或套餐");
        }

        shoppingCart.setNumber(1);
        shoppingCart.setCreateTime(LocalDateTime.now());
        shoppingCartMapper.insert(shoppingCart);
    }

    /**
     * 减少一份商品；数量为 1 时直接删除该条记录。
     */
    @Override
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart existing = shoppingCartMapper.getByUserIdAndGoods(buildQueryCondition(shoppingCartDTO));
        if (existing == null) {
            throw new ShoppingCartBusinessException("购物车中不存在该商品");
        }
        if (existing.getNumber() > 1) {
            existing.setNumber(existing.getNumber() - 1);
            shoppingCartMapper.updateNumber(existing);
        } else {
            shoppingCartMapper.deleteById(existing.getId());
        }
    }

    @Override
    public List<ShoppingCart> showShoppingCart() {
        return shoppingCartMapper.listByUserId(BaseContext.getCurrentId());
    }

    @Override
    public void cleanShoppingCart() {
        shoppingCartMapper.deleteByUserId(BaseContext.getCurrentId());
    }

    /** 构造带有当前登录用户 id 的查询条件，防止操作其他用户的购物车。 */
    private ShoppingCart buildQueryCondition(ShoppingCartDTO dto) {
        // 菜品和套餐必须二选一，避免生成含义不明确的查询条件。
        if (dto == null || (dto.getDishId() == null) == (dto.getSetmealId() == null)) {
            throw new ShoppingCartBusinessException("菜品和套餐必须选择一个");
        }
        return ShoppingCart.builder()
                .userId(BaseContext.getCurrentId())
                .dishId(dto.getDishId())
                .setmealId(dto.getSetmealId())
                .dishFlavor(dto.getDishFlavor())
                .build();
    }
}
