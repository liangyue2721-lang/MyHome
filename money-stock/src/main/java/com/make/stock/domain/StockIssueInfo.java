package com.make.stock.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import com.make.stock.domain.dto.StockIssueInfoTemp;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 新股发行信息对象 stock_issue_info
 *
 * @author erqi
 * @date 2025-05-28
 */
public class StockIssueInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 申购代码
     */
    private String applyCode;

    /**
     * 信息编号
     */
    @Excel(name = "信息编号")
    private String infoCode;

    /**
     * 证券代码
     */
    @Excel(name = "证券代码")
    private String securityCode;

    /**
     * 证券名称
     */
    @Excel(name = "证券名称")
    private String securityName;

    /**
     * 交易市场代码
     */
    @Excel(name = "交易市场代码")
    private String tradeMarketCode;

    /**
     * 交易市场
     */
    @Excel(name = "交易市场")
    private String tradeMarket;

    /**
     * 市场类型
     */
    @Excel(name = "市场类型")
    private String marketType;

    /**
     * 机构类型
     */
    @Excel(name = "机构类型")
    private String orgType;

    /**
     * 主营业务
     */
    @Excel(name = "主营业务")
    private String mainBusiness;

    /**
     * 发行数量(万股)
     */
    @Excel(name = "发行数量(万股)")
    private Long issueNum;

    /**
     * 网上发行数量
     */
    @Excel(name = "网上发行数量")
    private Long onlineIssueNum;

    /**
     * 网下配售数量
     */
    @Excel(name = "网下配售数量")
    private Long offlinePlacingNum;

    /**
     * 发行价格
     */
    @Excel(name = "发行价格")
    private BigDecimal issuePrice;

    /**
     * 发行后市盈率
     */
    @Excel(name = "发行后市盈率")
    private BigDecimal afterIssuePe;

    /**
     * 初始倍数
     */
    @Excel(name = "初始倍数")
    private BigDecimal initialMultiple;

    /**
     * 最新价格
     */
    @Excel(name = "最新价格")
    private BigDecimal latelyPrice;

    /**
     * 收盘价格
     */
    @Excel(name = "收盘价格")
    private BigDecimal closePrice;

    /**
     * 涨停价格
     */
    @Excel(name = "涨停价格")
    private BigDecimal limitUpPrice;

    /**
     * 最新价格（冗余字段）
     */
    @Excel(name = "最新价格", readConverterExp = "冗=余字段")
    private BigDecimal newestPrice;

    /**
     * 申购日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "申购日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date applyDate;

    /**
     * 中签号公布日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "中签号公布日", width = 30, dateFormat = "yyyy-MM-dd")
    private Date ballotNumDate;

    /**
     * 中签缴款日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "中签缴款日", width = 30, dateFormat = "yyyy-MM-dd")
    private Date ballotPayDate;

    /**
     * 上市日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "上市日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date listingDate;

    /**
     * 开盘价格
     */
    @Excel(name = "开盘价格")
    private BigDecimal openPrice;

    /**
     * 最新开盘溢价率(%)
     */
    @Excel(name = "最新开盘溢价率(%)")
    private BigDecimal ldOpenPremium;

    /**
     * 最新收盘涨幅(%)
     */
    @Excel(name = "最新收盘涨幅(%)")
    private BigDecimal ldCloseChange;

    /**
     * 换手率(%)
     */
    @Excel(name = "换手率(%)")
    private BigDecimal turnoverrate;

    /**
     * 最新最高涨幅(%)
     */
    @Excel(name = "最新最高涨幅(%)")
    private BigDecimal ldHighChang;

    /**
     * 最新平均价格
     */
    @Excel(name = "最新平均价格")
    private BigDecimal ldAveragePrice;

    /**
     * 预测网上顶格申购需配市值(万)
     */
    @Excel(name = "预测网上顶格申购需配市值(万)")
    private Long predictOnfundUpper;

    /**
     * 预测pe值（三年）
     */
    @Excel(name = "预测pe值", readConverterExp = "三=年")
    private BigDecimal predictPeThree;

    /**
     * 行业市盈率
     */
    @Excel(name = "行业市盈率")
    private BigDecimal industryPe;

    /**
     * 是否北京企业 0=否 1=是
     */
    @Excel(name = "是否北京企业 0=否 1=是")
    private Long isBeijing;

    /**
     * 是否注册制
     */
    @Excel(name = "是否注册制")
    private String isRegistration;

    /**
     * 总变化量
     */
    @Excel(name = "总变化量")
    private BigDecimal totalChange;

    /**
     * 利润
     */
    @Excel(name = "利润")
    private Long profit;

    /**
     * 网上发行中签率
     */
    @Excel(name = "网上发行中签率")
    private BigDecimal onlineIssueLwr;

    /**
     * 预测顶格申购需配市值
     */
    @Excel(name = "预测顶格申购需配市值")
    private BigDecimal topApplyMarketcap;

    /**
     * 网上申购上限
     */
    @Excel(name = "网上申购上限")
    private Long onlineApplyUpper;

    /**
     * 预测申购上限
     */
    @Excel(name = "预测申购上限")
    private Long predictOnapplyUpper;

    /**
     * 行业新市盈率
     */
    @Excel(name = "行业新市盈率")
    private BigDecimal industryPeNew;

    /**
     * 网下发行对象
     */
    @Excel(name = "网下发行对象")
    private String offlineEpObject;

    /**
     * 连续一字涨停天数
     */
    @Excel(name = "连续一字涨停天数")
    private String continuous1wordNum;

    public StockIssueInfo() {
    }

    public StockIssueInfo(StockIssueInfoTemp temp) {
        this.applyCode = temp.getApplyCode();
        this.infoCode = temp.getInfoCode();
        this.securityCode = temp.getSecurityCode();
        this.securityName = temp.getSecurityName();
        this.tradeMarketCode = temp.getTradeMarketCode();
        this.tradeMarket = temp.getTradeMarket();
        this.marketType = temp.getMarketType();
        this.orgType = temp.getOrgType();
        this.mainBusiness = temp.getMainBusiness();
        this.issueNum = temp.getIssueNum();
        this.onlineIssueNum = temp.getOnlineIssueNum();
        this.offlinePlacingNum = temp.getOfflinePlacingNum();
        this.issuePrice = temp.getIssuePrice();
        this.afterIssuePe = temp.getAfterIssuePe();
        this.initialMultiple = temp.getInitialMultiple();
        this.latelyPrice = temp.getLatelyPrice();
        this.closePrice = temp.getClosePrice();
        this.limitUpPrice = temp.getLimitUpPrice();
        if (null == temp.getNewestPrice() || temp.getNewestPrice().equals("-") || temp.getNewestPrice().isEmpty()) {
            this.newestPrice = new BigDecimal(0);
        } else {
            this.newestPrice = new BigDecimal(temp.getNewestPrice());
        }
        this.applyDate = temp.getApplyDate();
        this.ballotNumDate = temp.getBallotNumDate();
        this.ballotPayDate = temp.getBallotPayDate();
        this.listingDate = temp.getListingDate();
        this.openPrice = temp.getOpenPrice();
        this.ldOpenPremium = temp.getLdOpenPremium();
        this.ldCloseChange = temp.getLdCloseChange();
        this.turnoverrate = temp.getTurnoverrate();
        this.ldHighChang = temp.getLdHighChang();
        this.ldAveragePrice = temp.getLdAveragePrice();
        this.predictOnfundUpper = temp.getPredictOnfundUpper();
        this.predictPeThree = temp.getPredictPeThree();
        this.industryPe = temp.getIndustryPe();
        this.isBeijing = temp.getIsBeijing();
        this.isRegistration = temp.getIsRegistration();
        this.totalChange = temp.getTotalChange();
        this.profit = temp.getProfit();
        this.onlineIssueLwr = temp.getOnlineIssueLwr();
        this.topApplyMarketcap = temp.getTopApplyMarketcap();
        this.onlineApplyUpper = temp.getOnlineApplyUpper();
        this.predictOnapplyUpper = temp.getPredictOnapplyUpper();
        this.industryPeNew = temp.getIndustryPeNew();
        this.offlineEpObject = temp.getOfflineEpObject();
        this.continuous1wordNum = temp.getContinuous1wordNum();
    }

    public void setApplyCode(String applyCode) {
        this.applyCode = applyCode;
    }

    public String getApplyCode() {
        return applyCode;
    }

    public void setInfoCode(String infoCode) {
        this.infoCode = infoCode;
    }

    public String getInfoCode() {
        return infoCode;
    }

    public void setSecurityCode(String securityCode) {
        this.securityCode = securityCode;
    }

    public String getSecurityCode() {
        return securityCode;
    }

    public void setSecurityName(String securityName) {
        this.securityName = securityName;
    }

    public String getSecurityName() {
        return securityName;
    }

    public void setTradeMarketCode(String tradeMarketCode) {
        this.tradeMarketCode = tradeMarketCode;
    }

    public String getTradeMarketCode() {
        return tradeMarketCode;
    }

    public void setTradeMarket(String tradeMarket) {
        this.tradeMarket = tradeMarket;
    }

    public String getTradeMarket() {
        return tradeMarket;
    }

    public void setMarketType(String marketType) {
        this.marketType = marketType;
    }

    public String getMarketType() {
        return marketType;
    }

    public void setOrgType(String orgType) {
        this.orgType = orgType;
    }

    public String getOrgType() {
        return orgType;
    }

    public void setMainBusiness(String mainBusiness) {
        this.mainBusiness = mainBusiness;
    }

    public String getMainBusiness() {
        return mainBusiness;
    }

    public void setIssueNum(Long issueNum) {
        this.issueNum = issueNum;
    }

    public Long getIssueNum() {
        return issueNum;
    }

    public void setOnlineIssueNum(Long onlineIssueNum) {
        this.onlineIssueNum = onlineIssueNum;
    }

    public Long getOnlineIssueNum() {
        return onlineIssueNum;
    }

    public void setOfflinePlacingNum(Long offlinePlacingNum) {
        this.offlinePlacingNum = offlinePlacingNum;
    }

    public Long getOfflinePlacingNum() {
        return offlinePlacingNum;
    }

    public void setIssuePrice(BigDecimal issuePrice) {
        this.issuePrice = issuePrice;
    }

    public BigDecimal getIssuePrice() {
        return issuePrice;
    }

    public void setAfterIssuePe(BigDecimal afterIssuePe) {
        this.afterIssuePe = afterIssuePe;
    }

    public BigDecimal getAfterIssuePe() {
        return afterIssuePe;
    }

    public void setInitialMultiple(BigDecimal initialMultiple) {
        this.initialMultiple = initialMultiple;
    }

    public BigDecimal getInitialMultiple() {
        return initialMultiple;
    }

    public void setLatelyPrice(BigDecimal latelyPrice) {
        this.latelyPrice = latelyPrice;
    }

    public BigDecimal getLatelyPrice() {
        return latelyPrice;
    }

    public void setClosePrice(BigDecimal closePrice) {
        this.closePrice = closePrice;
    }

    public BigDecimal getClosePrice() {
        return closePrice;
    }

    public void setLimitUpPrice(BigDecimal limitUpPrice) {
        this.limitUpPrice = limitUpPrice;
    }

    public BigDecimal getLimitUpPrice() {
        return limitUpPrice;
    }

    public void setNewestPrice(BigDecimal newestPrice) {
        this.newestPrice = newestPrice;
    }

    public BigDecimal getNewestPrice() {
        return newestPrice;
    }

    public void setApplyDate(Date applyDate) {
        this.applyDate = applyDate;
    }

    public Date getApplyDate() {
        return applyDate;
    }

    public void setBallotNumDate(Date ballotNumDate) {
        this.ballotNumDate = ballotNumDate;
    }

    public Date getBallotNumDate() {
        return ballotNumDate;
    }

    public void setBallotPayDate(Date ballotPayDate) {
        this.ballotPayDate = ballotPayDate;
    }

    public Date getBallotPayDate() {
        return ballotPayDate;
    }

    public void setListingDate(Date listingDate) {
        this.listingDate = listingDate;
    }

    public Date getListingDate() {
        return listingDate;
    }

    public void setOpenPrice(BigDecimal openPrice) {
        this.openPrice = openPrice;
    }

    public BigDecimal getOpenPrice() {
        return openPrice;
    }

    public void setLdOpenPremium(BigDecimal ldOpenPremium) {
        this.ldOpenPremium = ldOpenPremium;
    }

    public BigDecimal getLdOpenPremium() {
        return ldOpenPremium;
    }

    public void setLdCloseChange(BigDecimal ldCloseChange) {
        this.ldCloseChange = ldCloseChange;
    }

    public BigDecimal getLdCloseChange() {
        return ldCloseChange;
    }

    public void setTurnoverrate(BigDecimal turnoverrate) {
        this.turnoverrate = turnoverrate;
    }

    public BigDecimal getTurnoverrate() {
        return turnoverrate;
    }

    public void setLdHighChang(BigDecimal ldHighChang) {
        this.ldHighChang = ldHighChang;
    }

    public BigDecimal getLdHighChang() {
        return ldHighChang;
    }

    public void setLdAveragePrice(BigDecimal ldAveragePrice) {
        this.ldAveragePrice = ldAveragePrice;
    }

    public BigDecimal getLdAveragePrice() {
        return ldAveragePrice;
    }

    public void setPredictOnfundUpper(Long predictOnfundUpper) {
        this.predictOnfundUpper = predictOnfundUpper;
    }

    public Long getPredictOnfundUpper() {
        return predictOnfundUpper;
    }

    public void setPredictPeThree(BigDecimal predictPeThree) {
        this.predictPeThree = predictPeThree;
    }

    public BigDecimal getPredictPeThree() {
        return predictPeThree;
    }

    public void setIndustryPe(BigDecimal industryPe) {
        this.industryPe = industryPe;
    }

    public BigDecimal getIndustryPe() {
        return industryPe;
    }

    public void setIsBeijing(Long isBeijing) {
        this.isBeijing = isBeijing;
    }

    public Long getIsBeijing() {
        return isBeijing;
    }

    public void setIsRegistration(String isRegistration) {
        this.isRegistration = isRegistration;
    }

    public String getIsRegistration() {
        return isRegistration;
    }

    public void setTotalChange(BigDecimal totalChange) {
        this.totalChange = totalChange;
    }

    public BigDecimal getTotalChange() {
        return totalChange;
    }

    public void setProfit(Long profit) {
        this.profit = profit;
    }

    public Long getProfit() {
        return profit;
    }

    public void setOnlineIssueLwr(BigDecimal onlineIssueLwr) {
        this.onlineIssueLwr = onlineIssueLwr;
    }

    public BigDecimal getOnlineIssueLwr() {
        return onlineIssueLwr;
    }

    public void setTopApplyMarketcap(BigDecimal topApplyMarketcap) {
        this.topApplyMarketcap = topApplyMarketcap;
    }

    public BigDecimal getTopApplyMarketcap() {
        return topApplyMarketcap;
    }

    public void setOnlineApplyUpper(Long onlineApplyUpper) {
        this.onlineApplyUpper = onlineApplyUpper;
    }

    public Long getOnlineApplyUpper() {
        return onlineApplyUpper;
    }

    public void setPredictOnapplyUpper(Long predictOnapplyUpper) {
        this.predictOnapplyUpper = predictOnapplyUpper;
    }

    public Long getPredictOnapplyUpper() {
        return predictOnapplyUpper;
    }

    public void setIndustryPeNew(BigDecimal industryPeNew) {
        this.industryPeNew = industryPeNew;
    }

    public BigDecimal getIndustryPeNew() {
        return industryPeNew;
    }

    public void setOfflineEpObject(String offlineEpObject) {
        this.offlineEpObject = offlineEpObject;
    }

    public String getOfflineEpObject() {
        return offlineEpObject;
    }

    public void setContinuous1wordNum(String continuous1wordNum) {
        this.continuous1wordNum = continuous1wordNum;
    }

    public String getContinuous1wordNum() {
        return continuous1wordNum;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("applyCode", getApplyCode())
                .append("infoCode", getInfoCode())
                .append("securityCode", getSecurityCode())
                .append("securityName", getSecurityName())
                .append("tradeMarketCode", getTradeMarketCode())
                .append("tradeMarket", getTradeMarket())
                .append("marketType", getMarketType())
                .append("orgType", getOrgType())
                .append("mainBusiness", getMainBusiness())
                .append("issueNum", getIssueNum())
                .append("onlineIssueNum", getOnlineIssueNum())
                .append("offlinePlacingNum", getOfflinePlacingNum())
                .append("issuePrice", getIssuePrice())
                .append("afterIssuePe", getAfterIssuePe())
                .append("initialMultiple", getInitialMultiple())
                .append("latelyPrice", getLatelyPrice())
                .append("closePrice", getClosePrice())
                .append("limitUpPrice", getLimitUpPrice())
                .append("newestPrice", getNewestPrice())
                .append("applyDate", getApplyDate())
                .append("ballotNumDate", getBallotNumDate())
                .append("ballotPayDate", getBallotPayDate())
                .append("listingDate", getListingDate())
                .append("openPrice", getOpenPrice())
                .append("ldOpenPremium", getLdOpenPremium())
                .append("ldCloseChange", getLdCloseChange())
                .append("turnoverrate", getTurnoverrate())
                .append("ldHighChang", getLdHighChang())
                .append("ldAveragePrice", getLdAveragePrice())
                .append("predictOnfundUpper", getPredictOnfundUpper())
                .append("predictPeThree", getPredictPeThree())
                .append("industryPe", getIndustryPe())
                .append("isBeijing", getIsBeijing())
                .append("isRegistration", getIsRegistration())
                .append("totalChange", getTotalChange())
                .append("profit", getProfit())
                .append("onlineIssueLwr", getOnlineIssueLwr())
                .append("topApplyMarketcap", getTopApplyMarketcap())
                .append("onlineApplyUpper", getOnlineApplyUpper())
                .append("predictOnapplyUpper", getPredictOnapplyUpper())
                .append("industryPeNew", getIndustryPeNew())
                .append("offlineEpObject", getOfflineEpObject())
                .append("continuous1wordNum", getContinuous1wordNum())
                .append("createTime", getCreateTime())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
