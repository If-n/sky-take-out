package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.PeriodOrdersDTO;
import com.sky.dto.PeriodTurnoverDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    /**
     * 插入新订单数据
     * 需要返回主键id，所以使用动态sql
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 条件分页查询
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询order
     * @param id
     * @return
     */
    @Select("select * from orders where id=#{id}")
    Orders getById(Long id);

    /**
     * 根据订单状态status查询对应订单数量
     * @param status
     * @return
     */
    @Select("select count(id) from orders where status=#{status}")
    Integer countStatus(Integer status);

    /**
     * 根据状态和时间条件查询
     * @param status
     * @param outTime
     * @return
     */
    @Select("select * from orders where status=#{status} and order_time<#{outTime}")
    List<Orders> getByStatusAndTimeLT(Integer status, LocalDateTime outTime);

    /**
     * 查询指定日期的指定状态的订单总金额
     * @param periodTurnoverDTO
     * @return
     */
    Double sumByMap(PeriodTurnoverDTO periodTurnoverDTO);

    /**
     * 根据dto条件查询订单数量
     * @param queryDTO
     * @return
     */
    Integer countByMap(PeriodOrdersDTO queryDTO);

    /**
     * 根据时间段查询销量前十数据
     * @param beginTime
     * @param endTime
     * @return
     */
    List<GoodsSalesDTO> getSalesTop10(LocalDateTime beginTime, LocalDateTime endTime);
}
