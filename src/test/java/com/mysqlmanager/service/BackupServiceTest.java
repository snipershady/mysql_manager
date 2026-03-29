package com.mysqlmanager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BackupServiceTest {

    @TempDir
    Path tempDir;

    private BackupService service;

    @BeforeEach
    void setUp() {
        service = new BackupService();
        ReflectionTestUtils.setField(service, "backupPath", tempDir.toString());
        ReflectionTestUtils.setField(service, "mysqldumpBin", "/usr/bin/mysqldump");
        ReflectionTestUtils.setField(service, "mysqlBin", "/usr/bin/mysql");
        ReflectionTestUtils.setField(service, "host", "127.0.0.1");
        ReflectionTestUtils.setField(service, "port", 3306);
        ReflectionTestUtils.setField(service, "user", "root");
        ReflectionTestUtils.setField(service, "password", "secret");
    }

    // --- listBackups ---

    @Test
    void listBackupsReturnsEmptyWhenDirectoryDoesNotExist() throws IOException {
        ReflectionTestUtils.setField(service, "backupPath", tempDir.resolve("nonexistent").toString());

        List<BackupService.BackupFile> backups = service.listBackups();

        assertThat(backups).isEmpty();
    }

    @Test
    void listBackupsReturnsEmptyWhenNoSqlGzFiles() throws IOException {
        Files.writeString(tempDir.resolve("readme.txt"), "ignore me");

        List<BackupService.BackupFile> backups = service.listBackups();

        assertThat(backups).isEmpty();
    }

    @Test
    void listBackupsReturnsSortedByDateDesc() throws IOException {
        createGzipFile("mydb_2026-01-01_10-00-00.sql.gz", "-- old");
        createGzipFile("mydb_2026-06-01_10-00-00.sql.gz", "-- new");

        List<BackupService.BackupFile> backups = service.listBackups();

        assertThat(backups).hasSize(2);
        assertThat(backups.get(0).filename()).isEqualTo("mydb_2026-06-01_10-00-00.sql.gz");
        assertThat(backups.get(1).filename()).isEqualTo("mydb_2026-01-01_10-00-00.sql.gz");
    }

    @Test
    void listBackupsParsesDbNameAndTimestamp() throws IOException {
        createGzipFile("testdb_2026-03-15_12-30-45.sql.gz", "-- dump");

        List<BackupService.BackupFile> backups = service.listBackups();

        assertThat(backups).hasSize(1);
        BackupService.BackupFile bf = backups.get(0);
        assertThat(bf.database()).isEqualTo("testdb");
        assertThat(bf.createdAt().getYear()).isEqualTo(2026);
        assertThat(bf.createdAt().getMonthValue()).isEqualTo(3);
    }

    @Test
    void listBackupsFormatsSize() throws IOException {
        // Write random bytes directly (not via gzip text helper) to guarantee > 1 KB on disk
        byte[] randomData = new byte[8192];
        new Random(42).nextBytes(randomData);
        Path file = tempDir.resolve("db_2026-01-01_00-00-00.sql.gz");
        Files.write(file, randomData);

        List<BackupService.BackupFile> backups = service.listBackups();

        assertThat(backups.get(0).sizeFormatted()).endsWith("KB").contains(".");
    }

    // --- listTableBackups ---

    @Test
    void listTableBackupsReturnsEmptyWhenDirectoryDoesNotExist() throws IOException {
        ReflectionTestUtils.setField(service, "backupPath", tempDir.resolve("nonexistent").toString());

        List<BackupService.TableBackupFile> backups = service.listTableBackups();

        assertThat(backups).isEmpty();
    }

    @Test
    void listTableBackupsIgnoresFullDatabaseDumps() throws IOException {
        createGzipFile("mydb_2026-01-01_10-00-00.sql.gz", "-- full db dump");

        List<BackupService.TableBackupFile> tableBackups = service.listTableBackups();

        assertThat(tableBackups).isEmpty();
    }

    @Test
    void listTableBackupsReturnsSortedByDateDesc() throws IOException {
        createGzipFile("mydb.users_2026-01-01_10-00-00.sql.gz", "-- old");
        createGzipFile("mydb.users_2026-06-01_10-00-00.sql.gz", "-- new");

        List<BackupService.TableBackupFile> backups = service.listTableBackups();

        assertThat(backups).hasSize(2);
        assertThat(backups.get(0).filename()).isEqualTo("mydb.users_2026-06-01_10-00-00.sql.gz");
    }

    @Test
    void listTableBackupsParsesDbAndTable() throws IOException {
        createGzipFile("mydb.orders_2026-05-10_08-00-00.sql.gz", "-- dump");

        List<BackupService.TableBackupFile> backups = service.listTableBackups();

        assertThat(backups).hasSize(1);
        BackupService.TableBackupFile tbf = backups.get(0);
        assertThat(tbf.database()).isEqualTo("mydb");
        assertThat(tbf.table()).isEqualTo("orders");
        assertThat(tbf.createdAt().getYear()).isEqualTo(2026);
    }

    // --- restoreTable: filename validation ---

    @Test
    void restoreTableRejectsPathTraversal() {
        assertThatThrownBy(() -> service.restoreTable("mydb", "../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non valido");
    }

    @Test
    void restoreTableRejectsNonTableFilename() {
        assertThatThrownBy(() -> service.restoreTable("mydb", "mydb_2026-01-01_00-00-00.sql.gz"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dump di tabella");
    }

    @Test
    void restoreTableRejectsBackslash() {
        assertThatThrownBy(() -> service.restoreTable("mydb", "dir\\file.sql.gz"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void restoreTableThrowsWhenFileNotFound() {
        assertThatThrownBy(() -> service.restoreTable("mydb", "mydb.users_2026-01-01_00-00-00.sql.gz"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("non trovato");
    }

    // --- restore: filename validation ---

    @Test
    void restoreRejectsPathTraversal() {
        assertThatThrownBy(() -> service.restore("mydb", "../evil.sql.gz"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void restoreThrowsWhenFileNotFound() {
        assertThatThrownBy(() -> service.restore("mydb", "missing_2026-01-01_00-00-00.sql.gz"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("non trovato");
    }

    // --- BackupFile record ---

    @Test
    void backupFileRecord() {
        var bf = new BackupService.BackupFile("f.sql.gz", "mydb", 1024L,
                java.time.LocalDateTime.of(2026, 1, 1, 0, 0), "1.0 KB");
        assertThat(bf.filename()).isEqualTo("f.sql.gz");
        assertThat(bf.database()).isEqualTo("mydb");
        assertThat(bf.sizeBytes()).isEqualTo(1024L);
        assertThat(bf.sizeFormatted()).isEqualTo("1.0 KB");
    }

    // --- TableBackupFile record ---

    @Test
    void tableBackupFileRecord() {
        var tbf = new BackupService.TableBackupFile("f.sql.gz", "mydb", "users", 512L,
                java.time.LocalDateTime.of(2026, 1, 1, 0, 0), "512 B");
        assertThat(tbf.table()).isEqualTo("users");
        assertThat(tbf.sizeFormatted()).isEqualTo("512 B");
    }

    // --- helpers ---

    private void createGzipFile(String filename, String content) throws IOException {
        Path file = tempDir.resolve(filename);
        try (GZIPOutputStream gz = new GZIPOutputStream(Files.newOutputStream(file))) {
            gz.write(content.getBytes());
        }
    }
}
