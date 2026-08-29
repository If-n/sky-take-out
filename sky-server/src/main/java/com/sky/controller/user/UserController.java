package com.sky.controller.user;

import com.sky.dto.UserLoginDTO;
import com.sky.result.Result;
import com.sky.service.UserService;
import com.sky.vo.UserLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/user")
@Api(tags = "c端用户模块接口")
@Slf4j
public class UserController {

    @Autowired
    private UserService  userService;

    /**
     * 用户微信登录接口
     * @param userLoginDTO
     * @return
     */
    @PostMapping("/login")
    @ApiOperation("用户登录")
    public Result<UserLoginVO> wxLogin(@RequestBody UserLoginDTO userLoginDTO){
        UserLoginVO userLoginVO=userService.wxLogin(userLoginDTO);
        return Result.success(userLoginVO);
    }
}
