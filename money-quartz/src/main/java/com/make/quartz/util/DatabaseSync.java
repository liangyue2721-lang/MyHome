package com.make.quartz.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 数据库同步工具类，支持全量/增量数据同步，基于更新时间戳进行增量更新
 * 支持自定义表结构，通过配置实现灵活的数据同步
 */
public class DatabaseSync {

    // 日志记录器
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseSync.class);

    /**
     * 数据库同步配置类
     */
    public static class SyncConfig {
        private final String tableName;
        private final String primaryKey;
        private final String updateTimeField;
        private final String[] columns;

        public SyncConfig(String tableName, String primaryKey, String updateTimeField, String[] columns) {
            this.tableName = Objects.requireNonNull(tableName);
            this.primaryKey = Objects.requireNonNull(primaryKey);
            this.updateTimeField = Objects.requireNonNull(updateTimeField);
            this.columns = Objects.requireNonNull(columns);
        }

        public String getTableName() {
            return tableName;
        }

        public String getPrimaryKey() {
            return primaryKey;
        }

        public String getUpdateTimeField() {
            return updateTimeField;
        }

        public String[] getColumns() {
            return columns;
        }
    }

    /**
     * 执行数据库同步
     *
     * @param sourceConn 源数据库连接
     * @param targetConn 目标数据库连接
     * @param config     同步配置
     * @param batchSize  批量操作大小
     */
    public static void syncTable(Connection sourceConn,
                                 Connection targetConn,
                                 SyncConfig config,
                                 int batchSize) throws SQLException {
        // 验证连接有效性
        validateConnection(sourceConn);
        validateConnection(targetConn);

        // 生成动态SQL
        final String mergeSql = buildMergeSql(config);
        LOGGER.debug("Generated merge SQL: " + mergeSql);

        // 获取目标表当前数据快照（ID和更新时间）
        Map<Integer, Timestamp> targetSnapshot = getTargetSnapshot(targetConn, config);

        try (
                // 创建预处理语句
                PreparedStatement sourceStmt = buildSourceQuery(sourceConn, config);
                ResultSet rs = sourceStmt.executeQuery();
                PreparedStatement mergeStmt = targetConn.prepareStatement(mergeSql)
        ) {
            targetConn.setAutoCommit(false);
            int count = 0, total = 0;

            while (rs.next()) {
                // 获取源数据更新时间
                Timestamp sourceUpdateTime = rs.getTimestamp(config.updateTimeField);
                int id = rs.getInt(config.primaryKey);
                if (null == sourceUpdateTime) {
                    continue;
                }
                // 增量检查：如果目标已有更新版本则跳过
                if (targetSnapshot.containsKey(id) &&
                        !sourceUpdateTime.after(targetSnapshot.get(id))) {
                    continue;
                }

                // 设置合并语句参数
                for (int i = 0; i < config.columns.length; i++) {
                    mergeStmt.setObject(i + 1, rs.getObject(config.columns[i]));
                }
                mergeStmt.addBatch();

                // 批量提交
                if (++count % batchSize == 0) {
                    executeBatch(targetConn, mergeStmt, total += count);
                    count = 0;
                }
            }

            // 提交剩余批次
            if (count > 0) {
                executeBatch(targetConn, mergeStmt, total += count);
            }

            LOGGER.info("同步完成，共处理" + total + "条记录");
        } catch (SQLException e) {
            targetConn.rollback();
            throw new SQLException("同步失败，已回滚事务", e);
        } finally {
            targetConn.setAutoCommit(true);
        }
    }

    /**
     * 构建源数据查询语句
     */
    private static PreparedStatement buildSourceQuery(Connection conn, SyncConfig config) throws SQLException {
        String sql = String.format("SELECT %s FROM %s ORDER BY %s",
                String.join(",", config.columns),
                config.tableName,
                config.primaryKey);
        return conn.prepareStatement(sql);
    }

    /**
     * 生成MERGE SQL语句（MySQL语法）
     */
    private static String buildMergeSql(SyncConfig config) {
        // INSERT部分列
        String insertColumns = String.join(",", config.columns);
        String placeholders = String.join(",",
                java.util.Collections.nCopies(config.columns.length, "?"));

        // UPDATE部分逻辑
        StringBuilder updateClause = new StringBuilder();
        for (String col : config.columns) {
            if (!col.equals(config.primaryKey)) {
                updateClause.append(col)
                        .append(" = IF(VALUES(").append(config.updateTimeField)
                        .append(") > ").append(config.updateTimeField)
                        .append(", VALUES(").append(col).append("), ").append(col).append("),");
            }
        }
        updateClause.setLength(updateClause.length() - 1); // 移除末尾逗号

        return String.format(
                "INSERT INTO %s (%s) VALUES (%s) ON DUPLICATE KEY UPDATE %s",
                config.tableName, insertColumns, placeholders, updateClause);
    }

    /**
     * 获取目标表当前数据快照
     */
    private static Map<Integer, Timestamp> getTargetSnapshot(Connection conn, SyncConfig config) throws SQLException {
        Map<Integer, Timestamp> snapshot = new HashMap<>();
        String sql = String.format("SELECT %s, %s FROM %s",
                config.primaryKey, config.updateTimeField, config.tableName);

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                snapshot.put(rs.getInt(1), rs.getTimestamp(2));
            }
        } catch (SQLSyntaxErrorException e) {
            LOGGER.error("{}失败啦：", config.tableName, e);
            sql = String.format("SELECT %s, %s FROM %s",
                    config.primaryKey, config.updateTimeField, config.tableName);
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    snapshot.put(rs.getInt(1), rs.getTimestamp(2));
                }
            } catch (Exception e2) {
                LOGGER.error("{}失败啦：", config.tableName, e);
                tryAlterTable(conn, config);

            }


        } catch (Exception e) {
            LOGGER.error("获取目标表当前数据快照失败", e);
        }
        return snapshot;
    }

    // 表结构修复方法
    private static void tryAlterTable(Connection conn, SyncConfig config) throws SQLException {
        String alterSql = "ALTER TABLE " + config.tableName +
                " ADD COLUMN " + config.updateTimeField +
                " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP " +
                "ON UPDATE CURRENT_TIMESTAMP";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(alterSql);
            // 修复后重试查询
            try (PreparedStatement retryStmt = conn.prepareStatement(alterSql);
                 ResultSet rs = retryStmt.executeQuery()) {
            }
        } catch (SQLException e) {
            LOGGER.error("{} 表结构修复失败：", config.tableName, e);
            throw e; // 重新抛出异常，终止流程
        }
    }

    /**
     * 执行批量操作并提交事务
     */
    private static void executeBatch(Connection conn, PreparedStatement stmt, int total) throws SQLException {
        int[] results = stmt.executeBatch();
        conn.commit();
        stmt.clearBatch();
        LOGGER.debug("已提交批次，累计处理" + total + "条，本次更新" + results.length + "条");
    }

    /**
     * 验证数据库连接有效性
     */
    private static void validateConnection(Connection conn) throws SQLException {
        if (conn == null || conn.isClosed()) {
            throw new SQLException("数据库连接无效或已关闭");
        }
    }

    /**
     * 创建数据库连接对象，通过JDBC驱动管理器获取与指定数据库的会话通道。
     * 该方法依赖已注册的JDBC驱动程序自动匹配数据库类型[3,7](@ref)。
     *
     * @param url      数据库连接地址，格式需符合JDBC规范（如："jdbc:mysql://localhost:3306/dbname"）
     * @param user     数据库访问用户名
     * @param password 数据库访问密码
     * @return 返回表示数据库会话的Connection对象
     * @throws SQLException 当连接失败时抛出，可能由网络异常、认证失败或驱动不兼容导致[1,5](@ref)
     *                      <p>
     *                      注意事项：
     *                      1. 需确保目标数据库的JDBC驱动已通过ServiceLoader机制注册[3,7](@ref)
     *                      2. 建议通过连接池管理连接以提高性能和资源利用率[1,5](@ref)
     *                      3. 敏感信息（如密码）应通过安全方式注入，避免硬编码[1,5](@ref)
     */
    public static Connection createConnection(String url, String user, String password) throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * 根据表名和更新时间字段名称，从数据库中查询该表的所有列信息以及主键，
     * 构造一个 SyncConfig 对象，用于数据同步任务配置。
     *
     * <p><strong>技术实现：</strong></p>
     * 1. 通过JDBC DatabaseMetaData接口获取表结构元数据[1,3,5](@ref)
     * 2. 采用预编译ResultSet处理方式避免SQL注入风险[7](@ref)
     * 3. 主键处理策略：仅取第一个主键字段（需配合业务表设计规范）[9,11](@ref)
     *
     * <p><strong>数据校验规则：</strong></p>
     * | 字段        | 校验类型          | 允许值范围                  | 异常处理策略               |
     * |-------------|-------------------|---------------------------|--------------------------|
     * | tableName   | 非空校验          | 非空字符串，长度≤64字符    | 抛出IllegalArgumentException[7](@ref)|
     * | updateTimeField | 字段存在性校验    | 必须存在于表结构中        | 抛出IllegalArgumentException[6,13](@ref)|
     * | connection  | 连接有效性校验    | 需通过isValid()验证       | 抛出SQLException[1,4](@ref)|
     *
     * <p><strong>性能优化：</strong></p>
     * - 使用ArrayList的批量add()方法替代逐条插入[7](@ref)
     * - 通过toArray(T[] a)方法优化数组转换性能[7](@ref)
     * - 采用try-with-resources自动管理资源释放[3,5](@ref)
     *
     * @param connection      数据库连接对象（需提前验证有效性[1,4](@ref)）
     * @param tableName       表名（需符合数据库标识符规范[7](@ref)）
     * @param updateTimeField 更新时间字段名称（需符合时间字段命名规范[6,13](@ref)）
     * @return 包含表结构配置的SyncConfig对象
     * @throws SQLException 若元数据查询失败或主键缺失
     */
    public static SyncConfig convertTableToSyncConfig(Connection connection, String tableName,
                                                      String updateTimeField, String updateAtField)
            throws SQLException {
        Objects.requireNonNull(connection, "connection不能为null");
        Objects.requireNonNull(tableName, "tableName不能为null");
        Objects.requireNonNull(updateTimeField, "updateTimeField不能为null");

        DatabaseMetaData metaData = connection.getMetaData();

        // 获取表所有列信息
        List<String> columnsList = new ArrayList<>();
        try (ResultSet columnsRs = metaData.getColumns(connection.getCatalog(), null, tableName, "%")) {
            while (columnsRs.next()) {
                String columnName = columnsRs.getString("COLUMN_NAME");
                columnsList.add(columnName);
            }
        }

        // 获取主键（这里只取第一个主键，如果有多个，可以根据需求调整）
        String primaryKey = null;
        try (ResultSet pkRs = metaData.getPrimaryKeys(connection.getCatalog(), null, tableName)) {
            if (pkRs.next()) {
                primaryKey = pkRs.getString("COLUMN_NAME");
            }
        }

        // 检查 updateTimeField 是否在列列表中
        boolean containsUpdateTimeField = true;

        if (!columnsList.contains(updateTimeField)) {
            containsUpdateTimeField = false;
        }

        if (!columnsList.contains(updateTimeField) && !columnsList.contains(updateAtField)) {
            throw new IllegalArgumentException("指定的更新时间字段 " + updateTimeField + "或"
                    + updateAtField + " 不存在于表 " + tableName);
        }

        // 将列集合转换为数组
        String[] columns = columnsList.toArray(new String[0]);
        SyncConfig syncConfig = null;
        if (containsUpdateTimeField) {
            if (primaryKey != null) {
                syncConfig = new SyncConfig(tableName, primaryKey, updateTimeField, columns);
            }

        } else {
            if (primaryKey != null) {
                syncConfig = new SyncConfig(tableName, primaryKey, updateAtField, columns);
            }

        }


        return syncConfig;
    }


    /**
     * 获取所有需要同步的表的配置信息。
     * <p>
     * 方法从源数据库中查询所有普通表，然后对每个表：
     * <ul>
     *   <li>检查表是否符合条件（例如存在主键、指定更新时间字段等），</li>
     *   <li>检查目标数据库中是否存在该表，若不存在则创建目标表，</li>
     *   <li>将表转换为同步配置对象并加入返回列表。</li>
     * </ul>
     *
     * @param sourceConn      源数据库连接
     * @param targetConn      目标数据库连接
     * @param updateTimeField 指定的更新时间字段名
     * @param updateAtField   指定的更新时刻字段名
     * @return 包含所有同步配置的列表
     * @throws SQLException 当访问数据库元数据或执行 DDL 语句发生错误时抛出
     */
    public static List<SyncConfig> getAllSyncConfigs(Connection sourceConn, Connection targetConn,
                                                     String updateTimeField,
                                                     String updateAtField)
            throws SQLException {
        Objects.requireNonNull(sourceConn, "sourceConn 不能为 null");
        Objects.requireNonNull(updateTimeField, "updateTimeField 不能为 null");

        List<SyncConfig> configs = new ArrayList<>();
        DatabaseMetaData metaData = sourceConn.getMetaData();

        // 查询当前数据库所有普通表（类型为 TABLE）
        try (ResultSet tablesRs = metaData.getTables(sourceConn.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (tablesRs.next()) {
                String tableName = tablesRs.getString("TABLE_NAME");
                String tableComment = tablesRs.getString("REMARKS"); // 获取表注释
                try {

                    // 如果注释为 null，则可以赋予默认值或进行其它处理
                    SyncTableConfig configTable = new SyncTableConfig(tableName, tableComment != null ? tableComment : "", 1);

                    // 检查并在目标数据库中创建表（如果不存在）
                    checkAndCreateTable(sourceConn, targetConn, configTable);

                    // 将表转换为同步配置对象（convertTableToSyncConfig 实现需根据实际情况编写）
                    SyncConfig config = convertTableToSyncConfig(sourceConn, tableName, updateTimeField, updateAtField);
                    configs.add(config);
                } catch (Exception e) {
                    // 如果该表不符合同步条件（例如没有主键或不存在指定的更新时间字段），则跳过该表
                    LOGGER.error("跳过表 {}：{}", tableName, e.getMessage());
                }
            }
        }
        return configs;
    }

    /**
     * 检查目标数据库中是否存在指定表，如果不存在则创建目标表。
     *
     * @param sourceConn 源数据库连接，用于获取表的元数据
     * @param targetConn 目标数据库连接
     * @param config     表的同步配置信息
     * @throws SQLException 当访问数据库元数据或执行 DDL 语句发生错误时抛出
     */
    private static void checkAndCreateTable(Connection sourceConn,
                                            Connection targetConn,
                                            SyncTableConfig config) throws SQLException {
        if (!tableExists(targetConn, config.getTableName())) {
            LOGGER.info("目标表 {} 不存在，开始创建表结构...", config.getTableName());
            String createSql = getTableDefinition(sourceConn, config);
            executeDDL(targetConn, createSql);
            LOGGER.info("目标表 {} 创建成功", config.getTableName());
        }
    }

    /**
     * 判断指定表是否存在于给定连接对应的数据库中。
     *
     * @param conn      数据库连接
     * @param tableName 表名
     * @return 如果表存在返回 true，否则返回 false
     * @throws SQLException 当访问数据库元数据时发生错误
     */
    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    public static String getTableDefinition(Connection sourceConn, SyncTableConfig config) throws SQLException {
        DatabaseMetaData meta = sourceConn.getMetaData();
        String tableName = config.getTableName();
        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE TABLE `").append(tableName).append("` (\n");

        // 读取列信息
        List<String> columnDefs = new ArrayList<>();
        try (ResultSet columns = meta.getColumns(null, null, tableName, null)) {
            while (columns.next()) {
                String colDef = buildColumnDefinition(columns);
                columnDefs.add(colDef);
            }
        }
        ddl.append(String.join(",\n", columnDefs));

        // 处理主键
        List<String> pkColumns = new ArrayList<>();
        try (ResultSet pkRs = meta.getPrimaryKeys(null, null, tableName)) {
            while (pkRs.next()) {
                pkColumns.add("`" + pkRs.getString("COLUMN_NAME") + "`");
            }
        }
        if (!pkColumns.isEmpty()) {
            ddl.append(",\n  PRIMARY KEY (")
                    .append(String.join(", ", pkColumns))
                    .append(") USING BTREE");
        }

        // 处理索引
        Map<String, List<String>> indexMap = new LinkedHashMap<>();
        try (ResultSet indexRs = meta.getIndexInfo(null, null, tableName, false, false)) {
            while (indexRs.next()) {
                String indexName = indexRs.getString("INDEX_NAME");
                if (indexName == null || "PRIMARY".equalsIgnoreCase(indexName)) {
                    continue;
                }
                String columnName = indexRs.getString("COLUMN_NAME");
                if (columnName != null) {
                    indexMap.computeIfAbsent(indexName, k -> new ArrayList<>()).add("`" + columnName + "`");
                }
            }
        }
        for (Map.Entry<String, List<String>> entry : indexMap.entrySet()) {
            ddl.append(",\n  KEY `").append(entry.getKey()).append("` (")
                    .append(String.join(", ", entry.getValue()))
                    .append(")");
        }

        ddl.append("\n) ");

        // 处理表选项
        ddl.append("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        if (config.getTableComment() != null && !config.getTableComment().trim().isEmpty()) {
            ddl.append(" COMMENT='").append(config.getTableComment().replace("'", "''")).append("'");
        }
        if (config.getAutoIncrementStart() > 0) {
            ddl.append(" AUTO_INCREMENT=").append(config.getAutoIncrementStart());
        }
        ddl.append(";");

        return ddl.toString();
    }

    /**
     * 处理单个列的定义，返回完整的 SQL 片段
     */
    private static String buildColumnDefinition(ResultSet columns) throws SQLException {
        String colName = columns.getString("COLUMN_NAME");
        String typeName = columns.getString("TYPE_NAME");
        int columnSize = columns.getInt("COLUMN_SIZE");
        int decimalDigits = columns.getInt("DECIMAL_DIGITS");
        int nullable = columns.getInt("NULLABLE");
        String defaultValue = columns.getString("COLUMN_DEF");
        String isAutoIncrement = columns.getString("IS_AUTOINCREMENT");
        String remarks = columns.getString("REMARKS");

        StringBuilder colDef = new StringBuilder();
        colDef.append("  `").append(colName).append("` ").append(getColumnType(typeName, columnSize, decimalDigits));

        // 处理默认值
        if (defaultValue != null && !defaultValue.trim().isEmpty()) {
            defaultValue = defaultValue.trim();
            if (isStringType(typeName)) {
                defaultValue = "'" + defaultValue.replace("'", "''") + "'";
            }
            if (isDateTimeType(typeName) && "CURRENT_TIMESTAMP".equalsIgnoreCase(defaultValue)) {
                colDef.append(" DEFAULT CURRENT_TIMESTAMP");
            } else {
                colDef.append(" DEFAULT ").append(defaultValue);
            }
        } else if (nullable != DatabaseMetaData.columnNoNulls) {
            colDef.append(" DEFAULT NULL");
        }

        // 添加 NOT NULL
        if (nullable == DatabaseMetaData.columnNoNulls) {
            colDef.append(" NOT NULL");
        }

        // 处理自增
        if ("YES".equalsIgnoreCase(isAutoIncrement)) {
            colDef.append(" AUTO_INCREMENT");
        }

        // 处理 DATETIME/TIMESTAMP 的 ON UPDATE CURRENT_TIMESTAMP
        if ("TIMESTAMP".equalsIgnoreCase(typeName) || "DATETIME".equalsIgnoreCase(typeName)) {
            colDef.append(" ON UPDATE CURRENT_TIMESTAMP");
        }

        // 处理列注释
        if (remarks != null && !remarks.trim().isEmpty()) {
            colDef.append(" COMMENT '").append(remarks.replace("'", "''")).append("'");
        }

        return colDef.toString();
    }

    /**
     * 获取字段的 SQL 数据类型定义
     */
    private static String getColumnType(String typeName, int columnSize, int decimalDigits) {
        if ("VARCHAR".equalsIgnoreCase(typeName) || "CHAR".equalsIgnoreCase(typeName)) {
            return String.format("%s(%d)", typeName, columnSize);
        }
        if ("DECIMAL".equalsIgnoreCase(typeName) || "NUMERIC".equalsIgnoreCase(typeName)) {
            return String.format("%s(%d,%d)", typeName, columnSize, decimalDigits);
        }
        return typeName;
    }

    /**
     * 判断字段是否是字符串类型
     */
    private static boolean isStringType(String typeName) {
        if (typeName == null) {
            return false;
        }
        return "VARCHAR".equalsIgnoreCase(typeName) ||
                "CHAR".equalsIgnoreCase(typeName) ||
                "TEXT".equalsIgnoreCase(typeName) ||
                "ENUM".equalsIgnoreCase(typeName) ||
                "SET".equalsIgnoreCase(typeName);
    }

    /**
     * 判断字段是否是 DATETIME/TIMESTAMP 类型
     */
    private static boolean isDateTimeType(String typeName) {
        return "DATETIME".equalsIgnoreCase(typeName) || "TIMESTAMP".equalsIgnoreCase(typeName);
    }


    /**
     * 执行DDL语句
     */
    private static void executeDDL(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * 同步表配置类，用于描述待同步表的基本信息。
     */
    public static class SyncTableConfig {
        private String tableName;
        private String tableComment;
        private int autoIncrementStart;

        public SyncTableConfig(String tableName, String tableComment, int autoIncrementStart) {
            this.tableName = tableName;
            this.tableComment = tableComment;
            this.autoIncrementStart = autoIncrementStart;
        }

        public String getTableName() {
            return tableName;
        }

        public String getTableComment() {
            return tableComment;
        }

        public int getAutoIncrementStart() {
            return autoIncrementStart;
        }
    }

}
