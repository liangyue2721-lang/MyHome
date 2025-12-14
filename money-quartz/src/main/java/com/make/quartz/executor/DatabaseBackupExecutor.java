package com.make.quartz.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库备份任务执行器
 * 负责实际执行数据库备份操作
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
     * 执行数据库备份任务
     */
    public void executeBackup() throws Exception {
        log.info("[DB-BACKUP] 开始执行数据库备份任务");
        Instant startInstant = Instant.now();
        
        try {
            // === 解析 JDBC URL（严格解析） ===
            if (StringUtils.isBlank(dbUrl)) {
                log.error("[DB-BACKUP] 数据库 URL 为空");
                throw new IllegalArgumentException("数据库 URL 为空");
            }
            
            String host;
            String port;
            String database;
            
            try {
                String url = dbUrl.trim();
                if (!url.startsWith("jdbc:mysql://")) {
                    log.error("[DB-BACKUP] 非法 JDBC URL：{}", url);
                    throw new IllegalArgumentException("非法 JDBC URL：" + url);
                }
                
                String withoutPrefix = url.substring("jdbc:mysql://".length());
                int slashIndex = withoutPrefix.indexOf('/');
                if (slashIndex <= 0) {
                    log.error("[DB-BACKUP] JDBC URL 缺少 host/port 部分：{}", dbUrl);
                    throw new IllegalArgumentException("JDBC URL 缺少 host/port 部分：" + dbUrl);
                }
                
                String hostPortPart = withoutPrefix.substring(0, slashIndex);
                String dbAndParams = withoutPrefix.substring(slashIndex + 1);
                
                int lastColon = hostPortPart.lastIndexOf(':');
                if (lastColon <= 0) {
                    log.error("[DB-BACKUP] JDBC URL 中无端口信息：{}", dbUrl);
                    throw new IllegalArgumentException("JDBC URL 中无端口信息：" + dbUrl);
                }
                
                host = hostPortPart.substring(0, lastColon);
                port = hostPortPart.substring(lastColon + 1);
                
                int qIdx = dbAndParams.indexOf('?');
                if (qIdx >= 0) {
                    database = dbAndParams.substring(0, qIdx);
                } else {
                    database = dbAndParams;
                }
                
                if (StringUtils.isAnyBlank(host, port, database)) {
                    log.error("[DB-BACKUP] JDBC URL 解析到空字段：host='{}', port='{}', database='{}'",
                            host, port, database);
                    throw new IllegalArgumentException("JDBC URL 解析到空字段");
                }
                
                log.info("[DB-BACKUP] URL 解析成功 -> host={}, port={}, database={}", host, port, database);
                
            } catch (Exception ex) {
                log.error("[DB-BACKUP] URL 解析异常：{}", dbUrl, ex);
                throw ex;
            }
            
            // === 备份文件路径 ===
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String backupDir = File.separator + "home" + File.separator + "Sql" + File.separator + dateDir;
            String backupFileName = database + "_backup_" + timestamp + ".sql";
            String backupFilePath = backupDir + File.separator + backupFileName;
            
            File dir = new File(backupDir);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    log.error("[DB-BACKUP] 创建目录失败: {}", backupDir);
                    throw new RuntimeException("创建目录失败: " + backupDir);
                }
                log.info("[DB-BACKUP] 目录创建成功：{}", backupDir);
            }
            
            // === mysqldump 可用性检查（可选） ===
            try {
                ProcessBuilder whichPb = new ProcessBuilder("/bin/sh", "-c", "which mysqldump");
                Process whichProc = whichPb.start();
                whichProc.waitFor();
            } catch (Throwable ignore) {
                // 非关键
            }
            
            // =====================================================
            // ===== 核心修复点：使用 --result-file= 而不是 -r =====
            // =====================================================
            List<String> command = new ArrayList<>();
            command.add("mysqldump");
            command.add("-h" + host);
            command.add("-P" + port);
            command.add("-u" + dbUsername);
            
            if (StringUtils.isNotEmpty(dbPassword)) {
                command.add("-p" + dbPassword);
            }
            
            command.add("--single-transaction");
            command.add("--routines");
            command.add("--triggers");
            command.add("--events");
            command.add("--set-gtid-purged=OFF");
            command.add("--default-character-set=utf8mb4");
            
            // ⭐⭐ 正确的文件输出方式，不依赖 shell 重定向 ⭐⭐
            command.add("--result-file=" + backupFilePath);
            
            // 数据库名
            command.add(database);
            
            String maskedCmd = maskPasswordInCommand(String.join(" ", command), dbPassword);
            log.info("[DB-BACKUP] 执行命令: {}", maskedCmd);
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);
            
            Process proc = pb.start();
            int exitCode = proc.waitFor();
            
            if (exitCode != 0) {
                StringBuilder err = new StringBuilder();
                try (BufferedReader errReader = new BufferedReader(new InputStreamReader(proc.getErrorStream()))) {
                    String line;
                    while ((line = errReader.readLine()) != null) {
                        err.append(line).append("\n");
                    }
                }
                log.error("[DB-BACKUP] 备份失败，exitCode={}，错误输出：\n{}", exitCode, err);
                throw new RuntimeException("备份失败，exitCode=" + exitCode);
            }
            
            // === 校验文件（此时文件一定不为空，只要 DB 有数据） ===
            File backupFile = new File(backupFilePath);
            if (!backupFile.exists()) {
                log.error("[DB-BACKUP] 备份文件未生成：{}", backupFilePath);
                throw new RuntimeException("备份文件未生成：" + backupFilePath);
            }
            
            long sizeBytes = Files.size(backupFile.toPath());
            double sizeMb = sizeBytes / 1024.0 / 1024.0;
            Duration duration = Duration.between(startInstant, Instant.now());
            
            log.info("[DB-BACKUP] 备份成功：文件={} 大小={} bytes（{} MB） 耗时={} 秒",
                    backupFilePath, sizeBytes, String.format("%.2f", sizeMb), duration.getSeconds());
            
        } catch (Exception e) {
            log.error("[DB-BACKUP] 备份任务异常: {}", e.getMessage(), e);
            throw e;
        }
        
        log.info("[DB-BACKUP] 备份任务结束");
    }
    
    /**
     * 日志掩码密码
     */
    private String maskPasswordInCommand(String cmd, String dbPassword) {
        if (StringUtils.isBlank(cmd) || StringUtils.isBlank(dbPassword)) {
            return cmd;
        }
        return cmd.replace("-p" + dbPassword, "-p****");
    }
}