package com.make.quartz.service.impl;

import com.make.stock.domain.StockKline;
import com.make.stock.service.IStockKlineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;

/**
 * 股票K线数据仓储服务
 * <p>
 * 封装K线数据的持久化操作，为上层业务逻辑提供统一的数据访问接口，
 * 减少主逻辑对底层服务的直接调用，提高代码的可维护性和可测试性。
 * </p>
 */
@Component
public class StockKlineRepositoryService {

    private static final Logger log = LoggerFactory.getLogger(StockKlineRepositoryService.class);

    /**
     * 股票K线服务接口，用于执行具体的数据库操作
     */
    @Resource
    private IStockKlineService stockKlineService;

    /**
     * 检查指定股票和日期的K线数据是否存在
     * <p>
     * 通过股票代码和交易日期查询数据库，判断对应的K线数据是否已经存在。
     * </p>
     *
     * @param stockCode 股票代码
     * @param tradeDate 交易日期
     * @return 如果存在返回true，否则返回false
     */
    public boolean existsByStockAndDate(String stockCode, java.util.Date tradeDate) {
        try {
            // 调用底层服务接口检查数据是否存在
            return stockKlineService.existsByStockAndDate(stockCode, tradeDate);
        } catch (Exception e) {
            // 发生异常时记录错误日志并返回false
            log.error("existsByStockAndDate 调用失败：{}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 插入单条股票K线数据
     * <p>
     * 将指定的K线数据插入到数据库中。
     * </p>
     *
     * @param kline K线数据对象
     */
    public void insertStockKline(StockKline kline) {
        // 调用底层服务接口执行插入操作
        stockKlineService.insertStockKline(kline);
    }

    /**
     * 根据股票代码和交易日期更新K线数据
     * <p>
     * 更新指定股票代码和交易日期的K线数据。
     * </p>
     *
     * @param kline K线数据对象
     */
    public void updateByStockCodeAndTradeDate(StockKline kline) {
        // 调用底层服务接口执行更新操作
        stockKlineService.updateByStockCodeAndTradeDate(kline);
    }

    /**
     * 批量插入或更新股票K线数据
     * <p>
     * 对传入的K线数据列表进行批量插入或更新操作，提高数据处理效率。
     * </p>
     *
     * @param list K线数据列表
     */
    public void insertOrUpdateBatch(List<StockKline> list) {
        // 参数校验：如果列表为空则直接返回
        if (list == null || list.isEmpty()) return;
        try {
            // 调用底层服务接口执行批量操作
            stockKlineService.insertOrUpdateBatch(list);
        } catch (Exception e) {
            // 发生异常时记录错误日志并抛出运行时异常
            log.error("批量入库失败，数量：{}，原因：{}", list.size(), e.getMessage(), e);
            throw new RuntimeException("批量入库失败", e);
        }
    }

    public void batchUpdateByStockCodeAndTradeDate(List<StockKline> updateList) {
        stockKlineService.batchUpdateByStockCodeAndTradeDate(updateList);
    }

    public List<LocalDate> selectExistsDates(String stockCode, List<LocalDate> tradeDateList) {
        return stockKlineService.selectExistsDates(stockCode, tradeDateList);
    }
}