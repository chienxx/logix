package com.logix.server.query;

import com.logix.server.model.PageResult;
import com.logix.server.model.logging.LogQueryRequest;
import com.logix.server.model.logging.LogRealtimeRequest;
import com.logix.server.model.logging.LogRecord;
import com.logix.server.storage.reader.RunLogReader;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 日志查询服务
 *
 * @author Kanade
 * @since 2025/11/24
 */
@Service
public class LogQueryService {

    private static final long DEFAULT_TIME_RANGE_MS = 3600_000L;
    private static final int DEFAULT_POLL_LIMIT = 200;

    private final RunLogReader reader;

    public LogQueryService(RunLogReader reader) {
        this.reader = reader;
    }

    public PageResult<LogRecord> queryLogs(LogQueryRequest request) {
        ensureTimeRange(request);

        int pageNum = request.getPageNum();
        int pageSize = request.getPageSize();
        int offset = (pageNum - 1) * pageSize;

        List<LogRecord> records = reader.queryLogs(request, offset, pageSize);
        int total = reader.queryCount(request);

        return new PageResult<>((long) total, pageNum, pageSize, records);
    }

    /**
     * 实时拉取日志
     * - 使用游标机制实现增量拉取
     * - 时间正序排列，新日志在后面（模拟 tail -f）
     */
    public List<LogRecord> pollLatestLogs(LogRealtimeRequest request) {
        ensureTimeRange(request);

        int limit = request.getLimit() != null ? request.getLimit() : DEFAULT_POLL_LIMIT;
        return reader.pollLogs(request, limit);
    }

    public List<String> listApplications() {
        return reader.queryAppNames();
    }

    public List<String> listEnvironments() {
        return reader.queryEnvs();
    }

    private void ensureTimeRange(LogQueryRequest request) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            long now = System.currentTimeMillis();
            request.setEndTime(now);
            request.setStartTime(now - DEFAULT_TIME_RANGE_MS);
        }
    }

    private void ensureTimeRange(LogRealtimeRequest request) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            long now = System.currentTimeMillis();
            request.setEndTime(now);
            request.setStartTime(now - DEFAULT_TIME_RANGE_MS);
        }
    }
}
