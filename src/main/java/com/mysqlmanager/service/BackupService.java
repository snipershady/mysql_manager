package com.mysqlmanager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Service
public class BackupService {

    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter TIMESTAMP_PARSE = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final Pattern BACKUP_PATTERN = Pattern.compile("^(.+)_(\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2})\\.sql\\.gz$");

    @Value("${backup.path}")
    private String backupPath;

    @Value("${backup.mysqldump:/usr/bin/mysqldump}")
    private String mysqldumpBin;

    @Value("${backup.mysql:/usr/bin/mysql}")
    private String mysqlBin;

    @Value("${mysql.manager.host:127.0.0.1}")
    private String host;

    @Value("${mysql.manager.port:3306}")
    private int port;

    @Value("${mysql.manager.user:root}")
    private String user;

    @Value("${mysql.manager.password:}")
    private String password;

    /**
     * Esegue un backup completo tramite mysqldump (trigger, routines, eventi).
     * Output compresso in .sql.gz.
     *
     * @return path assoluto del file creato
     */
    public String backup(String database) throws IOException, InterruptedException {
        Path dir = Paths.get(backupPath);
        Files.createDirectories(dir);

        String filename = database + "_" + LocalDateTime.now().format(TIMESTAMP_FMT) + ".sql.gz";
        Path outFile = dir.resolve(filename);

        ProcessBuilder pb = new ProcessBuilder(
                mysqldumpBin,
                "-h", host,
                "-P", String.valueOf(port),
                "-u", user,
                "--triggers",
                "--routines",
                "--events",
                "--single-transaction",
                "--add-drop-database",
                "--set-gtid-purged=OFF",
                "--databases", database
        );
        pb.environment().put("MYSQL_PWD", password);

        Process process = pb.start();

        CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(() -> {
            try { return new String(process.getErrorStream().readAllBytes()).trim(); }
            catch (IOException e) { return e.getMessage(); }
        });

        try (InputStream in = process.getInputStream();
             GZIPOutputStream out = new GZIPOutputStream(Files.newOutputStream(outFile))) {
            in.transferTo(out);
        }

        int exitCode = process.waitFor();
        String stderr = stderrFuture.join();

        if (exitCode != 0) {
            Files.deleteIfExists(outFile);
            throw new IOException("mysqldump fallito (exit " + exitCode + "): " + stderr);
        }

        return outFile.toAbsolutePath().toString();
    }

    /**
     * Ripristina un backup sul database specificato.
     * Prima esegue un backup di sicurezza del database corrente, poi applica il dump selezionato.
     *
     * @param database   nome del database da sovrascrivere
     * @param filename   nome del file .sql.gz (solo filename, senza path)
     * @return path del backup di sicurezza creato prima del ripristino
     */
    public String restore(String database, String filename) throws IOException, InterruptedException {
        // Sicurezza: accetta solo filename senza separatori di path
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new IllegalArgumentException("Nome file non valido: " + filename);
        }

        Path backupFile = Paths.get(backupPath).resolve(filename);
        if (!Files.exists(backupFile)) {
            throw new IOException("File di backup non trovato: " + filename);
        }

        // Backup di sicurezza del database corrente prima di sovrascrivere
        String safetyBackupPath = backup(database);

        // Ripristino: decomprimi e invia allo stdin di mysql
        ProcessBuilder pb = new ProcessBuilder(
                mysqlBin,
                "-h", host,
                "-P", String.valueOf(port),
                "-u", user
        );
        pb.environment().put("MYSQL_PWD", password);

        Process process = pb.start();

        CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(() -> {
            try { return new String(process.getErrorStream().readAllBytes()).trim(); }
            catch (IOException e) { return e.getMessage(); }
        });

        try (GZIPInputStream gz = new GZIPInputStream(Files.newInputStream(backupFile));
             OutputStream stdin = process.getOutputStream()) {
            gz.transferTo(stdin);
        }

        int exitCode = process.waitFor();
        String stderr = stderrFuture.join();

        if (exitCode != 0) {
            throw new IOException("Ripristino fallito (exit " + exitCode + "): " + stderr);
        }

        return safetyBackupPath;
    }

    /**
     * Elenca tutti i backup presenti nella directory, dal più recente.
     */
    public List<BackupFile> listBackups() throws IOException {
        Path dir = Paths.get(backupPath);
        if (!Files.exists(dir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(p -> p.getFileName().toString().endsWith(".sql.gz"))
                    .map(this::toBackupFile)
                    .sorted(Comparator.comparing(BackupFile::createdAt).reversed())
                    .toList();
        }
    }

    private BackupFile toBackupFile(Path path) {
        String filename = path.getFileName().toString();
        long size = 0;
        try { size = Files.size(path); } catch (IOException ignored) {}

        Matcher m = BACKUP_PATTERN.matcher(filename);
        String dbName = m.matches() ? m.group(1) : filename;
        LocalDateTime createdAt;
        try {
            createdAt = m.matches()
                    ? LocalDateTime.parse(m.group(2), TIMESTAMP_PARSE)
                    : LocalDateTime.ofEpochSecond(path.toFile().lastModified() / 1000, 0, java.time.ZoneOffset.UTC);
        } catch (Exception e) {
            createdAt = LocalDateTime.now();
        }

        return new BackupFile(filename, dbName, size, createdAt, formatSize(size));
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    public record BackupFile(
            String filename,
            String database,
            long sizeBytes,
            LocalDateTime createdAt,
            String sizeFormatted
    ) {}
}
