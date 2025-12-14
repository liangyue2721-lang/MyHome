package com.make.stock.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true) // 忽略未知字段
public class StockIssueInfoTemp {

    @JsonProperty("APPLY_CODE")
    private String applyCode;

    @JsonProperty("INFO_CODE")
    private String infoCode;

    @JsonProperty("SECURITY_CODE")
    private String securityCode;

    @JsonProperty("SECURITY_NAME")
    private String securityName;

    @JsonProperty("TRADE_MARKET_CODE")
    private String tradeMarketCode;

    @JsonProperty("TRADE_MARKET")
    private String tradeMarket;

    @JsonProperty("MARKET_TYPE")
    private String marketType;

    @JsonProperty("ORG_TYPE")
    private String orgType;

    @JsonProperty("MAIN_BUSINESS")
    private String mainBusiness;

    @JsonProperty("ISSUE_NUM")
    private Long issueNum;

    @JsonProperty("ONLINE_ISSUE_NUM")
    private Long onlineIssueNum;

    @JsonProperty("OFFLINE_PLACING_NUM")
    private Long offlinePlacingNum;

    @JsonProperty("ISSUE_PRICE")
    private BigDecimal issuePrice;

    @JsonProperty("AFTER_ISSUE_PE")
    private BigDecimal afterIssuePe;

    @JsonProperty("INITIAL_MULTIPLE")
    private BigDecimal initialMultiple;

    @JsonProperty("LATELY_PRICE")
    private BigDecimal latelyPrice;

    @JsonProperty("CLOSE_PRICE")
    private BigDecimal closePrice;

    @JsonProperty("LIMIT_UP_PRICE")
    private BigDecimal limitUpPrice;

    @JsonProperty("NEWEST_PRICE")
    private String newestPrice;

    @JsonProperty("APPLY_DATE")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date applyDate;

    @JsonProperty("BALLOT_NUM_DATE")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date ballotNumDate;

    @JsonProperty("BALLOT_PAY_DATE")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date ballotPayDate;

    @JsonProperty("LISTING_DATE")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date listingDate;

    @JsonProperty("OPEN_PRICE")
    private BigDecimal openPrice;

    @JsonProperty("LD_OPEN_PREMIUM")
    private BigDecimal ldOpenPremium;

    @JsonProperty("LD_CLOSE_CHANGE")
    private BigDecimal ldCloseChange;

    @JsonProperty("TURNOVERRATE")
    private BigDecimal turnoverrate;

    @JsonProperty("LD_HIGH_CHANG")
    private BigDecimal ldHighChang;

    @JsonProperty("LD_AVERAGE_PRICE")
    private BigDecimal ldAveragePrice;

    @JsonProperty("PREDICT_ONFUND_UPPER")
    private Long predictOnfundUpper;

    @JsonProperty("PREDICT_PE_THREE")
    private BigDecimal predictPeThree;

    @JsonProperty("INDUSTRY_PE")
    private BigDecimal industryPe;

    @JsonProperty("IS_BEIJING")
    private Long isBeijing;

    @JsonProperty("IS_REGISTRATION")
    private String isRegistration;

    @JsonProperty("TOTAL_CHANGE")
    private BigDecimal totalChange;

    @JsonProperty("PROFIT")
    private Long profit;

    @JsonProperty("ONLINE_ISSUE_LWR")
    private BigDecimal onlineIssueLwr;

    // 新增缺失字段
    @JsonProperty("TOP_APPLY_MARKETCAP")
    private BigDecimal topApplyMarketcap;

    @JsonProperty("ONLINE_APPLY_UPPER")
    private Long onlineApplyUpper;

    @JsonProperty("PREDICT_ONAPPLY_UPPER")
    private Long predictOnapplyUpper;

    @JsonProperty("INDUSTRY_PE_NEW")
    private BigDecimal industryPeNew;

    @JsonProperty("OFFLINE_EP_OBJECT")
    private String offlineEpObject;

    @JsonProperty("CONTINUOUS_1WORD_NUM")
    private String continuous1wordNum;

    @JsonProperty("OPEN_DATE")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date openDate;

    @JsonProperty("OPEN_AVERAGE_PRICE")
    private BigDecimal openAveragePrice;

    @JsonProperty("PREDICT_PE")
    private BigDecimal predictPe;

    @JsonProperty("PREDICT_ISSUE_PRICE2")
    private BigDecimal predictIssuePrice2;

