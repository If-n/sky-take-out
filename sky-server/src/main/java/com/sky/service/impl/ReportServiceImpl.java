package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.sky.dto.*;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 统计指定时间内的营业额
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //1.统计日期
        //1.1统计日期集合
        List<LocalDate> timeList=new ArrayList<>();
        for(LocalDate time=begin;!time.isEqual(end);time=time.plusDays(1)){
            timeList.add(time);
        }
        timeList.add(end);
        //1.2转为字符串
        String dateString = StringUtils.join(timeList, ",");
        //2.统计每天的营业额
        List<Double> turnoverList=new ArrayList<>();
        for (LocalDate time : timeList) {
            //2.1统计这一天最小时间到最大时间范围
            LocalDateTime beginTime = LocalDateTime.of(time, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(time, LocalTime.MAX);
            //2.2统计营业额
            //select sum(amount) from orders where order_time>beginTime and <endTime and status=complete
            //用一个dto封装并作为查询参数
            PeriodTurnoverDTO periodTurnoverDTO = PeriodTurnoverDTO
                    .builder()
                    .begin(beginTime)
                    .end(endTime)
                    .status(Orders.COMPLETED)
                    .build();
            Double dayTurnover= orderMapper.sumByMap(periodTurnoverDTO);
            //非空判断
            dayTurnover = dayTurnover == null ? 0.0 : dayTurnover;
            turnoverList.add(dayTurnover);
        }

        //2.3转为字符串
        String turnoverSting = StringUtils.join(turnoverList, ",");
        //3.封装并返回
        return TurnoverReportVO.builder().dateList(dateString).turnoverList(turnoverSting).build();
    }

    /**
     * 根据时间段统计每日用户总数和新增用户数
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistic(LocalDate begin, LocalDate end) {
        //1.日期字符串
        List<LocalDate> dateList=new ArrayList<>();
        for (LocalDate time=begin;!time.equals(end);time=time.plusDays(1)){
            dateList.add(time);
        }
        dateList.add(end);
        String dateStr = StringUtils.join(dateList, ",");

        //2.统计每日用户总数和新增用户数
        List<Integer> allUser=new ArrayList<>();
        List<Integer> addUser=new ArrayList<>();

        for (LocalDate time : dateList) {
            //2.1日期条件
            LocalDateTime beginTime = LocalDateTime.of(time, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(time, LocalTime.MAX);

            //3.每天用户总数
            //select count(id) from user where create_time<=end
            PeriodUsersDTO paramMap = PeriodUsersDTO.builder().end(endTime).build();
            Integer all= userMapper.sumByMap(paramMap);
            allUser.add(all);
            //4.每日新增用户数
            //select count(id) from user where create_time>=begin and ct<=end
            paramMap.setBegin(beginTime);
            Integer add= userMapper.sumByMap(paramMap);
            addUser.add(add);

        }
        //5.转为字符串
        String allUserStr = StringUtils.join(allUser, ",");
        String addUserStr = StringUtils.join(addUser, ",");

        return UserReportVO.builder().dateList(dateStr).totalUserList(allUserStr).newUserList(addUserStr).build();
    }

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrdersStatistic(LocalDate begin, LocalDate end) {
        //1.日期字符串
        List<LocalDate> dateList=new ArrayList<>();
        for(LocalDate time=begin;!time.equals(end);time=time.plusDays(1)){
            dateList.add(time);
        }
        dateList.add(end);
        //2.每日订单总数、有效订单数、总数
        //存放每天的订单总数
        List<Integer> orderCountList = new ArrayList<>();
        //存放每天的有效订单数
        List<Integer> validOrderCountList = new ArrayList<>();

        for (LocalDate day : dateList) {
            //日期参数
            LocalDateTime beginTime = LocalDateTime.of(day, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(day, LocalTime.MAX);

            //3.每日订单总数
            //select count(id) form orders where and order_time>=begintime and ot<=endtime
            PeriodOrdersDTO queryDTO = PeriodOrdersDTO.builder().begin(beginTime).end(endTime).build();
            Integer orderCount =orderMapper.countByMap(queryDTO);

            //4.每日有效订单数
            //select count(id) form orders where and order_time>=begintime and ot<=endtime and status=complete
            queryDTO.setStatus(Orders.COMPLETED);
            Integer validOrderCount = orderMapper.countByMap(queryDTO);

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }

        //5.计算订单有效率
        //计算时间区间内的订单总数量
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();

        //计算时间区间内的有效订单数量
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0){
            //计算订单完成率
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }

        //6.转为字符串封装返回
        return OrderReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCountList(StringUtils.join(orderCountList,","))
                .validOrderCountList(StringUtils.join(validOrderCountList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 查询销量top10
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        //1.查询销量top10的名字和销量数据
        //1.1参数类型转化
        LocalDateTime beginTime=LocalDateTime.of(begin,LocalTime.MIN);
        LocalDateTime endTime=LocalDateTime.of(end,LocalTime.MAX);
        //1.2查询
        List<GoodsSalesDTO> topInfo=orderMapper.getSalesTop10(beginTime,endTime);
        //2.解析查询结果
        List<String> nameList = topInfo.stream().map(item -> item.getName()).collect(Collectors.toList());
        List<Integer> numberList = topInfo.stream().map(item -> item.getNumber()).collect(Collectors.toList());
        //3.将list转为字符串封装VO并返回
        return SalesTop10ReportVO
                .builder()
                .nameList(StringUtils.join(nameList,","))
                .numberList(StringUtils.join(numberList,","))
                .build();
    }

}
