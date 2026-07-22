package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 购物车数据访问层。
 */
@Mapper
public interface ShoppingCartMapper {

    /**
     * 根据用户和商品信息查询购物车中的同一项。
     */
    List<ShoppingCart> listByUserIdAndGoods(ShoppingCart shoppingCart);

    /** 根据购物车主键和当前用户查询，保证不能操作其他用户的记录。 */
    @Select("select * from shopping_cart where id = #{id} and user_id = #{userId}")
    ShoppingCart getByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /** 新增一条购物车记录。 */
    @Insert("insert into shopping_cart(name,user_id,dish_id,setmeal_id,dish_flavor,number,amount,image,create_time) " +
            "values(#{name},#{userId},#{dishId},#{setmealId},#{dishFlavor},#{number},#{amount},#{image},#{createTime})")
    void insert(ShoppingCart shoppingCart);

    /** 修改购物车商品数量。 */
    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void updateNumber(ShoppingCart shoppingCart);

    /** 根据主键删除一条购物车记录。 */
    @Delete("delete from shopping_cart where id = #{id}")
    void deleteById(Long id);

    /** 查询指定用户的全部购物车记录。 */
    @Select("select * from shopping_cart where user_id = #{userId} order by create_time desc")
    List<ShoppingCart> listByUserId(Long userId);

    /** 清空指定用户的购物车。 */
    @Delete("delete from shopping_cart where user_id = #{userId}")
    void deleteByUserId(Long userId);

    /** 批量添加购物车记录，用于“再来一单”。 */
    void insertBatch(@Param("shoppingCartList") List<ShoppingCart> shoppingCartList);
}
