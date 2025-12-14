package com.make.stock.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.make.common.utils.DateUtils;
import com.make.stock.domain.SalesData;
import com.make.stock.service.ISalesDataService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.stock.mapper.StockTradesMapper;
import com.make.stock.domain.StockTrades;
import com.make.stock.service.IStockTradesService;

import javax.annotation.Resource;

/**
 * 股票利润Service业务层处理
 *
 * @author erqi
 * @date 2025-05-28
 */
@Service
public class StockTradesServiceImpl implements IStockTradesService {

    // 日志记录器
    private static final Logger log = LoggerFactory.getLogger(StockTradesServiceImpl.class);

    @Autowired
    private StockTradesMapper stockTradesMapper;

    @Resource
    private ISalesDataService salesDataService; // 折线图

    /**
     * 日期格式化器：确保 yyyy-MM-dd，不包含时分秒
     * DateTimeFormatter 是线程安全的，不用额外同步
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 查询股票利润
     *
     * @param id 股票利润主键
     * @return 股票利润
     */
    @Override
    public StockTrades selectStockTradesById(Long id) {
        return stockTradesMapper.selectStockTradesById(id);
    }

    /**
     * 查询股票利润列表
     *
     * @param stockTrades 股票利润
     * @return 股票利润
     */
    @Override
    public List<StockTrades> selectStockTradesList(StockTrades stockTrades) {
        return stockTradesMapper.selectStockTradesList(stockTrades);
    }

    /**
     * 新增股票利润
     *
     * @param stockTrades 股票利润
     * @return 结果
     */
    @Override
    public int insertStockTrades(StockTrades stockTrades) {
        stockTrades.setCreateTime(DateUtils.getNowDate());
        stockTrades.setSyncStatus(0L);
        return stockTradesMapper.insertStockTrades(stockTrades);
    }

    /**
     * 修改股票利润
     *
     * @param stockTrades 股票利润
     * @return 结果
     */
    @Override
    public int updateStockTrades(StockTrades stockTrades) {
        stockTrades.setUpdateTime(DateUtils.getNowDate());
        return stockTradesMapper.updateStockTrades(stockTrades);
    }

    /**
     * 批量删除股票利润
     *
     * @param ids 需要删除的股票利润主键
     * @return 结果
     */
    @Override
    public int deleteStockTradesByIds(Long[] ids) {
        return stockTradesMapper.deleteStockTradesByIds(ids);
    }

    /**
     * 删除股票利润信息
     *
     * @param id 股票利润主键
     * @return 结果
     */
    @Override
    public int deleteStockTradesById(Long id) {
        return stockTradesMapper.deleteStockTradesById(id);
    }

    /**
     * 根据名称修改股票最新利润
     *
     * @param stockTrades 股票最新数据
     * @return 结果
     */
    @Override
    public int updateStockTradesByCode(StockTrades stockTrades) {
        return stockTradesMapper.updateStockTradesByCode(stockTrades);
    }

    /**
     * 查询股票信息
     *
     * @param stockTrades 股票信息
     * @return 股票信息集合
     */
    @Override
    public List<StockTrades> selectStockTradesOne(StockTrades stockTrades) {
        return stockTradesMapper.selectStockTradesOne(stockTrades);
    }

    /**
     * 查询当日所有交易记录，累加净利润，并写入或更新 SalesData 表中的当天数据。
     *
     * @throws RuntimeException 如果在日期转换或数据库操作中发生不可恢复错误
     */
    public void queryStockProfitData() {
        // —— 1. 查询所有交易记录 ——
        List<StockTrades> trades = stockTradesMapper.selectStockTradesList(new StockTrades());
        if (CollectionUtils.isEmpty(trades)) {
            log.info("【StockProfitService】未查询到任何交易记录，跳过当天净利润统计");
            return;
        }

        // —— 2. 累加净利润 ——
        BigDecimal totalProfit = trades.stream()
                .map(StockTrades::getNetProfit)    // 提取每笔交易的净利润
                .filter(Objects::nonNull)         // 过滤掉 null 值
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // —— 3. 构造当日记录 ——
        // 3.1 获取本地日期（不含时分秒）
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        // 3.2 将 LocalDate 转为 java.util.Date，且时分秒均为 00:00:00
        Date recordDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());

        SalesData salesData = new SalesData();
        salesData.setRecordDate(recordDate);

        // —— 4. 写入或更新 SalesData ——
        List<SalesData> existing = salesDataService.selectSalesDataList(salesData);
        salesData.setProfit(totalProfit);
        if (CollectionUtils.isNotEmpty(existing)) {
            salesData.setId(existing.get(0).getId());
            salesDataService.updateSalesData(salesData);
            log.info("【StockProfitService】已更新 {} 的净利润，金额：{}", today.format(DATE_FORMATTER), totalProfit);
        } else {
            salesDataService.insertSalesData(salesData);
            log.info("【StockProfitService】已插入 {} 的净利润，金额：{}", today.format(DATE_FORMATTER), totalProfit);
        }

    }
}
