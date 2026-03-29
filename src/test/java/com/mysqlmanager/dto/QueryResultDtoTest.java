package com.mysqlmanager.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QueryResultDtoTest {

    @Test
    void selectConstructor() {
        List<String> cols = List.of("id", "name");
        List<List<Object>> rows = List.of(
                List.of(1, "Alice"),
                List.of(2, "Bob")
        );
        QueryResultDto dto = new QueryResultDto(cols, rows, 42L);

        assertThat(dto.getColumns()).isEqualTo(cols);
        assertThat(dto.getRows()).isEqualTo(rows);
        assertThat(dto.getExecutionTimeMs()).isEqualTo(42L);
        assertThat(dto.getRowCount()).isEqualTo(2);
        assertThat(dto.isUpdateQuery()).isFalse();
        assertThat(dto.getAffectedRows()).isZero();
    }

    @Test
    void updateConstructor() {
        QueryResultDto dto = new QueryResultDto(5, 10L);

        assertThat(dto.getColumns()).isEmpty();
        assertThat(dto.getRows()).isEmpty();
        assertThat(dto.getExecutionTimeMs()).isEqualTo(10L);
        assertThat(dto.getRowCount()).isZero();
        assertThat(dto.isUpdateQuery()).isTrue();
        assertThat(dto.getAffectedRows()).isEqualTo(5);
    }

    @Test
    void emptyResultSet() {
        QueryResultDto dto = new QueryResultDto(List.of("col"), List.of(), 1L);
        assertThat(dto.getRowCount()).isZero();
        assertThat(dto.isUpdateQuery()).isFalse();
    }
}
