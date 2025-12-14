package com.make;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DemoTest {
    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(RuoYiApplication.class);


    public static void main(String[] args) throws IOException {
        String filePath ="E:\\copilot-host-amd64_3.1.0_2db5dfc0.tar.gz";
        // 对于大文件使用优化版本
        String s = calculateLargeFileMD5(filePath);
        System.out.println(s);
    }


    /**
     * 快速计算大文件的MD5哈希值（优化版，适用于100G以上大文件）
     *
     * @param filePath 文件路径
     * @return 文件的MD5哈希值（32位小写十六进制字符串）
     * @throws IOException 当文件读取失败时抛出
     *
     * <p>优化特性：
     * 1. 使用更大的缓冲区（64KB）提升大文件读取效率
     * 2. 使用RandomAccessFile提高大文件读取性能
     * 3. 进度日志输出（适用于长时间运行的任务）
     * 4. 内存效率优化，避免将整个文件加载到内存中
     */
    private static String calculateLargeFileMD5(String filePath) throws IOException {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            java.nio.file.Path path = java.nio.file.Paths.get(filePath);
            long fileSize = java.nio.file.Files.size(path);
            long totalRead = 0;

            // 对于大文件使用更大的缓冲区
            int bufferSize = fileSize > 1024 * 1024 * 1024 ? 65536 : 8192; // 1GB以上文件使用64KB缓冲区
            byte[] buffer = new byte[bufferSize];

            try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(filePath, "r")) {
                long startTime = System.currentTimeMillis();
                int bytesRead;

                while ((bytesRead = raf.read(buffer)) != -1) {
                    md.update(buffer, 0, bytesRead);
                    totalRead += bytesRead;

                    // 每处理5GB数据输出一次进度日志
                    if (totalRead % (5L * 1024 * 1024 * 1024) == 0) {
                        long elapsed = System.currentTimeMillis() - startTime;
                        double speed = totalRead / (1024.0 * 1024.0 * 1024.0) / (elapsed / 1000.0);
                        log.info("MD5计算进度: 已处理 {} GB, 平均速度: " + String.format("%.2f", speed) + " GB/s",
                                totalRead / (1024 * 1024 * 1024));
                    }
                }

                long elapsed = System.currentTimeMillis() - startTime;
                log.info("MD5计算完成，文件大小: {} bytes, 耗时: {} ms", fileSize, elapsed);

                byte[] digest = md.digest();
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) {
                    sb.append(String.format("%02x", b & 0xff));
                }
                return sb.toString();
            }
        } catch (java.security.NoSuchAlgorithmException e) {
            log.error("MD5算法不可用", e);
            throw new RuntimeException("无法计算文件MD5值", e);
        }
    }
}
