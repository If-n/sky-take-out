package com.sky.dto;

import lombok.Data;

/**
 * userlogin时请求微信服务响应体
 */
@Data
public class WXUserLoginResponseDTO {

   /*
    参数名	类型	说明
    session_key	string	会话密钥
    unionid	string	用户在开放平台的唯一标识符，若当前小程序已绑定到微信开放平台帐号下会返回，详见 UnionID 机制说明。
    openid	string	用户唯一标识
    errcode	number	错误码，请求失败时返回
    errmsg	string	错误信息，请求失败时返回
   */

    private String session_key;
    private String unionid;
    private String openid;
    private String errcode;
    private String errmsg;
}
