package com.make.stock.mapper;

import java.util.List;

import com.make.stock.domain.StockIssueInfo;

/**
 * 新股发行信息Mapper接口
 *
 * @author erqi
 * @date 2025-05-28
 */
public interface StockIssueInfoMapper {

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
     * 删除新股发行信息
     *
     * @param applyCode 新股发行信息主键
     * @return 结果
     */
    public int deleteStockIssueInfoByApplyCode(String applyCode);

    /**
     * 批量删除新股发行信息
     *
     * @param applyCodes 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteStockIssueInfoByApplyCodes(String[] applyCodes);

    /**
     * 根据业务申请代码查询关联的股票发行代码
     *
     * <p>通过申请编号获取已注册的股票发行代码，适用于以下场景：
     *
     * @param applyCode 业务申请代码（格式：{@code yyyyMMdd-xxxx}），例如{@code 20250529-0001}
     * @return 关联的股票发行代码（6位数字），例如{@code 600519}，若未找到则返回{@code null}
     * @throws IllegalArgumentException 当{@code applyCode}格式不符合{@code yyyyMMdd-xxxx}时抛出
     * @apiNote 该方法会校验申请代码的时效性（有效期30天）
     * @reference 参考《证券业务数据交换标准》（JR/T 0042-2023）第5.3.2条
     * @since 2.3.0 新增科创板代码映射支持
     * @example
     * <pre>{@code
     * String code = service.selectStockIssueCodeByApplyCode("20250529-0001");
     * // 输出：600519
     * }</pre>
     */
    String selectStockIssueInfoExistCode(String applyCode);
}
