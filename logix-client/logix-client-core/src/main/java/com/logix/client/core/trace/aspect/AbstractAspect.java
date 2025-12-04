package com.logix.client.core.trace.aspect;

import com.logix.client.core.trace.TraceContext;
import com.logix.client.core.trace.TraceMessage;
import com.logix.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.logix.common.constants.LogixConstants;

/**
 * 链路追踪打点拦截
 *
 * @author Kanade
 * @since 2025/10/16
 */
@Slf4j
public abstract class AbstractAspect {

    public Object aroundExecute(JoinPoint joinPoint) throws Throwable {
        TraceMessage traceMessage = TraceContext.currentTraceMessage.get();
        String traceId = TraceContext.currentTraceID.get();
        if (traceMessage == null) {
            traceMessage = new TraceMessage();
            traceMessage.getDepth().set(0);
        }
        traceMessage.setSignature(joinPoint.getSignature().toString());
        traceMessage.setPosition(LogixConstants.Trace.START);
        traceMessage.getDepth().incrementAndGet();
        TraceContext.currentTraceMessage.set(traceMessage);
        if (traceId != null) {
            log.info(LogixConstants.Trace.PREFIX + "{}", JsonUtils.toJson(traceMessage));
        }
        Object proceed = ((ProceedingJoinPoint) joinPoint).proceed();
        traceMessage.setSignature(joinPoint.getSignature().toString());
        traceMessage.setPosition(LogixConstants.Trace.END);
        traceMessage.getDepth().incrementAndGet();
        if (traceId != null) {
            log.info(LogixConstants.Trace.PREFIX + "{}", JsonUtils.toJson(traceMessage));
        }
        return proceed;
    }
}
