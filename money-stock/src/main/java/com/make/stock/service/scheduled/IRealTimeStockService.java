package com.make.stock.service.scheduled;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author 84522
 */
public interface IRealTimeStockService {

    /**
     * 执行实时查询股票达到5天或30天最低任务
     */
    Map<String, List<String>> getDynamicThreshold();

    /**
     * 刷新内存映射表数据
     * <p>
     * 该方法用于实时更新内存中Map结构存储的列表数据信息，适用于维护内存数据与外部变化的同步。
     * Map的键对应数据标识，值为关联的数据列表。通常与定时调度器配合实现周期更新。
     * </p>
     * <p>
     * 示例：
     * <pre>
     * {@code
     * realTimeService.refreshInMemoryMapEntries();
     * }
     * </pre>
     * </p>
     *
     * @throws RuntimeException 当数据更新过程中发生格式异常或并发冲突时抛出
     */
    void refreshInMemoryMapEntries();

    /**
     * 批量同步股票数据到数据库
     *
     * @implNote 实现逻辑：
     * 1. 分批次处理数据（每批1000条）
     * 2. 先更新后插入的幂等性保障
     * 3. 事务级数据一致性控制
     * 4. 详细的执行日志和异常追踪
     * @优势： - 批量操作提升写入性能（5-10倍）
     * - 通过IDEMPOTENT设计保证数据完整性
     * - 支持断点续传功能
     * @事务管理： - 使用Spring事务注解控制事务边界
     * - 异常时自动回滚未提交数据
     * - 事务超时时间配置（默认30秒）
     */
    void batchSyncStockDataToDB();

    /**
     * 刷新内存中的财富数据映射
     *
     * @implNote 实现逻辑：
     * 1. 清空过期缓存条目
     * 2. 加载热点股票数据
     * 3. 构建股票代码到实体对象的映射关系
     * 4. 启动预热机制提升查询响应速度
     * @优势： - 减少数据库查询压力
     * - 提高数据访问效率（10-100倍提升）
     * - 支持动态热点数据加载
     * @缓存策略： - LRU缓存淘汰算法
     * - 最大缓存容量限制（可配置）
     * - 数据有效性校验机制
     */
    void refreshWealthInMemoryMapEntries();

    void batchSyncStockDataToDB2();

    void getHistoryDataStock() throws IOException;

    void getHistoryDataStockNewDay() throws IOException;
}
