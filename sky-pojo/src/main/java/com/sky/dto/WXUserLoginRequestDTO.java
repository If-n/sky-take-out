package com.sky.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * userlogin时请求微信服务请求体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WXUserLoginRequestDTO {

    /*
     参数名	类型	必填	说明
     appid	string	是	小程序 appId
     secret	string	是	小程序 appSecret
     js_code	string	是	登录时获取的 code，可通过wx.login获取
     grant_type	string	是	授权类型，此处只需填写 authorization_code
    */

    private String appid;
    private String secret;
    private String js_code;
    private String grant_type;
}
