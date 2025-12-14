package com.make.quartz.service;

import com.make.finance.domain.dto.CCBCreditCardTransactionEmail;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * 实时股票服务接口
 *
 * <p>核心功能：
 * <ol>
 *   <li>实时股票数据采集与处理；</li>
 *   <li>内存数据与持久化存储同步；</li>
 *   <li>各种定时批处理任务（串行执行）。</li>
 * </ol>
 */
public interface IRealTimeService {

    /**
     * 刷新并同步最新的新股信息。
     *
     * <p>该方法从远程接口获取最新新股列表并转换成实体列表：<br>
     * - 若数据库不存在该新股（根据申购代码判断），则执行插入，并发送通知邮件；<br>
     * - 若数据库已存在且信息发生变化，则执行更新；<br>
     * - 如遇异常仅记录日志，不影响后续流程。
     */
    void refreshNewStockInformation();

    /**
     * 执行财富数据库全量备份（批量处理）。
     *
     * <p>处理流程：<br>
     * 1. 全量查询股票基础数据并分批处理（每批处理固定数量）；<br>
     * 2. 将当前数据转换为历史记录格式并批量插入历史表；<br>
     * 3. 使用事务控制保证操作的一致性；<br>
     * 4. 捕获并记录执行过程中的异常信息。
     *
     * <p>该方法为同步执行，在执行前会尝试获取全局锁，若锁已被占用则直接返回。
     */
    void wealthDBDataBak();

    /**
     * 更新股票交易利润数据。
     *
     * <p>处理流程：<br>
     * 1. 查询所有未完成的卖出交易；<br>
     * 2. 对每条交易，获取当前市场价格（或 ETF 收盘价）并重新计算净利润；<br>
     * 3. 更新数据库中对应的交易记录。
     *
     * <p>如远程价格接口调用失败会抛出 IOException，其他异常在内部捕获并记录日志。
     *
     * @throws IOException 如果调用外部接口获取价格失败
     */
    void updateStockProfitData() throws IOException;

    /**
     * 查询当天所有交易记录并更新用户净利润。
     *
     * <p>处理流程：<br>
     * 1. 查询当日所有交易记录；<br>
     * 2. 按用户分组累加净利润；<br>
     * 3. 对每个用户，根据当日记录写入或更新 SalesData 表；<br>
     * 4. 触发年度投资汇总数据的更新（每个用户单独处理）。
     *
     * <p>该方法为同步执行，对所有用户依次处理，各用户间相互独立。异常在内部记录日志，不会抛出。
     */
    void queryStockProfitData();

    /**
     * 更新 ETF 数据。
     *
     * <p>处理流程：<br>
     * 1. 查询所有 ETF 基础数据；<br>
     * 2. 对每个 ETF，调用外部接口获取最新价格并更新记录；<br>
     * 3. 异常在内部捕获并记录日志，继续处理下一条数据。
     *
     * <p>如果调用外部接口失败会抛出 IOException。
     *
     * @throws IOException 如果调用外部接口获取ETF价格失败
     */
    void updateEtfData() throws IOException;

//    /**
//     * 数据库同步任务。
//     *
//     * <p>该方法通过 JDBC 连接源库和目标库，将选定表按更新时间字段进行数据同步（增量复制）。
//     * 如果发生异常，会在内部记录日志并结束。
//     */
//    void databaseSync();

    /**
     * 记录系统线程池状态。
     *
     * <p>该方法用于日志记录当前所有线程池的状态信息，方便系统监控。
     * 任何异常在内部捕获并记录日志。
     */
    void logAllThreadPoolStatus();

    /**
     * 查询并处理当日上市公司信息。
     *
     * <p>处理流程：<br>
     * 1. 查询指定日期的上市新股信息；<br>
     * 2. 遍历每只新股，检查是否已有上市公告记录；<br>
     * 3. 如果是首次通知或通知次数未达上限，则插入或更新上市公告并发送通知邮件；<br>
     * 4. 使用 Redis 缓存标记已通知的股票，避免重复通知。
     *
     * @param midnight 当日零点时间（上市日期）
     */
    void queryListingStatusColumn(Date midnight);

    /**
     * 更新关注股票的利润数据。
     *
     * <p>该方法查询所有关注股票，并依次处理每只股票：调用 processWatchStockToPython(Watchstock)
     * 计算并更新该股票的利润数据。异常会在任务内部捕获并记录日志，不影响其他股票。
     */
    void updateWatchStockProfitData();

