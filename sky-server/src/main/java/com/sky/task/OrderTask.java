package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理超时未支付的订单,每分钟执行一次
     */
    //@Scheduled(cron = "1/5 * * * * *")
    @Scheduled(cron = "0 * * * * *")
    private void processTimeoutOrder(){
        //1.获取当前时间
        LocalDateTime now = LocalDateTime.now();
        //log.info("处理超时未支付的订单：{}",now);
        //2.计算超时时间
        LocalDateTime outTime = now.plusMinutes(-15);
        //3.查询<超时时间&&未支付状态的order
        List<Orders> orders= orderMapper.getByStatusAndTimeLT(Orders.PENDING_PAYMENT,outTime);
        //非空判断
        if(orders!=null&&!orders.isEmpty()){
            //4.取消订单，更新订单信息
            for (Orders order : orders) {
                order.setStatus(Orders.CANCELLED);
                order.setCancelTime(now);
                order.setCancelReason("订单超时未支付，自动取消");
                orderMapper.update(order);
            }
        }
    }


    /**
     * 每天清理派送中订单
     */
    //@Scheduled(cron = "1/5 * * * * *")
    @Scheduled(cron = "0 0 1 * * *")
    private void cleanDeliveryOrder(){
        //1.获取当前时间
        LocalDateTime now = LocalDateTime.now();
        //log.info("清理派送中订单：{}",now);
        //2.计算需要清理的订单时间
        LocalDateTime outTime = now.plusHours(-1);
        //3.条件查询订单
        List<Orders> orders = orderMapper.getByStatusAndTimeLT(Orders.DELIVERY_IN_PROGRESS, outTime);
        //非空判断
        if(orders!=null&&!orders.isEmpty()){
            //4.更新订单信息
            for (Orders order : orders) {
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            }
        }
    }
}
