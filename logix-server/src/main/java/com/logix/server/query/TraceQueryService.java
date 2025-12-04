package com.logix.server.query;

import com.logix.server.model.logging.TraceNode;
import com.logix.server.model.logging.TraceQueryRequest;
import com.logix.server.storage.reader.TraceLogReader;
import com.logix.server.storage.reader.TraceLogReader.TraceRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 链路追踪服务
 *
 * @author Kanade
 * @since 2025/11/24
 */
@Slf4j
@Service
public class TraceQueryService {

    private final TraceLogReader reader;

    public TraceQueryService(TraceLogReader reader) {
        this.reader = reader;
    }

    public List<TraceNode> queryTrace(TraceQueryRequest request) {
        if (!StringUtils.hasText(request.getTraceId())) {
            throw new IllegalArgumentException("TraceId不能为空");
        }
        List<TraceRow> rows = reader.queryByTraceId(request);
        return buildTraceTree(rows);
    }

    private List<TraceNode> buildTraceTree(List<TraceRow> rows) {
        List<TraceNode> roots = new ArrayList<>();
        Deque<TraceNode> stack = new ArrayDeque<>();

        for (TraceRow row : rows) {
            boolean isStart = "<".equals(row.position) || "1".equals(row.position);

            if (isStart) {
                TraceNode node = createNode(row, stack);
                if (stack.isEmpty()) {
                    roots.add(node);
                } else {
                    stack.peek().getChildren().add(node);
                }
                stack.push(node);
            } else {
                closeNode(stack, row.methodName, row.depth, row.dt);
            }
        }

        closeUnclosedNodes(stack, roots);
        sortNodes(roots);
        return roots;
    }

    private TraceNode createNode(TraceRow row, Deque<TraceNode> stack) {
        TraceNode node = new TraceNode();
        node.setTraceId(row.traceId);
        node.setSignature(row.methodName);
        node.setAppName(row.appName);
        node.setServerIp(row.serverIp);
        node.setDepth(row.depth != null ? row.depth : (stack.isEmpty() ? 0 : stack.peek().getDepth() + 1));
        node.setStartTime(row.dt);
        return node;
    }

    private void closeNode(Deque<TraceNode> stack, String signature, Integer depth, Long endTime) {
        while (!stack.isEmpty()) {
            TraceNode candidate = stack.pop();
            if (matches(candidate, signature, depth)) {
                candidate.setEndTime(endTime);
                candidate.setDuration(calcDuration(candidate.getStartTime(), endTime));
                break;
            }
        }
    }

    private boolean matches(TraceNode node, String signature, Integer depth) {
        return Objects.equals(node.getSignature(), signature) ||
                (depth != null && Objects.equals(node.getDepth(), depth));
    }

    private void closeUnclosedNodes(Deque<TraceNode> stack, List<TraceNode> roots) {
        long fallbackEnd = findMaxTime(roots);
        while (!stack.isEmpty()) {
            TraceNode node = stack.pop();
            if (node.getEndTime() == null) {
                Long start = node.getStartTime();
                node.setEndTime(start != null ? start : fallbackEnd);
                node.setDuration(calcDuration(start, node.getEndTime()));
            }
        }
    }

    private long findMaxTime(List<TraceNode> nodes) {
        return nodes.stream()
                .map(n -> n.getEndTime() != null ? n.getEndTime() : n.getStartTime())
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(System.currentTimeMillis());
    }

    private long calcDuration(Long start, Long end) {
        if (start == null || end == null) return 0L;
        return Math.max(0, end - start);
    }

    private void sortNodes(List<TraceNode> nodes) {
        nodes.sort(Comparator.comparing(TraceNode::getStartTime, Comparator.nullsLast(Long::compareTo)));
        for (TraceNode node : nodes) {
            if (!node.getChildren().isEmpty()) {
                sortNodes(node.getChildren());
            }
        }
    }
}