    /**
     * 保存工商银行信用卡交易记录。
     *
     * <p>处理流程：<br>
     * 1. 解析每条交易明细的交易日期和入账日期；<br>
     * 2. 映射其他字段及金额，并调用 Service 插入记录；<br>
     * 3. 所有记录按顺序处理，异常在内部捕获并记录日志。
     *
     * @param emailList 从邮件解析出的交易记录列表
     */
    void saveCCBCreditCardTransaction(List<CCBCreditCardTransactionEmail> emailList);

    /**
     * 更新当年用户资产总额（年度存款汇总）。
     *
     * <p>处理流程：<br>
     * 1. 获取当前年份；<br>
     * 2. 查询所有资产记录，并按用户分组累加各自的资产总额；<br>
     * 3. 对每个用户的总额，更新或插入 AnnualDepositSummary 表中的当年汇总记录。
     *
     * <p>如果查询到的资产记录列表为空，会跳过处理。
     */
    void updateDepositAmount();

    /**
     * 更新工商银行存款金额并同步消费记录。
     *
     * <p>处理流程：<br>
     * 1. 根据 loanRepaymentId 查询当月还款信息；<br>
     * 2. 查询指定 assetId 对应的资产记录；<br>
     * 3. 在事务中扣减资产金额并更新资产记录；<br>
     * 4. 查找是否已有对应的消费记录：如有则更新金额，否则插入新记录。
     *
     * <p>如未找到还款或资产记录，会抛出 NoSuchElementException；如余额不足，则抛出 IllegalStateException。
     *
     * @param loanRepaymentId 借款还款记录 ID
     * @param assetId         资产记录 ID
     * @throws IllegalStateException  如果资产余额不足
     */
    void updateICBCDepositAmount(Long loanRepaymentId, Long assetId);

    /**
     * 每日股票数据归档任务。
     *
     * <p>功能：将 Redis 中缓存的实时股票数据同步到数据库，完成当日股票年度表现数据归档。主要流程：<br>
     * 1. 从 Redis 获取全部股票代码；<br>
     * 2. 查询数据库中已有的年度表现记录；<br>
     * 3. 遍历每只股票：<br>
     *    - 从缓存获取实时数据并解析成实体；<br>
     *    - 若数据库已有记录，则更新（包括年初重置最低价逻辑）；<br>
     *    - 若数据库无记录，则插入新纪录；<br>
     * 4. 计算相关统计并记录耗时日志。
     *
     * <p>该方法为同步阻塞执行，每只股票的异常在内部捕获并记录，不影响其他股票处理。
     */
    void archiveDailyStockData();

    /**
     * 更新关注股票的周低、周高、年低、年高价格。
     *
     * <p>处理流程：<br>
     * 1. 查询所有关注股票；<br>
     * 2. 对每只股票分别查询最近一周和今年的 K 线数据；<br>
     * 3. 计算并更新其周低/周高、年低/年高；<br>
     * 4. 批量更新所有关注股票的记录。
     *
     * <p>异常在内部捕获并记录，不影响其他股票的更新。
     */
    void updateWatchStockYearLow();

    /**
     * 更新关注的美股行情数据。
     *
     * <p>处理流程：<br>
     * 1. 定义需要监控的美股代码列表；<br>
     * 2. 查询数据库中已有记录；<br>
     * 3. 判断当前时间是否处于午夜时间窗口，用于重置周低价和年低价；<br>
     * 4. 遍历每只股票：调用外部数据获取工具获取今日行情，并构建更新实体；<br>
     *    - 如果记录已存在，则根据情况更新最低价和当前价；<br>
     *    - 如果记录不存在，则插入新记录并初始化最低价；<br>
     * 5. 统计处理成功和失败的股票数量并记录日志。
     *
     * <p>单个股票的处理异常会记录日志，不影响其他股票的更新。
     */
    void updateWatchStockUs();

    /**
     * 执行股票 K 线数据更新任务。
     *
     * <p>处理流程：<br>
     * 1. 为每只股票创建并执行任务，更新其 K 线数据；<br>
     * 2. 每个任务内部处理可能抛出异常，但会在任务中记录日志，不影响其他任务；<br>
     * 3. 所有任务完成后方法结束。
     *
     * @param nodeId 节点ID，用于区分不同节点的任务
     * @throws RuntimeException 当执行过程中出现非预期异常时抛出
     */
    void updateStockPriceTaskRunning(int nodeId);
}
