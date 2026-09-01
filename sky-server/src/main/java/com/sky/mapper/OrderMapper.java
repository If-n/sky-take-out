package com.sky.mapper;

import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper {

    /**
     * 插入新订单数据
     * 需要返回主键id，所以使用动态sql
     * @param orders
     */
    void insert(Orders orders);
}