    @JsonProperty("PREDICT_ISSUE_PRICE")
    private BigDecimal predictIssuePrice;

    @JsonProperty("PREDICT_ISSUE_PRICE1")
    private BigDecimal predictIssuePrice1;

    @JsonProperty("PREDICT_ISSUE_PE")
    private BigDecimal predictIssuePe;

    @JsonProperty("ONLINE_APPLY_PRICE")
    private BigDecimal onlineApplyPrice;

    @JsonProperty("PAGE_PREDICT_PRICE1")
    private BigDecimal pagePredictPrice1;

    @JsonProperty("PAGE_PREDICT_PRICE2")
    private BigDecimal pagePredictPrice2;

    @JsonProperty("PAGE_PREDICT_PRICE3")
    private BigDecimal pagePredictPrice3;

    @JsonProperty("PAGE_PREDICT_PE1")
    private BigDecimal pagePredictPe1;

    @JsonProperty("PAGE_PREDICT_PE2")
    private BigDecimal pagePredictPe2;

    @JsonProperty("PAGE_PREDICT_PE3")
    private BigDecimal pagePredictPe3;

    @JsonProperty("SELECT_LISTING_DATE")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date selectListingDate;

    @JsonProperty("INDUSTRY_PE_RATIO")
    private BigDecimal industryPeRatio;

    @JsonProperty("IS_REGISTRATION_NEW")
    private String isRegistrationNew;

    public BigDecimal getTopApplyMarketcap() {
        return topApplyMarketcap;
    }

    public void setTopApplyMarketcap(BigDecimal topApplyMarketcap) {
        this.topApplyMarketcap = topApplyMarketcap;
    }

    public Long getOnlineApplyUpper() {
        return onlineApplyUpper;
    }

    public void setOnlineApplyUpper(Long onlineApplyUpper) {
        this.onlineApplyUpper = onlineApplyUpper;
    }

    public Long getPredictOnapplyUpper() {
        return predictOnapplyUpper;
    }

    public void setPredictOnapplyUpper(Long predictOnapplyUpper) {
        this.predictOnapplyUpper = predictOnapplyUpper;
    }

    public BigDecimal getIndustryPeNew() {
        return industryPeNew;
    }

    public void setIndustryPeNew(BigDecimal industryPeNew) {
        this.industryPeNew = industryPeNew;
    }

    public String getOfflineEpObject() {
        return offlineEpObject;
    }

    public void setOfflineEpObject(String offlineEpObject) {
        this.offlineEpObject = offlineEpObject;
    }

    public String getContinuous1wordNum() {
        return continuous1wordNum;
    }

    public void setContinuous1wordNum(String continuous1wordNum) {
        this.continuous1wordNum = continuous1wordNum;
    }

    public Date getOpenDate() {
        return openDate;
    }

    public void setOpenDate(Date openDate) {
        this.openDate = openDate;
    }

    public BigDecimal getOpenAveragePrice() {
        return openAveragePrice;
    }

    public void setOpenAveragePrice(BigDecimal openAveragePrice) {
        this.openAveragePrice = openAveragePrice;
    }

    public BigDecimal getPredictPe() {
        return predictPe;
    }

    public void setPredictPe(BigDecimal predictPe) {
        this.predictPe = predictPe;
    }

    public BigDecimal getPredictIssuePrice2() {
        return predictIssuePrice2;
    }

    public void setPredictIssuePrice2(BigDecimal predictIssuePrice2) {
        this.predictIssuePrice2 = predictIssuePrice2;
    }

    public BigDecimal getPredictIssuePrice() {
        return predictIssuePrice;
    }

    public void setPredictIssuePrice(BigDecimal predictIssuePrice) {
        this.predictIssuePrice = predictIssuePrice;
    }

    public BigDecimal getPredictIssuePrice1() {
        return predictIssuePrice1;
    }

    public void setPredictIssuePrice1(BigDecimal predictIssuePrice1) {
        this.predictIssuePrice1 = predictIssuePrice1;
    }

    public BigDecimal getPredictIssuePe() {
        return predictIssuePe;
    }

    public void setPredictIssuePe(BigDecimal predictIssuePe) {
        this.predictIssuePe = predictIssuePe;
    }

    public BigDecimal getOnlineApplyPrice() {
        return onlineApplyPrice;
    }

