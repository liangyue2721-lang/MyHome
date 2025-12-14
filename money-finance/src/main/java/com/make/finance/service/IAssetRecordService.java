package com.make.finance.service;

import java.util.List;
import com.make.finance.domain.AssetRecord;
import com.make.finance.domain.vo.LabelEntity;

/**
 * 个人资产明细Service接口
 * 
 * @author 贰柒
 * @date 2025-05-28
 */
public interface IAssetRecordService 
{
    /**
     * 查询个人资产明细
     * 
     * @param assetId 个人资产明细主键
     * @return 个人资产明细
     */
    public AssetRecord selectAssetRecordByAssetId(Long assetId);

    /**
     * 查询个人资产明细列表
     * 
     * @param assetRecord 个人资产明细
     * @return 个人资产明细集合
     */
    public List<AssetRecord> selectAssetRecordList(AssetRecord assetRecord);

    /**
     * 新增个人资产明细
     * 
     * @param assetRecord 个人资产明细
     * @return 结果
     */
    public int insertAssetRecord(AssetRecord assetRecord);

    /**
     * 修改个人资产明细
     * 
     * @param assetRecord 个人资产明细
     * @return 结果
     */
    public int updateAssetRecord(AssetRecord assetRecord);

    /**
     * 批量删除个人资产明细
     * 
     * @param assetIds 需要删除的个人资产明细主键集合
     * @return 结果
     */
    public int deleteAssetRecordByAssetIds(Long[] assetIds);

    /**
     * 删除个人资产明细信息
     * 
     * @param assetId 个人资产明细主键
     * @return 结果
     */
    public int deleteAssetRecordByAssetId(Long assetId);

    /**
     * 获取系统支持的资产类型列表
     *
     * @return 包含资产类型信息的列表，每个{@link LabelEntity}包含：
     *         <ul>
     *             <li><b>code</b>：类型编码（如"STOCK"、"BOND"）</li>
     *             <li><b>name</b>：类型名称（如"股票"、"债券"）</li>
     *             <li><b>category</b>：所属大类（如"金融"、"实物"）</li>
     *         </ul>
     * @apiNote 数据示例：[{"code":"STOCK","name":"股票","category":"金融"}]
     * @since 1.1.0 新增"数字货币"分类
     */
    List<LabelEntity> getAssetType();

    /**
     * 获取资产记录柱状图数据
     *
     * @return 包含时间维度和金额的列表，每个{@link AssetRecord}包含：
     *         <ul>
     *             <li><b>timePeriod</b>：时间周期（如"2025-Q1"、"2024-12"）</li>
     *             <li><b>totalAmount</b>：周期内总金额（BigDecimal类型）</li>
     *             <li><b>compareRate</b>：与上一周期对比增长率（百分比数值）</li>
     *             <li><b>currency</b>：货币单位（如"CNY"、"USD"）</li>
     *         </ul>
     * @since 2.0.0 新增多币种支持
     */
    List<AssetRecord> getRecordColumnChart(Long userId);

    /**
     * 获取资产状态分类信息
     *
     * @return 状态列表，每个{@link LabelEntity}包含：
     *         <ul>
     *             <li><b>statusCode</b>：状态代码（如"ACTIVE"、"FROZEN"）</li>
     *             <li><b>statusDesc</b>：状态描述（如"正常交易"、"冻结中"）</li>
     *             <li><b>priority</b>：优先级（数值越小优先级越高）</li>
     *         </ul>
     * @example 返回数据示例：[{"statusCode":"ACTIVE","statusDesc":"正常交易","priority":1}]
     * @throws IllegalStateException 如果状态数据未初始化
     */
    List<LabelEntity> getAssetStatus();


}
