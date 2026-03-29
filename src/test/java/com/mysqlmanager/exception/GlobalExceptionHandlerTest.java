package com.mysqlmanager.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private Model model;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        model = new ExtendedModelMap();
    }

    @Test
    void handleAccessDeniedReturns403View() {
        String view = handler.handleAccessDenied(new AccessDeniedException("Non autorizzato"), model);

        assertThat(view).isEqualTo("error/403");
        assertThat(model.getAttribute("error")).asString().contains("Non autorizzato");
    }

    @Test
    void handleSqlExceptionReturnsSqlErrorView() throws Exception {
        SQLException ex = new SQLException("Table not found", "42S02", 1146);

        String view = handler.handleSqlException(ex, model);

        assertThat(view).isEqualTo("error/sql-error");
        assertThat(model.getAttribute("error")).asString().contains("Table not found");
        assertThat(model.getAttribute("sqlState")).isEqualTo("42S02");
    }

    @Test
    void handleIllegalArgumentReturnsBadRequestView() {
        String view = handler.handleIllegalArgument(
                new IllegalArgumentException("Identificatore non valido"), model);

        assertThat(view).isEqualTo("error/bad-request");
        assertThat(model.getAttribute("error")).isEqualTo("Identificatore non valido");
    }

    @Test
    void handleSqlExceptionWithNullState() throws Exception {
        SQLException ex = new SQLException("Generic error", (String) null);

        String view = handler.handleSqlException(ex, model);

        assertThat(view).isEqualTo("error/sql-error");
        assertThat(model.getAttribute("sqlState")).isNull();
    }
}
