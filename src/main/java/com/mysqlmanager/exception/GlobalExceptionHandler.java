package com.mysqlmanager.exception;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.sql.SQLException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException e, Model model) {
        model.addAttribute("error", "Accesso negato: " + e.getMessage());
        return "error/403";
    }

    @ExceptionHandler(SQLException.class)
    public String handleSqlException(SQLException e, Model model) {
        model.addAttribute("error", "Errore MySQL: " + e.getMessage());
        model.addAttribute("sqlState", e.getSQLState());
        return "error/sql-error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException e, Model model) {
        model.addAttribute("error", e.getMessage());
        return "error/bad-request";
    }
}
