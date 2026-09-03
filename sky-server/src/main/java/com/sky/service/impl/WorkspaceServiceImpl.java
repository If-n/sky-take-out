package com.sky.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.sky.constant.StatusConstant;
import com.sky.dto.*;
import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 根据时间段统计营业数据
     * @param begin
     * @param end
     * @return
     */
    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {
        /**
         * 营业额：当日已完成订单的总金额
         * 有效订单：当日已完成订单的数量
         * 订单完成率：有效订单数 / 总订单数
         * 平均客单价：营业额 / 有效订单数
         * 新增用户：当日新增用户的数量
         */


        PeriodOrdersDTO periodOrdersDTO = PeriodOrdersDTO.builder().begin(begin).end(end).build();

        //查询总订单数
        Integer totalOrderCount = orderMapper.countByMap(periodOrdersDTO);
        periodOrdersDTO.setStatus(Orders.COMPLETED);
        //营业额
        PeriodTurnoverDTO periodTurnoverDTO = BeanUtil.copyProperties(periodOrdersDTO, PeriodTurnoverDTO.class);
        Double turnover = orderMapper.sumByMap(periodTurnoverDTO);
        turnover = turnover == null? 0.0 : turnover;

        //有效订单数
        Integer validOrderCount = orderMapper.countByMap(periodOrdersDTO);

        Double unitPrice = 0.0;

        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0 && validOrderCount != 0){
            //订单完成率
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
            //平均客单价
            unitPrice = turnover / validOrderCount;
        }

        PeriodUsersDTO periodUsersDTO = BeanUtil.copyProperties(periodOrdersDTO, PeriodUsersDTO.class);
        //新增用户数
        Integer newUsers = userMapper.sumByMap(periodUsersDTO);

        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }


    /**
     * 查询订单管理数据
     *
     * @return
     */
    public OrderOverViewVO getOrderOverView() {

        PeriodOrdersDTO periodOrdersDTO = PeriodOrdersDTO
                .builder()
                .begin(LocalDateTime.now().with(LocalTime.MIN))
                .status( Orders.TO_BE_CONFIRMED)
                .build();

        //待接单
        Integer waitingOrders = orderMapper.countByMap(periodOrdersDTO);

        //待派送
        periodOrdersDTO.setStatus(Orders.CONFIRMED);
        Integer deliveredOrders = orderMapper.countByMap(periodOrdersDTO);

        //已完成
        periodOrdersDTO.setStatus(Orders.COMPLETED);
        Integer completedOrders = orderMapper.countByMap(periodOrdersDTO);

        //已取消
        periodOrdersDTO.setStatus(Orders.CANCELLED);
        Integer cancelledOrders = orderMapper.countByMap(periodOrdersDTO);

        //全部订单
        periodOrdersDTO.setStatus(null);
        Integer allOrders = orderMapper.countByMap(periodOrdersDTO);

        return OrderOverViewVO.builder()
                .waitingOrders(waitingOrders)
                .deliveredOrders(deliveredOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .allOrders(allOrders)
                .build();
    }

    /**
     * 查询菜品总览
     *
     * @return
     */
    public DishOverViewVO getDishOverView() {

        DishQueryByStatusAndCategoryIdDTO map = DishQueryByStatusAndCategoryIdDTO
                .builder().status(StatusConstant.ENABLE).build();

        Integer sold = dishMapper.countByMap(map);

        map.setStatus(StatusConstant.DISABLE);
        Integer discontinued = dishMapper.countByMap(map);

        return DishOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    /**
     * 查询套餐总览
     *
     * @return
     */
    public SetmealOverViewVO getSetmealOverView() {

        SetmealQueryByStatusAndCategoryIdDTO map = SetmealQueryByStatusAndCategoryIdDTO
                .builder().status(StatusConstant.ENABLE).build();


        Integer sold = setmealMapper.countByMap(map);

        map.setStatus(StatusConstant.DISABLE);
        Integer discontinued = setmealMapper.countByMap(map);

        return SetmealOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }
}
