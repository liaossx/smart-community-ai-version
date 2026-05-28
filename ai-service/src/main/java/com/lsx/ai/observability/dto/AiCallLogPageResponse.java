package com.lsx.ai.observability.dto;

import java.util.ArrayList;
import java.util.List;

public class AiCallLogPageResponse {
    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private List<AiCallLogItem> records = new ArrayList<>();

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

    public List<AiCallLogItem> getRecords() {
        return records;
    }

    public void setRecords(List<AiCallLogItem> records) {
        this.records = records;
    }
}