    public void setOnlineApplyPrice(BigDecimal onlineApplyPrice) {
        this.onlineApplyPrice = onlineApplyPrice;
    }

    public BigDecimal getPagePredictPrice1() {
        return pagePredictPrice1;
    }

    public void setPagePredictPrice1(BigDecimal pagePredictPrice1) {
        this.pagePredictPrice1 = pagePredictPrice1;
    }

    public BigDecimal getPagePredictPrice2() {
        return pagePredictPrice2;
    }

    public void setPagePredictPrice2(BigDecimal pagePredictPrice2) {
        this.pagePredictPrice2 = pagePredictPrice2;
    }

    public BigDecimal getPagePredictPrice3() {
        return pagePredictPrice3;
    }

    public void setPagePredictPrice3(BigDecimal pagePredictPrice3) {
        this.pagePredictPrice3 = pagePredictPrice3;
    }

    public BigDecimal getPagePredictPe1() {
        return pagePredictPe1;
    }

    public void setPagePredictPe1(BigDecimal pagePredictPe1) {
        this.pagePredictPe1 = pagePredictPe1;
    }

    public BigDecimal getPagePredictPe2() {
        return pagePredictPe2;
    }

    public void setPagePredictPe2(BigDecimal pagePredictPe2) {
        this.pagePredictPe2 = pagePredictPe2;
    }

    public BigDecimal getPagePredictPe3() {
        return pagePredictPe3;
    }

    public void setPagePredictPe3(BigDecimal pagePredictPe3) {
        this.pagePredictPe3 = pagePredictPe3;
    }

    public Date getSelectListingDate() {
        return selectListingDate;
    }

    public void setSelectListingDate(Date selectListingDate) {
        this.selectListingDate = selectListingDate;
    }

    public BigDecimal getIndustryPeRatio() {
        return industryPeRatio;
    }

    public void setIndustryPeRatio(BigDecimal industryPeRatio) {
        this.industryPeRatio = industryPeRatio;
    }

    public String getIsRegistrationNew() {
        return isRegistrationNew;
    }

    public void setIsRegistrationNew(String isRegistrationNew) {
        this.isRegistrationNew = isRegistrationNew;
    }

    public String getApplyCode() {
        return applyCode;
    }

    public void setApplyCode(String applyCode) {
        this.applyCode = applyCode;
    }

    public String getInfoCode() {
        return infoCode;
    }

    public void setInfoCode(String infoCode) {
        this.infoCode = infoCode;
    }

    public String getSecurityCode() {
        return securityCode;
    }

    public void setSecurityCode(String securityCode) {
        this.securityCode = securityCode;
    }

    public String getSecurityName() {
        return securityName;
    }

    public void setSecurityName(String securityName) {
        this.securityName = securityName;
    }

    public String getTradeMarketCode() {
        return tradeMarketCode;
    }

    public void setTradeMarketCode(String tradeMarketCode) {
        this.tradeMarketCode = tradeMarketCode;
    }

    public String getTradeMarket() {
        return tradeMarket;
    }

    public void setTradeMarket(String tradeMarket) {
        this.tradeMarket = tradeMarket;
    }

    public String getMarketType() {
        return marketType;
    }

    public void setMarketType(String marketType) {
        this.marketType = marketType;
    }

    public String getOrgType() {
        return orgType;
    }

    public void setOrgType(String orgType) {
        this.orgType = orgType;
    }

    public String getMainBusiness() {
        return mainBusiness;
    }

    public void setMainBusiness(String mainBusiness) {
        this.mainBusiness = mainBusiness;
    }

    public Long getIssueNum() {
        return issueNum;
    }

    public void setIssueNum(Long issueNum) {
        this.issueNum = issueNum;
    }

    public Long getOnlineIssueNum() {
        return onlineIssueNum;
    }

    public void setOnlineIssueNum(Long onlineIssueNum) {
        this.onlineIssueNum = onlineIssueNum;
    }

    public Long getOfflinePlacingNum() {
        return offlinePlacingNum;
    }

    public void setOfflinePlacingNum(Long offlinePlacingNum) {
        this.offlinePlacingNum = offlinePlacingNum;
    }

    public BigDecimal getIssuePrice() {
        return issuePrice;
    }

    public void setIssuePrice(BigDecimal issuePrice) {
        this.issuePrice = issuePrice;
    }

    public BigDecimal getAfterIssuePe() {
        return afterIssuePe;
    }

