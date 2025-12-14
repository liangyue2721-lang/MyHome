package com.make.stock.service;

import java.util.List;

import com.make.stock.domain.StockIssueInfo;

/**
 * 新股发行信息Service接口
 *
 * @author erqi
 * @date 2025-05-28
 */
public interface IStockIssueInfoService {

    /**
     * 查询新股发行信息
     *
     * @param applyCode 新股发行信息主键
     * @return 新股发行信息
     */
    public StockIssueInfo selectStockIssueInfoByApplyCode(String applyCode);

    /**
     * 查询新股发行信息列表
     *
     * @param stockIssueInfo 新股发行信息
     * @return 新股发行信息集合
     */
    public List<StockIssueInfo> selectStockIssueInfoList(StockIssueInfo stockIssueInfo);

    /**
     * 新增新股发行信息
     *
     * @param stockIssueInfo 新股发行信息
     * @return 结果
     */
    public int insertStockIssueInfo(StockIssueInfo stockIssueInfo);

    /**
     * 修改新股发行信息
     *
     * @param stockIssueInfo 新股发行信息
     * @return 结果
     */
    public int updateStockIssueInfo(StockIssueInfo stockIssueInfo);

    /**
     * 批量删除新股发行信息
     *
     * @param applyCodes 需要删除的新股发行信息主键集合
     * @return 结果
     */
    public int deleteStockIssueInfoByApplyCodes(String[] applyCodes);

    /**
     * 删除新股发行信息信息
     *
     * @param applyCode 新股发行信息主键
     * @return 结果
     */
    public int deleteStockIssueInfoByApplyCode(String applyCode);

    /**
     * 根据申请代码查询股票发行信息关联代码
     *
     * <p>通过业务申请编号获取对应的股票发行代码，适用于以下场景：
     * <ul>
     *   <li>{@linkplain #queryStockIssueInfoCode(String) 核心业务系统对接}</li>
     * </ul>
     *
     * @param applyCode 业务申请代码（格式：{@code YYYYMMDD-XXXX}），例如{@code 20250529-0001}
     */
    String queryStockIssueInfoCode(String applyCode);

}
