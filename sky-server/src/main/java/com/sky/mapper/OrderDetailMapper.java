package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {

    /**
     * 批量插入订单明细数据
     * @param orderDetails
     */
    void insertBatch(List<OrderDetail> orderDetails);

    /**
     * 根据id查询details
     * @param orderId
     * @return
     */
    @Select("select * from order_detail where order_id=#{orderId} ")
    List<OrderDetail> getByOrderId(Long orderId);
}
