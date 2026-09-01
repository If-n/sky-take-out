package com.sky.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加商品到购物车
     * @param shoppingCartDTO
     */
    @Override
    @Transactional
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        //1.先判断购物车中是否存在相同商品，如果存在则直接数量+1
        //1.1查询数据库（条件查询）
        //由于userId也作为查询条件，所以直接用ShoppingCart实体类作为参数，将userId封装进去
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = BeanUtil.copyProperties(shoppingCartDTO, ShoppingCart.class);
        shoppingCart.setUserId(userId);
        List<ShoppingCart> cartList=shoppingCartMapper.list(shoppingCart);
        //1.2判断查询结果
        if(cartList!=null&&!cartList.isEmpty()){
            //存在相同商品，取出数据
            ShoppingCart sameCart = cartList.get(0);
            sameCart.setNumber(sameCart.getNumber()+1);
            //更新数量
            shoppingCartMapper.updateNumberById(sameCart);
            //返回
            return;
        }
        //2.如果不存在则添加新数据
        //3.添加菜品
        if(shoppingCartDTO.getDishId()!=null){
            //3.1根据dishId查询对应dish信息
            Dish dish = dishMapper.getById(shoppingCartDTO.getDishId());
            //3.2封装进shoppingCart实体类(填充数据)
            shoppingCart.setName(dish.getName());
            shoppingCart.setImage(dish.getImage());
            shoppingCart.setAmount(dish.getPrice());
        }else{
            //4.添加套餐
            //4.1查询套餐信息
            Setmeal setmeal = setmealMapper.getById(shoppingCartDTO.getSetmealId());
            //4.2封装进实体类
            shoppingCart.setName(setmeal.getName());
            shoppingCart.setAmount(setmeal.getPrice());
            shoppingCart.setImage(setmeal.getImage());
        }
        shoppingCart.setCreateTime(LocalDateTime.now());
        shoppingCart.setNumber(1);
        //5.添加购物车数据到数据库
        shoppingCartMapper.insert(shoppingCart);
    }

    /**
     * 查看购物车
     * @return
     */
    @Override
    public List<ShoppingCart> showShoppingCart() {
        //1.根据userId查询购物车数据(条件查询)
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        //2.返回
        return list;
    }

    /**
     * 清空购物车
     */
    @Override
    public void deleteByUserId() {
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteByUserId(userId);
    }
}
