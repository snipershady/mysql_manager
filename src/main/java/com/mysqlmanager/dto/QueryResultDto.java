package com.mysqlmanager.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class QueryResultDto {

    private final List<String> columns;
    private final List<List<Object>> rows;
    private final long executionTimeMs;
    private final int rowCount;
    private final boolean isUpdateQuery;
    private final int affectedRows;

    /** Per SELECT */
    public QueryResultDto(List<String> columns, List<List<Object>> rows, long executionTimeMs) {
        this.columns = columns;
        this.rows = rows;
        this.executionTimeMs = executionTimeMs;
        this.rowCount = rows.size();
        this.isUpdateQuery = false;
        this.affectedRows = 0;
    }

    /** Per INSERT/UPDATE/DELETE/DDL */
    public QueryResultDto(int affectedRows, long executionTimeMs) {
        this.columns = List.of();
        this.rows = List.of();
        this.executionTimeMs = executionTimeMs;
        this.rowCount = 0;
        this.isUpdateQuery = true;
        this.affectedRows = affectedRows;
    }
}
