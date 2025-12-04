package com.logix.server.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页结果
 *
 * @author Kanade
 * @since 2025/11/24
 */
@Data
@NoArgsConstructor
public class PageResult<T> {
    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码（从1开始）
     */
    private Integer pageNum;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 数据列表
     */
    private List<T> data;

    /**
     * 是否有下一页
     */
    private Boolean hasNext;

    /**
     * 总页数
     */
    private Integer totalPages;

    /**
     * 全参构造函数
     */
    public PageResult(Long total, Integer pageNum, Integer pageSize, List<T> data) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.data = data;
        this.calculateDerivedFields();
    }

    /**
     * 计算派生字段（hasNext, totalPages）
     */
    private void calculateDerivedFields() {
        if (total != null && pageNum != null && pageSize != null && pageSize > 0) {
            // 计算是否有下一页
            this.hasNext = (long) pageNum * pageSize < total;

            // 计算总页数
            this.totalPages = (int) Math.ceil((double) total / pageSize);
        } else {
            this.hasNext = false;
            this.totalPages = 0;
        }
    }
}
