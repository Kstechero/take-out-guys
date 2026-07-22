package com.sky.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import com.sky.enumeration.OperationType;


/**
 * 自定义注解，用于标识某个方法需要进行功能字段自动填充处理
 */
@Target(ElementType.METHOD) // 该注解只能标注在方法上
@Retention(RetentionPolicy.RUNTIME) // 注解在运行时仍然可用
public @interface AutoFill {
    OperationType value(); // 注解属性，用于指定操作类型（INSERT 或 UPDATE）    
}
