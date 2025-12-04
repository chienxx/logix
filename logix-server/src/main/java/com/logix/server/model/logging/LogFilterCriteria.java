package com.logix.server.model.logging;

/**
 * 日志查询公共过滤条件
 *
 * @author Kanade
 * @since 2025/11/23
 */
public interface LogFilterCriteria {

    String getAppName();

    String getEnv();

    String getLevel();

    String getTraceId();

    String getKeyword();

    String getServerIp();

    Long getStartTime();

    Long getEndTime();

    /**
     * 游标：上一条日志的时间戳（毫秒）
     * 用于实时拉取场景的增量查询
     */
    default Long getCursorDt() {
        return null;
    }

    /**
     * 游标：同一毫秒内的序号
     * 配合 cursorDt 精确定位日志位置
     */
    default Long getCursorSeq() {
        return null;
    }
}
