package com.mysqlmanager.controller;

import com.mysqlmanager.dto.QueryResultDto;
import com.mysqlmanager.service.AuditService;
import com.mysqlmanager.service.DatabaseManagerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class QueryController {

    private final DatabaseManagerService dbService;
    private final AuditService auditService;

    @GetMapping("/db/{database}/query")
    public String queryPage(@PathVariable String database, Model model) {
        model.addAttribute("database", database);
        return "db/query";
    }

    @PostMapping("/db/{database}/query")
    public String executeQuery(@PathVariable String database,
                                @RequestParam String sql,
                                Authentication auth,
                                HttpServletRequest request,
                                Model model) {
        model.addAttribute("database", database);
        model.addAttribute("sql", sql);

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        long start = System.currentTimeMillis();
        try {
            QueryResultDto result = dbService.executeQuery(database, sql, isAdmin);
            long elapsed = System.currentTimeMillis() - start;
            auditService.log(auth.getName(), request.getRemoteAddr(), database, sql, elapsed, true, null);
            model.addAttribute("result", result);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            auditService.log(auth.getName(), request.getRemoteAddr(), database, sql, elapsed, false, e.getMessage());
            model.addAttribute("error", e.getMessage());
        }
        return "db/query";
    }

    @GetMapping("/db/{database}/table/{table}")
    public String describeTable(@PathVariable String database,
                                 @PathVariable String table,
                                 Model model) {
        try {
            model.addAttribute("database", database);
            model.addAttribute("table", table);
            model.addAttribute("columns", dbService.describeTable(database, table));
            model.addAttribute("createSql", dbService.showCreateTable(database, table));
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "db/table-detail";
    }
}
