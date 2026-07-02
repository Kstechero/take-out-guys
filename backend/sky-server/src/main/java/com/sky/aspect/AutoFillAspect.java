package com.sky.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.JoinPoint;
import org.springframework.stereotype.Component;
import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.enumeration.OperationType;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import com.sky.context.BaseContext;

import org.aspectj.lang.reflect.MethodSignature;
import lombok.extern.slf4j.Slf4j;

/**
 * 自定义切面，实现公共字段自动填充处理逻辑
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    /**
     * 切入点
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillpointcut() {
        // 切入点表达式，匹配所有标注了 @AutoFill 注解的方法

    }

    @Before("autoFillpointcut()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("执行了公共字段自动填充处理逻辑...");
        
        //获取当前被拦截的方法上的数据库操作类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature(); //方法签名对象
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class); //获取方法上的注解对象
        OperationType operationType = autoFill.value(); //获取注解属性值（操作类型）


        //获取到当前被拦截的方法的参数--实体对象
        Object[] args = joinPoint.getArgs(); //方法参数列表
        if (args == null || args.length == 0) {
            return; //没有参数，直接返回
        }

        Object entity = args[0]; //获取第一个参数（实体对象）

        //准备赋值的数据

        LocalDateTime now = LocalDateTime.now(); //当前时间
        Long currentId = BaseContext.getCurrentId(); //当前登录用户id


        //根据当前不同的操作类型，为对应的属性通过反射来赋值
        if(operationType == OperationType.INSERT) {
            //插入操作，填充创建时间、更新时间、创建人、修改人
            try {
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
              
                setCreateTime.invoke(entity, now);

                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
               
                setUpdateTime.invoke(entity, now);

                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
               
                setCreateUser.invoke(entity, currentId);

                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
              
                setUpdateUser.invoke(entity, currentId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (operationType == OperationType.UPDATE) {
            //更新操作，填充更新时间、修改人
            try {
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
             
                setUpdateTime.invoke(entity, now);

                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class     );

                setUpdateUser.invoke(entity, currentId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
