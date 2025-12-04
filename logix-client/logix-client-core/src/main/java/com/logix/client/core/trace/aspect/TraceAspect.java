package com.logix.client.core.trace.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;


/**
 * Trace注解切面处理器
 *
 * @author Kanade
 * @since 2025/10/16
 */
@Aspect
@Component
@ConditionalOnMissingBean(value = AbstractAspect.class, ignored = TraceAspect.class)
public class TraceAspect extends AbstractAspect {

    @Around("@annotation(com.logix.client.core.trace.annotation.Trace))")
    public Object around(JoinPoint joinPoint) throws Throwable {
        return aroundExecute(joinPoint);
    }
}