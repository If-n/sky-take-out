package com.sky.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //1.用户提交数据校验，防止恶意下单
        //1.1地址非空判断
        //1.1.1根据dto里的地址id查询地址表数据
        Long addressBookId = ordersSubmitDTO.getAddressBookId();
        AddressBook address = addressBookMapper.getById(addressBookId);
        //1.1.2非空判断
        if (address == null) {
            //1.1.3空则抛异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //1.2购物车非空判断
        //1.2.1获取当前userId，根据userId查询购物车数据
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder().userId(userId).build();
        List<ShoppingCart> cartList = shoppingCartMapper.list(shoppingCart);
        //1.2.2非空判断
        if (cartList == null || cartList.isEmpty()) {
            //1.2.3空则抛异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //2.校验通过，执行业务
        //3.将订单数据添加到订单表
        //3.1封装orders实体类
        Orders orders = BeanUtil.copyProperties(ordersSubmitDTO, Orders.class);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));//设置订单号
        orders.setStatus(Orders.PENDING_PAYMENT);//设置订单状态
        orders.setUserId(userId);//设置下单用户id
        orders.setOrderTime(LocalDateTime.now());//设置下单时间
        orders.setPayStatus(Orders.UN_PAID);//设置支付状态
        orders.setPhone(address.getPhone());//设置收货人手机号
        orders.setAddress(address.getProvinceName() + address.getCityName()
                + address.getDistrictName() + address.getDetail());//设置收货人地址
        orders.setConsignee(address.getConsignee());//设置收货人姓名
        //3.2插入订单表(同时将生成的主键返回给orders对象)
        orderMapper.insert(orders);
        //4.将订单明细（商品数据）添加到订单明细表
        //4.1通过购物车数据list查询订单明细（商品）list
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (ShoppingCart cart : cartList) {
            OrderDetail orderDetail = BeanUtil.copyProperties(cart, OrderDetail.class);
            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);
        }
        //4.2批量插入orderDetail数据
        orderDetailMapper.insertBatch(orderDetails);
        //5.清空购物车数据
        shoppingCartMapper.deleteByUserId(userId);
        //6.封装并返回VO对象
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder().id(orders.getId()).orderNumber(orders.getNumber()).orderAmount(orders.getAmount()).orderTime(orders.getOrderTime()).build();
        return orderSubmitVO;
    }


    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 分页查询历史订单
     *
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    @Override
    public PageResult page(Integer pageNum, Integer pageSize, Integer status) {
        //1.将查询条件封装到dto中，方便条件查询
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setStatus(status);
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        //2.分页查询当前用户订单
        //2.1使用插件进行分页
        PageHelper.startPage(pageNum, pageSize);
        //2.2使用封装好的dto进行条件查询数据
        Page<Orders> orders = orderMapper.pageQuery(ordersPageQueryDTO);
        List<OrderVO> orderVOS = new ArrayList<>();

        //非空判断
        if (orders != null && orders.getTotal() > 0) {
            //3.逐个查询对应detail数据并封装入OrderVO数据list
            for (Orders order : orders) {
                //3.1根据orderId查询details
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(order.getId());
                //3.2封装orderVO对象
                OrderVO orderVO = BeanUtil.copyProperties(order, OrderVO.class);
                orderVO.setOrderDetailList(orderDetails);
                //3.3加入list
                orderVOS.add(orderVO);
            }
        }
        //4.返回
        return new PageResult(orders.getTotal(), orderVOS);
    }

    /**
     * 查看订单详情
     *
     * @param id
     * @return
     */
    @Override
    public OrderVO details(Long id) {
        //1.根据订单id查询对应订单数据
        Orders order = orderMapper.getById(id);

        //2.封装为orderVO
        OrderVO orderVO = BeanUtil.copyProperties(order, OrderVO.class);

        //3.查询detail并封装进去
        List<OrderDetail> orderDatils = orderDetailMapper.getByOrderId(id);
        orderVO.setOrderDetailList(orderDatils);
        //4.返回数据
        return orderVO;
    }

    /**
     * 用户取消订单
     *
     * @param id
     */
    public void userCancelById(Long id) throws Exception {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        // 订单处于待接单状态下取消，需要进行退款
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            //调用微信支付退款接口
            weChatPayUtil.refund(
                    ordersDB.getNumber(), //商户订单号
                    ordersDB.getNumber(), //商户退款单号
                    new BigDecimal(0.01),//退款金额，单位 元
                    new BigDecimal(0.01));//原订单金额

            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }

        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     *
     * @param id
     */
    public void repetition(Long id) {
        // 查询当前用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 将购物车对象批量添加到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 管理端订单搜索
     *
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        //1.分页条件查询对应订单
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> ordersPage = orderMapper.pageQuery(ordersPageQueryDTO);

        //2.解析查询结果
        List<Orders> ordersList = ordersPage.getResult();
        //3.遍历每个订单，封装为OrderVO对象->List<OrderVO>
        List<OrderVO> orderVOS = new ArrayList<>();

        //非空判断
        if (ordersList == null || ordersList.isEmpty()) {
            return new PageResult(ordersPage.getTotal(), orderVOS);
        }


        for (Orders order : ordersList) {
            //3.1根据orderId查询details并拼接为字符串
            String orderDishes = getOrderDishesStr(order);
            //3.2格式转换
            OrderVO orderVO = BeanUtil.copyProperties(order, OrderVO.class);
            //3.3封装
            orderVO.setOrderDishes(orderDishes);
            //3.4加入集合
            orderVOS.add(orderVO);
        }

        //4.封装为pageresult返回
        return new PageResult(ordersPage.getTotal(), orderVOS);

    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        //1.查询待接单数量
        Integer toBeConfirmed= orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        //2.查询待派送数量
        Integer confirmed= orderMapper.countStatus(Orders.CONFIRMED);
        //3.查询派送中数量
        Integer deliveryInProgress= orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);
        //4.封装并返回
        OrderStatisticsVO ovo = new OrderStatisticsVO();
        ovo.setConfirmed(confirmed);
        ovo.setToBeConfirmed(toBeConfirmed);
        ovo.setDeliveryInProgress(deliveryInProgress);
        return ovo;
    }

    /**
     * 管理端接单
     * @param ordersConfirmDTO
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        //1.根据订单id更新订单
        Long userId = ordersConfirmDTO.getId();
        Orders orders = new Orders();
        orders.setUserId(userId);
        orders.setStatus(Orders.CONFIRMED);
        orderMapper.update(orders);
    }

    /**
     * 拒单
     *
     * @param ordersRejectionDTO
     */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());

        // 订单只有存在且状态为2（待接单）才可以拒单
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        /*//支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == Orders.PAID) {
            //用户已支付，需要退款
            String refund = weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
            log.info("申请退款：{}", refund);
        }*/

        // 拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 根据订单id获取菜品信息字符串
     *
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }

    /**
     * 取消订单
     *
     * @param ordersCancelDTO
     */
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());

        /*//支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == 1) {
            //用户已支付，需要退款
            String refund = weChatPayUtil.refund(
                    ordersDB.getNumber(),
                    ordersDB.getNumber(),
                    new BigDecimal(0.01),
                    new BigDecimal(0.01));
            log.info("申请退款：{}", refund);
        }*/

        // 管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }


}
