package com.logix.server.controller;

import com.logix.server.model.logging.LogQueryRequest;
import com.logix.server.model.logging.LogRealtimeRequest;
import com.logix.server.model.logging.LogRecord;
import com.logix.server.model.PageResult;
import com.logix.server.query.LogQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 日志查询接口
 *
 * @author Kanade
 * @since 2025/11/23
 */
@Slf4j
@RestController
@RequestMapping("/api/logs")
public class LogController {

    @Resource
    private LogQueryService logQueryService;

    /**
     * 分页查询日志
     */
    @PostMapping("/query")
    public PageResult<LogRecord> queryLogs(@RequestBody LogQueryRequest request) {
        return logQueryService.queryLogs(request);
    }

    /**
     * 获取应用列表
     */
    @GetMapping("/apps")
    public List<String> getApps() {
        return logQueryService.listApplications();
    }

    /**
     * 获取环境列表
     */
    @GetMapping("/envs")
    public List<String> getEnvs() {
        return logQueryService.listEnvironments();
    }

    /**
     * 实时拉取日志
     */
    @PostMapping("/poll")
    public List<LogRecord> pollLatest(@RequestBody LogRealtimeRequest request) {
        log.info("实时拉取日志，游标: {}-{}", request.getCursorDt(), request.getCursorSeq());
        return logQueryService.pollLatestLogs(request);
    }
}
