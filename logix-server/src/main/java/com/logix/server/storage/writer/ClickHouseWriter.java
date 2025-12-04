package com.logix.server.storage.writer;

import com.logix.common.model.BaseLogEvent;

import java.util.List;

/**
 * ClickHouse写入器接口
 *
 * @author Kanade
 * @since 2025/11/23
 */
public interface ClickHouseWriter<T extends BaseLogEvent> {

    /**
     * 批量插入日志
     *
     * @param events 日志事件列表
     */
    void batchInsert(List<T> events);
}
