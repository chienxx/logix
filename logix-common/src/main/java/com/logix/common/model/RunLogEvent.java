package com.logix.common.model;

import com.logix.common.enums.LogLevel;
import com.logix.common.enums.LogType;
import lombok.Getter;
import lombok.Setter;

/**
 * 运行日志事件
 *
 * @author Kanade
 * @since 2025/09/22
 */
@Getter
@Setter
public class RunLogEvent extends BaseLogEvent {

    /**
     * 当eventTime相同时服务端无法正确排序，因此需要增加一个字段保证相同毫秒的日志可正确排序
     */
    private Long seq;

    /**
     * 日志级别
     */
    private LogLevel logLevel;

    /**
     * 完整日志内容
     * 包含：原始消息 + 异常堆栈 + 自定义属性
     * 格式化后的最终内容，便于存储和检索
     */
    private String content;

    /**
     * 类名
     */
    private String className;

    /**
     * 线程名
     */
    private String threadName;


    @Override
    public LogType getLogType() {
        return LogType.RUN;
    }
}