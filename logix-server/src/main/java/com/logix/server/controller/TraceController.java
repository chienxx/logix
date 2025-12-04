package com.logix.server.controller;

import com.logix.server.model.logging.TraceNode;
import com.logix.server.model.logging.TraceQueryRequest;
import com.logix.server.query.TraceQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 链路追踪接口
 *
 * @author Kanade
 * @since 2025/11/23
 */
@Slf4j
@RestController
@RequestMapping("/api/trace")
public class TraceController {

    @Resource
    private TraceQueryService traceQueryService;

    /**
     * 查询链路追踪数据
     */
    @PostMapping("/query")
    public List<TraceNode> queryTrace(@RequestBody TraceQueryRequest request) {
        return traceQueryService.queryTrace(request);
    }
}
