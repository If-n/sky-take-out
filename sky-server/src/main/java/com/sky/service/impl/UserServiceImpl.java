package com.sky.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.sky.constant.JwtClaimsConstant;
import com.sky.constant.SystemConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.dto.WXUserLoginResponseDTO;
import com.sky.entity.User;
import com.sky.mapper.UserMapper;
import com.sky.properties.JwtProperties;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private WeChatProperties weChatProperties;//微信小程序配置
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 用户微信登录服务实现
     * @param userLoginDTO
     * @return
     */
    @Override
    public UserLoginVO wxLogin(UserLoginDTO userLoginDTO) {
        //1.获取c端传入的code
        String code = userLoginDTO.getCode();
        //2.调用微信服务接口获取用户唯一openid

        String openid = getOpenId(code);

        //3.根据openid查询user数据库
        User user= userMapper.getByOpenId(openid);
        //4.判断user是否存在
        if(user==null){
            //4.1 user不存在，则进行注册流程，插入新数据后需要拿到mysql生成的主键id
            user = User.builder().openid(openid).createTime(LocalDateTime.now()).build();
            userMapper.insert(user);
        }
        //5.封装为userlogindvo并返回
        UserLoginVO userLoginVO = BeanUtil.copyProperties(user, UserLoginVO.class);

        //6.生成jwt装入VO返回前端
        Map<String, Object> claims=new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID,user.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);
        userLoginVO.setToken(token);
        return userLoginVO;
    }

    //调用微信服务接口获取用户唯一openid
    private String getOpenId(String code) {
        //2.1封装请求体
        //-请求url
        String URL= SystemConstant.WX_LOGIN_URL;
        //-请求参数
        Map<String, String> paramMap=new HashMap<>();
        paramMap.put("appid",weChatProperties.getAppid());
        paramMap.put("secret",weChatProperties.getSecret());
        paramMap.put("js_code", code);
        paramMap.put("grant_type","authorization_code");
        //2.2发送请求并获得响应
        String response = HttpClientUtil.doGet(URL, paramMap);
        log.info("响应体：{}",response);
        //2.3解析响应
        WXUserLoginResponseDTO wxUserLoginResponseDTO = JSONUtil.toBean(response, WXUserLoginResponseDTO.class);
        //2.4获取openid
        return wxUserLoginResponseDTO.getOpenid();
    }
}
