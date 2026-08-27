package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    /**
     * 将口味批量插入口味表（菜品id：逻辑外键）
     * @param flavors
     */

    void insertBatch(List<DishFlavor> flavors);
}
