package com.lytoyo.framework.aspectj;

import java.lang.annotation.*;

/**
 * Package:com.lytoyo.framework.aspectj
 *
 * @ClassName:SysLog
 * @Create:2025/12/4 15:45
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SysLog {
    String value() default "";
}
