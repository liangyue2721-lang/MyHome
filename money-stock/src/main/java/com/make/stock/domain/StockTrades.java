package com.make.stock.domain;

import java.math.BigDecimal;

import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 股票利润对象 stock_trades
 *
 * @author erqi
 * @date 2025-07-29
 */
public class StockTrades extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Excel(name = "主键ID")
    private Long id;

    /**
     * 股票代码（交易所唯一标识）
     */
    @Excel(name = "股票代码")
    private String stockCode;

    /**
     * 股票名称（上市公司简称）
     */
    @Excel(name = "股票名称")
    private String stockName;

    /**
     * 初始买入股数（单位：手）
     */
    @Excel(name = "初次买入数量")
    private Long initialShares;

    /**
     * 初始买入价格（单位：元）
     */
    @Excel(name = "买入价位")
    private BigDecimal buyPrice;

    /**
     * 目标价位
     */
    @Excel(name = "目标价位")
    private BigDecimal sellTargetPrice;

    /**
     * 目标利润
     */
    @Excel(name = "目标利润")
    private BigDecimal targetNetProfit;

    /**
     * 卖出价位
     */
    @Excel(name = "卖出价位")
    private BigDecimal sellPrice;

    /**
     * 追加价位1
     */
    @Excel(name = "追加价位1")
    private BigDecimal additionalPrice1;

    /**
     * 追加数量1
     */
    @Excel(name = "追加数量1")
    private Long additionalShares1;

    /**
     * 追加价位2
     */
    @Excel(name = "追加价位2")
    private BigDecimal additionalPrice2;

    /**
     * 追加数量2
     */
    @Excel(name = "追加数量2")
    private Long additionalShares2;

    /**
     * 追加价位3
     */
    @Excel(name = "追加价位3")
    private BigDecimal additionalPrice3;

    /**
     * 追加数量3
     */
    @Excel(name = "追加数量3")
    private Long additionalShares3;

    /**
     * 总成本
     */
    @Excel(name = "总成本")
    private BigDecimal totalCost;

    /**
     * 净利润
     */
    @Excel(name = "净利润")
    private BigDecimal netProfit;

    /**
     * 卖出状态（0:未卖出, 1:已卖出）
     */
    @Excel(name = "是否卖出")
    private Integer isSell;

    /**
     * 是否同步
     */
    @Excel(name = "是否同步")
    private Long syncStatus;

    /**
     * API接口
     */
    @Excel(name = "stockApi")
    private String stockApi;


    /**
     * 用户 ID，关联用户表
     */
    @Excel(name = "用户 ID，关联用户表")
    private Long userId;


    public StockTrades setSellTargetPrice(BigDecimal sellTargetPrice) {
        this.sellTargetPrice = sellTargetPrice;
        return this;
    }



    public StockTrades setTargetNetProfit(BigDecimal targetNetProfit) {
        this.targetNetProfit = targetNetProfit;
        return this;
    }


    /**
     * 设置主键ID（支持链式调用）
     *
     * @param id 主键ID
     * @return 当前对象实例
     */
    public StockTrades setId(Long id) {
        this.id = id;
        return this;
    }

    /**
     * 获取股票代码
     *
     * @return 股票代码
     */
    public String getStockCode() {
        return stockCode;
    }

    /**
     * 设置股票代码（支持链式调用）
     *
     * @param stockCode 股票代码
     * @return 当前对象实例
     */
    public StockTrades setStockCode(String stockCode) {
        this.stockCode = stockCode;
        return this;
    }

    // 其他字段的 getter 和链式 setter 方法（格式与上述相同）

    public String getStockName() {
        return stockName;
    }

    public StockTrades setStockName(String stockName) {
        this.stockName = stockName;
        return this;
    }

    public Long getInitialShares() {
        return initialShares;
    }

    public StockTrades setInitialShares(Long initialShares) {
        this.initialShares = initialShares;
        return this;
    }

    public BigDecimal getBuyPrice() {
        return buyPrice;
    }

    public StockTrades setBuyPrice(BigDecimal buyPrice) {
        this.buyPrice = buyPrice;
        return this;
    }

    public BigDecimal getSellPrice() {
        return sellPrice;
    }

    public StockTrades setSellPrice(BigDecimal sellPrice) {
        this.sellPrice = sellPrice;
        return this;
    }

    // 追加操作相关方法
    public BigDecimal getAdditionalPrice1() {
        return additionalPrice1;
    }

    public StockTrades setAdditionalPrice1(BigDecimal additionalPrice1) {
        this.additionalPrice1 = additionalPrice1;
        return this;
    }

    public Long getAdditionalShares1() {
        return additionalShares1;
    }

    public StockTrades setAdditionalShares1(Long additionalShares1) {
        this.additionalShares1 = additionalShares1;
        return this;
    }

    // 其他追加操作字段的链式方法...

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public StockTrades setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
        return this;
    }

    public BigDecimal getNetProfit() {
        return netProfit;
    }

    public StockTrades setNetProfit(BigDecimal netProfit) {
        this.netProfit = netProfit;
        return this;
    }

    public Integer getIsSell() {
        return isSell;
    }

    /**
     * 设置卖出状态（支持链式调用）
     *
     * @param isSell 卖出状态 (0:未卖出, 1:已卖出)
     * @return 当前对象实例
     */
    public StockTrades setIsSell(Integer isSell) {
        this.isSell = isSell;
        return this;
    }

    public Long getSyncStatus() {
        return syncStatus;
    }

    public StockTrades setSyncStatus(Long syncStatus) {
        this.syncStatus = syncStatus;
        return this;
    }

    public String getStockApi() {
        return stockApi;
    }

    public void setStockApi(String stockApi) {
        this.stockApi = stockApi;
    }

    public BigDecimal getAdditionalPrice2() {
        return additionalPrice2;
    }

    public void setAdditionalPrice2(BigDecimal additionalPrice2) {
        this.additionalPrice2 = additionalPrice2;
    }

    public Long getAdditionalShares2() {
        return additionalShares2;
    }

    public void setAdditionalShares2(Long additionalShares2) {
        this.additionalShares2 = additionalShares2;
    }

    public BigDecimal getAdditionalPrice3() {
        return additionalPrice3;
    }

    public void setAdditionalPrice3(BigDecimal additionalPrice3) {
        this.additionalPrice3 = additionalPrice3;
    }

    public Long getAdditionalShares3() {
        return additionalShares3;
    }

    public void setAdditionalShares3(Long additionalShares3) {
        this.additionalShares3 = additionalShares3;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getSellTargetPrice() {
        return sellTargetPrice;
    }

    public BigDecimal getTargetNetProfit() {
        return targetNetProfit;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 重写toString方法（使用Apache Commons Lang3实现）
     *
     * @return 对象字符串表示
     */
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("stockCode", getStockCode())
                .append("stockName", getStockName())
                .append("initialShares", getInitialShares())
                .append("buyPrice", getBuyPrice())
                .append("sellPrice", getSellPrice())
                .append("additionalPrice1", getAdditionalPrice1())
                .append("additionalShares1", getAdditionalShares1())
                .append("additionalPrice2", getAdditionalPrice2())
                .append("additionalShares2", getAdditionalShares2())
                .append("additionalPrice3", getAdditionalPrice3())
                .append("additionalShares3", getAdditionalShares3())
                .append("totalCost", getTotalCost())
                .append("netProfit", getNetProfit())
                .append("isSell", getIsSell())
                .append("syncStatus", getSyncStatus())
                .append("createTime", getCreateTime())
                .append("updateTime", getUpdateTime())
                .append("stockApi", getStockApi())
                .toString();
    }
}
