package com.lsx.ai.operations.dto;

import java.util.ArrayList;
import java.util.List;

public class OperationsActionTaskPageResponse {
    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private List<OperationsActionTaskItem> records = new ArrayList<>();

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public List<OperationsActionTaskItem> getRecords() {
        return records;
    }

    public void setRecords(List<OperationsActionTaskItem> records) {
        this.records = records;
    }
}
