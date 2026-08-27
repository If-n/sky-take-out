package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面类，公共字段自动填充切面
 */
@Component//交给spring创建bean并管理
@Slf4j
@Aspect//表示这是一个切面类，有拦截器的作用，切面=切入点+通知
public class AutoFillAspect {
    /**
     * 切入点
     */
    //切点表达式(execution表示指定某方法为切点，@annotation表示指定使用了某个注解的才可以作为切点)
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillCutPoint() {
    }

    /**
     * 前置通知，对切入点方法的增强(对公共字段的自动填充增强)
     *
     * @param joinPoint
     */
    @Before("autoFillCutPoint()")//指定切入点
    public void autoFill(JoinPoint joinPoint/*连接点，可通过连接点获得被增强方法的方法名/参数等信息（反射）并进行操作*/) {
        log.info("自动填充..");
        //1.通过连接点获取当前操作数据库的类型（注解的属性）
        //1.1获取签名（对方法的签名，所以类型强转为下置接口MethodSignature）
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        //1.2获取指定的autofill注解
        AutoFill annotation = signature.getMethod().getAnnotation(AutoFill.class);
        //1.3获取注解属性
        OperationType type = annotation.value();
        //2.获取被增强方法的操作参数
        //2.1获取参数列表
        Object[] args = joinPoint.getArgs();
        //非空判断
        if (args == null || args.length == 0) {
            return;
        }
        //2.2获取参数,不能强转，因为employee/category..等类都可以使用这个切面
        Object arg = args[0];
        //3.准备填充的数据
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();
        //4.根据数据库操作类型调用方法进行填充
        if (type == OperationType.INSERT) {
            //插入新数据，需要更新4个字段

            try {
                //4.1获取更新方法
                Method setCreateTime = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setUpdateTime = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setCreateUser = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateUser = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                //4.2进行数据填充
                setCreateTime.invoke(arg, now);
                setUpdateTime.invoke(arg, now);
                setCreateUser.invoke(arg, currentId);
                setUpdateUser.invoke(arg, currentId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (type == OperationType.UPDATE) {
            //更新数据，需要更新2个字段
            try {
                //4.1获取更新方法
                Method setUpdateTime = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                //4.2进行数据填充
                setUpdateTime.invoke(arg, now);
                setUpdateUser.invoke(arg, currentId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
