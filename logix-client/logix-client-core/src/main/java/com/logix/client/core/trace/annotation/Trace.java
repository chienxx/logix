package com.logix.client.core.trace.annotation;

import java.lang.annotation.*;

/**
 * 链路追踪注解
 *
 * @author Kanade
 * @since 2025/10/16
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Trace {
}