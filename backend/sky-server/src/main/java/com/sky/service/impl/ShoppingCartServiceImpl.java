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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    @Transactional
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = buildQueryCondition(shoppingCartDTO);
        List<ShoppingCart> existingRows = shoppingCartMapper.listByUserIdAndGoods(shoppingCart);
        if (existingRows != null && !existingRows.isEmpty()) {
            ShoppingCart existing = existingRows.get(0);
            int total = existingRows.stream().mapToInt(ShoppingCart::getNumber).sum() + 1;
            existing.setNumber(total);
            shoppingCartMapper.updateNumber(existing);
            // 自动清理历史版本产生的重复行，只保留第一条并合并数量。
            for (int i = 1; i < existingRows.size(); i++) {
                shoppingCartMapper.deleteById(existingRows.get(i).getId());
            }
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
        ShoppingCart existing = null;
        // 购物车列表携带记录 id，直接精确匹配并校验记录属于当前用户。
        if (shoppingCartDTO != null && shoppingCartDTO.getId() != null) {
            existing = shoppingCartMapper.getByIdAndUserId(
                    shoppingCartDTO.getId(), BaseContext.getCurrentId());
        }
        // 菜单及规格弹窗没有记录 id，继续按商品与规格匹配。
        if (existing == null) {
            List<ShoppingCart> rows = shoppingCartMapper.listByUserIdAndGoods(
                    buildQueryCondition(shoppingCartDTO));
            existing = rows == null || rows.isEmpty() ? null : rows.get(0);
        }
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
    @Transactional
    public List<ShoppingCart> showShoppingCart() {
        Long userId = BaseContext.getCurrentId();
        List<ShoppingCart> rows = shoppingCartMapper.listByUserId(userId);
        if (rows == null || rows.size() < 2) {
            return rows;
        }

        // 清理历史版本产生的重复行：同商品、同口味合并 number，其他规格保持独立。
        Map<String, ShoppingCart> firstByGoods = new LinkedHashMap<>();
        boolean merged = false;
        for (ShoppingCart row : rows) {
            String flavor = row.getDishFlavor() == null ? "" : row.getDishFlavor().trim();
            String key = String.valueOf(row.getDishId()) + '|'
                    + String.valueOf(row.getSetmealId()) + '|' + flavor;
            ShoppingCart first = firstByGoods.get(key);
            if (first == null) {
                firstByGoods.put(key, row);
                continue;
            }
            first.setNumber(first.getNumber() + row.getNumber());
            shoppingCartMapper.updateNumber(first);
            shoppingCartMapper.deleteById(row.getId());
            merged = true;
        }
        return merged ? shoppingCartMapper.listByUserId(userId) : rows;
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
        String dishFlavor = dto.getDishFlavor();
        if (dishFlavor != null && dishFlavor.trim().isEmpty()) {
            dishFlavor = null;
        }
        return ShoppingCart.builder()
                .userId(BaseContext.getCurrentId())
                .dishId(dto.getDishId())
                .setmealId(dto.getSetmealId())
                .dishFlavor(dishFlavor)
                .build();
    }
}
