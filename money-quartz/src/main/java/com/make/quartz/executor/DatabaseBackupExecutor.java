package com.make.quartz.executor;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 生产级 MySQL 备份执行器（结构 + 数据）
 * 特性：
 * 1) 默认使用 single-transaction（InnoDB 一致性无锁）
 * 2) 检测到非 InnoDB 表时，自动切换 lock-all-tables（保证完整一致）
 * 3) 使用 --defaults-extra-file 避免密码暴露到命令行
 * 4) 写入临时文件后原子移动，避免生成半截备份文件
 * 5) 关键校验：文件存在、大小、基本内容（CREATE/INSERT 等）
 */
@Component
public class DatabaseBackupExecutor {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBackupExecutor.class);

    @Value("${spring.datasource.druid.master.username}")
    private String dbUsername;

    @Value("${spring.datasource.druid.master.password}")
    private String dbPassword;

    @Value("${spring.datasource.druid.master.url}")
    private String dbUrl;

    /**
     * 建议通过配置指定 mysqldump 绝对路径，如 /usr/bin/mysqldump
     * 留空则使用系统 PATH 查找。
     */
    @Value("${db.backup.mysqldump:mysqldump}")
    private String mysqldumpPath;

    /**
     * 备份根目录（生产建议独立挂载大盘）
     */
    @Value("${db.backup.baseDir:/home/Sql}")
    private String backupBaseDir;

    /**
     * 单次备份超时（秒）
     */
    @Value("${db.backup.timeoutSeconds:1800}")
    private long timeoutSeconds;

    /**
     * 是否导出 routines / triggers / events（按需开关）
     */
    @Value("${db.backup.dumpRoutines:true}")
    private boolean dumpRoutines;

    @Value("${db.backup.dumpTriggers:true}")
    private boolean dumpTriggers;

    @Value("${db.backup.dumpEvents:true}")
    private boolean dumpEvents;

    /**
     * 执行数据库备份任务
     */
    public void executeBackup() throws Exception {
        Instant start = Instant.now();
        log.info("[DB-BACKUP] Start backup");

        JdbcInfo jdbc = parseMysqlJdbcUrl(dbUrl);
        String host = jdbc.host;
        int port = jdbc.port;
        String database = jdbc.database;

        // 备份目录：/baseDir/yyyy-MM-dd/
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        Path dir = Paths.get(backupBaseDir, dateDir);
        Files.createDirectories(dir);

        String fileName = database + "_backup_" + ts + ".sql";
        Path finalFile = dir.resolve(fileName);

        // 临时文件：写完再 move，避免半截文件被当成成功备份
        Path tempFile = dir.resolve(fileName + ".tmp");

        // 1) 检测是否存在非 InnoDB 表（决定一致性策略）
        boolean hasNonInnoDb = detectNonInnoDbTables(host, port, database);

        // 2) 创建临时 defaults-extra-file（避免命令行暴露密码）
        Path defaultsFile = null;
        try {
            defaultsFile = createDefaultsExtraFile(host, port, dbUsername, dbPassword);

            List<String> cmd = buildMysqldumpCommand(defaultsFile, database, tempFile, hasNonInnoDb);

            log.info("[DB-BACKUP] Strategy: {}", hasNonInnoDb ? "LOCK_ALL_TABLES (non-InnoDB detected)" : "SINGLE_TRANSACTION (InnoDB)");
            log.info("[DB-BACKUP] Execute: {} ...", safeCommandForLog(cmd));

            ExecResult result = exec(cmd, timeoutSeconds);

            if (result.timedOut) {
                throw new RuntimeException("Backup timed out after " + timeoutSeconds + " seconds");
            }
            if (result.exitCode != 0) {
                throw new RuntimeException("Backup failed, exitCode=" + result.exitCode + ", stderr=" + result.stderrTail);
            }

            // 3) 关键校验：文件存在、大小、内容基本特征
            validateDumpFile(tempFile);

            // 4) 原子移动到最终文件
            try {
                Files.move(tempFile, finalFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                // 某些文件系统不支持 ATOMIC_MOVE，则降级
                Files.move(tempFile, finalFile, StandardCopyOption.REPLACE_EXISTING);
            }

            long sizeBytes = Files.size(finalFile);
            double sizeMb = sizeBytes / 1024.0 / 1024.0;

            Duration cost = Duration.between(start, Instant.now());
            log.info("[DB-BACKUP] Success: file={}, size={} bytes ({} MB), cost={}s",
                    finalFile, sizeBytes, String.format("%.2f", sizeMb), cost.getSeconds());

        } catch (Exception e) {
            log.error("[DB-BACKUP] Failed: {}", e.getMessage(), e);
            // 清理临时文件（若存在）
            safeDelete(tempFile);
            throw e;
        } finally {
            // 清理 defaults-extra-file（含敏感信息）
            safeDelete(defaultsFile);
        }

        log.info("[DB-BACKUP] End backup");
    }

    /**
     * 解析 MySQL JDBC URL（支持常见形态：jdbc:mysql://host:port/db?params）
     */
    private JdbcInfo parseMysqlJdbcUrl(String url) {
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("dbUrl is blank");
        }
        String u = url.trim();
        if (!u.startsWith("jdbc:mysql://")) {
            throw new IllegalArgumentException("Invalid MySQL JDBC URL: " + u);
        }

        // jdbc:mysql://host:port/db?xxx
        // 兼容 IPv6: jdbc:mysql://[::1]:3306/db?xxx
        Pattern p = Pattern.compile("^jdbc:mysql://([^/]+)/([^?]+)(\\?.*)?$");
        Matcher m = p.matcher(u);
        if (!m.find()) {
            throw new IllegalArgumentException("Invalid MySQL JDBC URL format: " + u);
        }

        String hostPort = m.group(1);
        String db = m.group(2);

        String host;
        int port;

        if (hostPort.startsWith("[") && hostPort.contains("]")) { // IPv6
            int idx = hostPort.indexOf("]");
            host = hostPort.substring(1, idx);
            String rest = hostPort.substring(idx + 1);
            if (rest.startsWith(":")) {
                port = Integer.parseInt(rest.substring(1));
            } else {
                port = 3306;
            }
        } else {
            int c = hostPort.lastIndexOf(':');
            if (c > 0) {
                host = hostPort.substring(0, c);
                port = Integer.parseInt(hostPort.substring(c + 1));
            } else {
                host = hostPort;
                port = 3306;
            }
        }

        if (StringUtils.isAnyBlank(host, db)) {
            throw new IllegalArgumentException("Parsed JDBC URL has blank fields");
        }

        log.info("[DB-BACKUP] JDBC parsed: host={}, port={}, database={}", host, port, db);
        return new JdbcInfo(host, port, db);
    }

    /**
     * 检测是否存在非 InnoDB 表：
     * - 若存在，为保证一致性，mysqldump 需使用 --lock-all-tables（或业务允许停写/锁库）
     */
    private boolean detectNonInnoDbTables(String host, int port, String database) {
        // 生产建议：如环境中 mysql 客户端不可用，可改为 JDBC 查询 information_schema.tables
        // 这里优先复用 mysql 命令更轻量；若失败则保守返回 true（宁可锁表，也不要不完整）
        Path defaultsFile = null;
        try {
            defaultsFile = createDefaultsExtraFile(host, port, dbUsername, dbPassword);

            List<String> cmd = new ArrayList<>();
            cmd.add("mysql");
            cmd.add("--defaults-extra-file=" + defaultsFile.toAbsolutePath());
            cmd.add("-N"); // skip column names
            cmd.add("-e");
            cmd.add("SELECT COUNT(1) FROM information_schema.tables WHERE table_schema='" + escapeSql(database) + "' AND engine IS NOT NULL AND engine <> 'InnoDB';");

            ExecResult r = exec(cmd, Math.min(60, timeoutSeconds));
            if (r.exitCode != 0 || r.timedOut) {
                log.warn("[DB-BACKUP] detectNonInnoDbTables check failed, fallback to conservative LOCK_ALL_TABLES. stderr={}", r.stderrTail);
                return true;
            }
            String out = r.stdoutTrim();
            long cnt = 0;
            try {
                cnt = Long.parseLong(out.isEmpty() ? "0" : out);
            } catch (NumberFormatException ignore) {
                log.warn("[DB-BACKUP] detectNonInnoDbTables parse output failed: '{}', fallback to LOCK_ALL_TABLES", out);
                return true;
            }
            return cnt > 0;
        } catch (Exception e) {
            log.warn("[DB-BACKUP] detectNonInnoDbTables exception, fallback to LOCK_ALL_TABLES: {}", e.getMessage());
            return true;
        } finally {
            safeDelete(defaultsFile);
        }
    }

    /**
     * 构建 mysqldump 命令：结构 + 数据
     * 输出使用 --result-file=（不依赖 shell 重定向）
     */
    private List<String> buildMysqldumpCommand(Path defaultsFile, String database, Path outputFile, boolean lockAllTables) {
        List<String> cmd = new ArrayList<>();
        cmd.add(mysqldumpPath);

        // 使用 defaults-extra-file，避免 -pPASSWORD 出现在命令行
        cmd.add("--defaults-extra-file=" + defaultsFile.toAbsolutePath());

        // 关键：结构 + 数据（默认即包含，以下参数用于增强兼容性与完整性）
        cmd.add("--databases");
        cmd.add(database);

        // 一致性策略（二选一）
        if (lockAllTables) {
            // 最强一致性：锁所有表（包含非事务表）
            cmd.add("--lock-all-tables");
        } else {
            // InnoDB 一致性快照
            cmd.add("--single-transaction");
            // 避免额外 LOCK TABLES（对 InnoDB 更友好）
            cmd.add("--skip-lock-tables");
        }

        // 推荐：更稳定的导出方式（大表更友好）
        cmd.add("--quick");
        cmd.add("--hex-blob");

        // 视图/触发器/事件/存储过程（按需）
        if (dumpRoutines) cmd.add("--routines");
        if (dumpTriggers) cmd.add("--triggers");
        if (dumpEvents) cmd.add("--events");

        // MySQL 8+ 兼容：某些环境会因 column statistics 导致导入/兼容问题
        cmd.add("--column-statistics=0");

        // GTID 环境常见：避免导出时写入 SET @@GLOBAL.GTID_PURGED 影响恢复
        cmd.add("--set-gtid-purged=OFF");

        // 避免缺少 FILE 权限时报错（部分 MySQL 8 场景）
        cmd.add("--no-tablespaces");

        // 字符集
        cmd.add("--default-character-set=utf8mb4");

        // 输出文件（不走 stdout，避免缓冲/截断风险）
        cmd.add("--result-file=" + outputFile.toAbsolutePath());

        // 可选：更可控的 insert 形态（按需开启）
        // cmd.add("--extended-insert");
        // cmd.add("--complete-insert");

        return cmd;
    }

    /**
     * 创建临时 defaults-extra-file（包含 [client] user/password/host/port）
     * 文件权限尽量收紧，使用完立即删除。
     */
    private Path createDefaultsExtraFile(String host, int port, String user, String password) throws IOException {
        if (StringUtils.isBlank(user)) throw new IllegalArgumentException("dbUsername is blank");

        Path f = Files.createTempFile("mysql-client-", ".cnf");
        // 尽量收紧权限（Linux/macOS 下有效；Windows 可能无效）
        try {
            Set<PosixFilePermission> perms = EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
            );
            Files.setPosixFilePermissions(f, perms);
        } catch (Exception ignore) {
            // 非关键：不同文件系统/OS 可能不支持
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[client]\n");
        sb.append("user=").append(user).append("\n");
        if (StringUtils.isNotEmpty(password)) {
            sb.append("password=").append(password).append("\n");
        }
        sb.append("host=").append(host).append("\n");
        sb.append("port=").append(port).append("\n");

        Files.writeString(f, sb.toString(), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
        return f;
    }

    /**
     * 执行命令并返回结果（读取 stdout/stderr，防止缓冲区阻塞）
     */
    private ExecResult exec(List<String> cmd, long timeoutSec) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);

        Process p = pb.start();

        StreamCollector stdout = new StreamCollector(p.getInputStream(), 64_000);
        StreamCollector stderr = new StreamCollector(p.getErrorStream(), 64_000);

        Thread t1 = new Thread(stdout, "cmd-stdout");
        Thread t2 = new Thread(stderr, "cmd-stderr");
        t1.start();
        t2.start();

        boolean finished = p.waitFor(timeoutSec, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
        }

        t1.join(TimeUnit.SECONDS.toMillis(5));
        t2.join(TimeUnit.SECONDS.toMillis(5));

        int exit = finished ? p.exitValue() : -1;

        return new ExecResult(exit, !finished, stdout.getText(), stderr.getText());
    }

    /**
     * 校验备份文件：存在、大小、具备基本 SQL 结构
     */
    private void validateDumpFile(Path dumpFile) throws IOException {
        if (dumpFile == null || !Files.exists(dumpFile)) {
            throw new RuntimeException("Dump file not generated: " + dumpFile);
        }
        long size = Files.size(dumpFile);
        if (size < 256) {
            throw new RuntimeException("Dump file too small, may be invalid. size=" + size + ", file=" + dumpFile);
        }

        // 读取前几 KB 进行基本特征判断（避免全文件扫描）
        byte[] head = readHead(dumpFile, 16 * 1024);
        String s = new String(head, StandardCharsets.UTF_8);

        // 基本判断：至少出现一些典型关键字（不同版本 mysqldump 头部略有差异）
        boolean ok = s.contains("MySQL dump") || s.contains("CREATE TABLE") || s.contains("INSERT INTO") || s.contains("SET NAMES");
        if (!ok) {
            throw new RuntimeException("Dump file content looks abnormal (missing typical dump markers). file=" + dumpFile);
        }
    }

    private byte[] readHead(Path file, int maxBytes) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int total = 0;
            int r;
            while ((r = in.read(buf)) > 0) {
                int take = Math.min(r, maxBytes - total);
                out.write(buf, 0, take);
                total += take;
                if (total >= maxBytes) break;
            }
            return out.toByteArray();
        }
    }

    private void safeDelete(Path p) {
        if (p == null) return;
        try {
            Files.deleteIfExists(p);
        } catch (Exception ignore) {
        }
    }

    /**
     * 日志中仅输出安全信息：去掉 defaults 文件路径的敏感联想不强，但避免过度暴露
     */
    private String safeCommandForLog(List<String> cmd) {
        List<String> copy = new ArrayList<>(cmd.size());
        for (String c : cmd) {
            if (c.startsWith("--defaults-extra-file=")) {
                copy.add("--defaults-extra-file=****");
            } else {
                copy.add(c);
            }
        }
        return String.join(" ", copy);
    }

    private String escapeSql(String s) {
        return s == null ? "" : s.replace("'", "''");
    }

    private static class JdbcInfo {
        final String host;
        final int port;
        final String database;

        JdbcInfo(String host, int port, String database) {
            this.host = host;
            this.port = port;
            this.database = database;
        }
    }

    private static class ExecResult {
        final int exitCode;
        final boolean timedOut;
        final String stdout;
        final String stderrTail;

        ExecResult(int exitCode, boolean timedOut, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.timedOut = timedOut;
            this.stdout = stdout == null ? "" : stdout;
            this.stderrTail = tail(stderr, 8000);
        }

        String stdoutTrim() {
            return stdout.trim();
        }

        static String tail(String s, int max) {
            if (s == null) return "";
            if (s.length() <= max) return s;
            return s.substring(s.length() - max);
        }
    }

    /**
     * 收集输出流（限制最大字符，避免极端情况下内存增长）
     */
    private static class StreamCollector implements Runnable {
        private final InputStream in;
        private final int maxChars;
        private final StringBuilder sb = new StringBuilder();

        StreamCollector(InputStream in, int maxChars) {
            this.in = in;
            this.maxChars = maxChars;
        }

        @Override
        public void run() {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (sb.length() < maxChars) {
                        sb.append(line).append('\n');
                    }
                }
            } catch (IOException ignore) {
            }
        }

        String getText() {
            return sb.toString();
        }
    }
}
