package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    /**
     * 将口味批量插入口味表（菜品id：逻辑外键）
     * @param flavors
     */

    void insertBatch(List<DishFlavor> flavors);

    /**
     * 根据dishId删除口味数据
     * @param dishId
     */
    @Delete("delete from dish_flavor where dish_id=#{dishId}")
    void deleteByDishId(Long dishId);

    /**
     * 根据dishIds批量删除口味
     * @param ids
     */
    void deleteByDishIds(List<Long> dishIds);
}
