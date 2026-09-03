package com.sky.mapper;

import com.sky.dto.PeriodUsersDTO;
import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;


@Mapper
public interface UserMapper {

    /**
     * 根据openid查询用户
     * @param openid
     * @return
     */
    @Select("select * from user where openid=#{openid}")
    User getByOpenId(String openid);

    /**
     * 插入新user数据
     * @param user
     */
    void insert(User user);

    /**
     * 根据id查找用户
     * @param userId
     * @return
     */
    @Select("select * from user where id=#{userId}")
    User getById(Long userId);

    /**
     * 根据时间段条件查询对应用户数
     * @param paramMap
     * @return
     */
    Integer sumByMap(PeriodUsersDTO paramMap);
}
