package com.sky.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品&口味
     * @param dishDTO
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        //1.新增菜品
        //1.1将前端传入的数据封装为数据库的实体类dto->entity
        Dish dish = BeanUtil.copyProperties(dishDTO, Dish.class);
        dishMapper.insert(dish);

        //1.2插入数据后，会自动将生成的主键赋值给dish的id属性
        //获得dishId
        Long dishId = dish.getId();

        //2.新增口味
        List<DishFlavor> flavors = dishDTO.getFlavors();
        //2.1非空判断
        if(flavors!=null&& !flavors.isEmpty()){
            //2.2为每个口味指定对应菜品id
            flavors.forEach(flavor->flavor.setDishId(dishId));
            //2.3批量插入口味表
            dishFlavorMapper.insertBatch(flavors);
        }


    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        //1.获取dto数据
        int page = dishPageQueryDTO.getPage();
        int pageSize = dishPageQueryDTO.getPageSize();
        //2.分页查询dish数据
        //2.1使用插件进行分页查询sql拼接
        PageHelper.startPage(page,pageSize);
        //2.2分页查询
        Page<DishVO> dishPage= dishMapper.queryPage(dishPageQueryDTO);
        //3.封装并返回数据
        return new PageResult(dishPage.getTotal(),dishPage.getResult());
    }

    /**
     * 批量删除菜品
     * @param ids
     */
    @Override
    public void deleteBatch(List<Long> ids) {
        //1.判断菜品是否能被删除
        //1.1判断菜品状态，起售则不可删除
        for (Long id : ids) {
            //查询每个菜品状态，如果有起售状态则抛异常
            Dish dish=dishMapper.getById(id);
            if(dish.getStatus()== StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //1.2判断每个菜品是否有关联套餐，如果有则不可删除
        List<Long> mealIds =setmealDishMapper.getSetmealIdsbyDishIds(ids);
        if(mealIds!=null&&!mealIds.isEmpty()){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //可以删除
        /*for (Long id : ids) {
            //2.删除dish表中的数据
            dishMapper.deleteById(id);
            //3.删除口味表中的数据
            dishFlavorMapper.deleteByDishId(id);
        }*/

        //批量删除，减少sql语句
        dishMapper.deleteByIds(ids);
        dishFlavorMapper.deleteByDishIds(ids);


    }

}
