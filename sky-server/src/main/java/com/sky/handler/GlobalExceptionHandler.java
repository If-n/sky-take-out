package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }


    /**
     * 捕获新增员工时用户名重复异常
     */
    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex){
        //获取异常信息
        String exMsg = ex.getMessage();
        //1.如果是唯一约束异常，则说明新增用户时用户名已存在，系统抛出业务异常，需要手动捕获并提示前端用户，保证系统稳定性
        //Duplicate entry 'qaq' for key 'employee.idx_username'
        if(exMsg.contains("Duplicate entry")){
            //1.1处理异常信息
            String[] exArray = exMsg.split(" ");
            //1.2.翻译并返回前端
            String exInfo= MessageConstant.ALREADY_EXIST+exArray[2];
            return Result.success(exInfo);
        }else{
            //2.其他未知异常
            return Result.success(MessageConstant.UNKNOWN_ERROR);
        }
    }
}
