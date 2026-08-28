package com.sky.service.impl;

import com.sky.constant.RedisConstant;
import com.sky.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisAccessor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ShopServiceImpl implements ShopService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 设置店铺营业状态
     * @param status
     */
    @Override
    public void setShopStatus(Integer status) {
        //将店铺营业状态更新到redis中
        stringRedisTemplate.opsForValue().set(RedisConstant.SHOP_STATUS_KEY,status.toString());
    }

    /**
     * 查询店铺营业状态
     * @return
     */
    @Override
    public Integer getShopStatus() {
        String status = stringRedisTemplate.opsForValue().get(RedisConstant.SHOP_STATUS_KEY);
        return Integer.parseInt(status);
    }
}
