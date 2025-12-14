package com.make.web.service.impl;

import com.make.finance.domain.AssetRecord;
import com.make.finance.mapper.AssetRecordMapper;
import com.make.stock.domain.StockTrades;
import com.make.stock.mapper.StockTradesMapper;
import com.make.web.service.ISyncService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 数据同步服务实现类
 *
 * @author your-name
 */
@Service
public class SyncServiceImpl implements ISyncService {

    @Resource
    private AssetRecordMapper assetRecordMapper;


    @Resource
    private StockTradesMapper stockTradesMapper;



    /**
     * <p>同步股票交易净利润至资产记录，并将已同步的交易标记为已处理。</p>
     *
     * <h3>实现思路</h3>
     * <ol>
     *     <li><b>查询</b>：一次性查询出所有 <code>sync_status = 0</code> 的交易。</li>
     *     <li><b>汇总</b>：使用 Java 8 Stream 对 <code>net_profit</code> 做求和，忽略 <code>null</code>。</li>
     *     <li><b>更新资产</b>：若有资产记录且汇总利润不为 0，则在同一事务内更新金额。</li>
     *     <li><b>批量更新交易状态</b>：统一把这些交易的 <code>sync_status</code> 设为 1，减少数据库往返。</li>
     * </ol>
     *
     * <p><b>注意</b>：方法已加上 <code>@Transactional</code>，确保四步操作要么全部成功、要么全部回滚。</p>
     *
     * @return 同步的资产记录信息
     */
    @Transactional(rollbackFor = Exception.class) // 事务保证原子性
    @Override
    public AssetRecord syncStockTrades() {
        // 1. 查询所有待同步的交易记录（sync_status = 0）
        List<StockTrades> unsyncedTrades =
                stockTradesMapper.selectStockTradesList(new StockTrades().setSyncStatus(0L).setIsSell(1));

        // 若已全部同步，则直接结束
        if (unsyncedTrades == null || unsyncedTrades.isEmpty()) {
            return null;
        }

        // 2. 计算总净利润，Stream API 自动过滤 null
        BigDecimal totalRevenue = unsyncedTrades.stream()
                .map(StockTrades::getNetProfit)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. 更新资产记录（此处写死 asset_id = 3，可提取为参数）
        AssetRecord assetRecord = assetRecordMapper.selectAssetRecordByAssetId(3L);
        if (assetRecord != null && totalRevenue.compareTo(BigDecimal.ZERO) != 0) {
            // 当前金额可能为空，使用 Optional 兜底
            BigDecimal currentAmount = Optional.ofNullable(assetRecord.getAmount())
                    .orElse(BigDecimal.ZERO);
            assetRecord.setAmount(currentAmount.add(totalRevenue));
            assetRecordMapper.updateAssetRecord(assetRecord); // 单次写入
            // 4. 批量把这些交易标记为已同步，减少 N 次 update
            unsyncedTrades.forEach(t -> t.setSyncStatus(1L));
            stockTradesMapper.updateStockTradesBatch(unsyncedTrades); // 需要在 Mapper 层实现批量 SQL
        }

        return assetRecord;
    }

}