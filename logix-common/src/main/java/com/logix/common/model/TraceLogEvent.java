package com.logix.common.model;

import com.logix.common.enums.LogType;
import lombok.Getter;
import lombok.Setter;

/**
 * 链路追踪日志事件
 *
 * @author Kanade
 * @since 2025/09/22
 */
@Getter
@Setter
public class TraceLogEvent extends BaseLogEvent {

    /**
     * 方法调用位置：START/END
     */
    private String position;

    /**
     * 调用深度
     */
    private Integer depth;

    @Override
    public LogType getLogType() {
        return LogType.TRACE;
    }
}