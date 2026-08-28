package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 根据dishId集合查询套餐id集合
     * @param dishIds
     * @return
     */
    List<Long> getSetmealIdsbyDishIds(List<Long> dishIds);
}