    public void setAfterIssuePe(BigDecimal afterIssuePe) {
        this.afterIssuePe = afterIssuePe;
    }

    public BigDecimal getInitialMultiple() {
        return initialMultiple;
    }

    public void setInitialMultiple(BigDecimal initialMultiple) {
        this.initialMultiple = initialMultiple;
    }

    public BigDecimal getLatelyPrice() {
        return latelyPrice;
    }

    public void setLatelyPrice(BigDecimal latelyPrice) {
        this.latelyPrice = latelyPrice;
    }

    public BigDecimal getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(BigDecimal closePrice) {
        this.closePrice = closePrice;
    }

    public BigDecimal getLimitUpPrice() {
        return limitUpPrice;
    }

    public void setLimitUpPrice(BigDecimal limitUpPrice) {
        this.limitUpPrice = limitUpPrice;
    }

    public String getNewestPrice() {
        return newestPrice;
    }

    public void setNewestPrice(String newestPrice) {
        this.newestPrice = newestPrice;
    }

    public Date getApplyDate() {
        return applyDate;
    }

    public void setApplyDate(Date applyDate) {
        this.applyDate = applyDate;
    }

    public Date getBallotNumDate() {
        return ballotNumDate;
    }

    public void setBallotNumDate(Date ballotNumDate) {
        this.ballotNumDate = ballotNumDate;
    }

    public Date getBallotPayDate() {
        return ballotPayDate;
    }

    public void setBallotPayDate(Date ballotPayDate) {
        this.ballotPayDate = ballotPayDate;
    }

    public Date getListingDate() {
        return listingDate;
    }

    public void setListingDate(Date listingDate) {
        this.listingDate = listingDate;
    }

    public BigDecimal getOpenPrice() {
        return openPrice;
    }

    public void setOpenPrice(BigDecimal openPrice) {
        this.openPrice = openPrice;
    }

    public BigDecimal getLdOpenPremium() {
        return ldOpenPremium;
    }

    public void setLdOpenPremium(BigDecimal ldOpenPremium) {
        this.ldOpenPremium = ldOpenPremium;
    }

    public BigDecimal getLdCloseChange() {
        return ldCloseChange;
    }

    public void setLdCloseChange(BigDecimal ldCloseChange) {
        this.ldCloseChange = ldCloseChange;
    }

    public BigDecimal getTurnoverrate() {
        return turnoverrate;
    }

    public void setTurnoverrate(BigDecimal turnoverrate) {
        this.turnoverrate = turnoverrate;
    }

    public BigDecimal getLdHighChang() {
        return ldHighChang;
    }

    public void setLdHighChang(BigDecimal ldHighChang) {
        this.ldHighChang = ldHighChang;
    }

    public BigDecimal getLdAveragePrice() {
        return ldAveragePrice;
    }

    public void setLdAveragePrice(BigDecimal ldAveragePrice) {
        this.ldAveragePrice = ldAveragePrice;
    }

    public Long getPredictOnfundUpper() {
        return predictOnfundUpper;
    }

    public void setPredictOnfundUpper(Long predictOnfundUpper) {
        this.predictOnfundUpper = predictOnfundUpper;
    }

    public BigDecimal getPredictPeThree() {
        return predictPeThree;
    }

    public void setPredictPeThree(BigDecimal predictPeThree) {
        this.predictPeThree = predictPeThree;
    }

    public BigDecimal getIndustryPe() {
        return industryPe;
    }

    public void setIndustryPe(BigDecimal industryPe) {
        this.industryPe = industryPe;
    }

    public Long getIsBeijing() {
        return isBeijing;
    }

    public void setIsBeijing(Long isBeijing) {
        this.isBeijing = isBeijing;
    }

    public String getIsRegistration() {
        return isRegistration;
    }

    public void setIsRegistration(String isRegistration) {
        this.isRegistration = isRegistration;
    }

    public BigDecimal getTotalChange() {
        return totalChange;
    }

    public void setTotalChange(BigDecimal totalChange) {
        this.totalChange = totalChange;
    }

    public Long getProfit() {
        return profit;
    }

    public void setProfit(Long profit) {
        this.profit = profit;
    }

    public BigDecimal getOnlineIssueLwr() {
        return onlineIssueLwr;
    }

    public void setOnlineIssueLwr(BigDecimal onlineIssueLwr) {
        this.onlineIssueLwr = onlineIssueLwr;
    }
}
