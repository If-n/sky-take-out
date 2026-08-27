package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解类，公共字段的自动填充注解
 */
@Target(ElementType.METHOD)//指明注解使用的类型（方法注解/属性注解）
@Retention(RetentionPolicy.RUNTIME)//指明注解存活的周期（仅编译时存活/运行时一直存活）
public @interface AutoFill {
    //注解属性，数据库的操作类型(枚举类)
    OperationType value();
}
