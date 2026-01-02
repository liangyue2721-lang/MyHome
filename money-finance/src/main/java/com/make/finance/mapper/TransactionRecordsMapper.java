package com.make.finance.mapper;

import java.util.List;
import java.util.Map;

import com.make.finance.domain.TransactionRecords;
import org.apache.ibatis.annotations.Param;

/**
 * 微信支付宝流水Mapper接口
 *
 * @author è´°æ
 * @date 2025-05-27
 */
public interface TransactionRecordsMapper {
    /**
     * 查询微信支付宝流水
     *
     * @param id 微信支付宝流水主键
     * @return 微信支付宝流水
     */
    public TransactionRecords selectTransactionRecordsById(Long id);

    /**
     * 查询微信支付宝流水列表
     *
     * @param transactionRecords 微信支付宝流水
     * @return 微信支付宝流水集合
     */
    public List<TransactionRecords> selectTransactionRecordsList(TransactionRecords transactionRecords);

    /**
     * 新增微信支付宝流水
     *
     * @param transactionRecords 微信支付宝流水
     * @return 结果
     */
    public int insertTransactionRecords(TransactionRecords transactionRecords);

    /**
     * 修改微信支付宝流水
     *
     * @param transactionRecords 微信支付宝流水
     * @return 结果
     */
    public int updateTransactionRecords(TransactionRecords transactionRecords);

    /**
     * 删除微信支付宝流水
     *
     * @param id 微信支付宝流水主键
     * @return 结果
     */
    public int deleteTransactionRecordsById(Long id);

    /**
     * 批量删除微信支付宝流水
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteTransactionRecordsByIds(Long[] ids);

    /**
     * 获取交易类型分布饼图数据
     *
     * @return 包含交易类型及其占比的列表，每个Map包含：
     * <ul>
     *     <li><b>typeName</b>：交易类型名称（如"餐饮"、"交通"）</li>
     *     <li><b>amountRatio</b>：该类型金额占总金额的比例（0-1之间的Double值）</li>
     *     <li><b>transactionCount</b>：该类型交易笔数（可选字段）</li>
     * </ul>
     * @apiNote 数据格式示例：[{"typeName":"餐饮","amountRatio":0.25,"transactionCount":15}]
     */
    List<Map<String, Object>> selectTransactionTypePieChartData(@Param("userId") Long userId);

    /**
     * 获取总金额分类占比饼图数据
     *
     * @return 包含金额分类及占比的列表，每个Map包含：
     * <ul>
     *     <li><b>category</b>：金额分类（如"收入"、"支出"）</li>
     *     <li><b>totalAmount</b>：该分类总金额（BigDecimal或Double类型）</li>
     *     <li><b>percentage</b>：分类金额占比（百分比数值）</li>
     * </ul>
     * @throws NullPointerException 如果查询结果为空
     */
    List<Map<String, Object>> selectTotalAmountPieChartData(@Param("userId") Long userId);

    /**
     * 获取总金额趋势图表数据（支持折线图/柱状图）
     *
     * @return 包含时间维度和金额的列表，每个Map包含：
     * <ul>
     *     <li><b>timePeriod</b>：时间周期（如"2025-01"、"Q2"）</li>
     *     <li><b>totalAmount</b>：周期内总金额（数值类型）</li>
     *     <li><b>compareRate</b>：与上一周期对比增长率（可选字段）</li>
     * </ul>
     * @since 1.2.0 新增时间粒度筛选功能
     */
    List<Map<String, Object>> getTotalAmountChartData(@Param("userId") Long userId);

    /**
     * 获取总金额多维度对比柱状图数据
     *
     * @return 包含分类维度及金额的列表，每个Map包含：
     * <ul>
     *     <li><b>category</b>：分类名称（如"产品A"、"产品B"）</li>
     *     <li><b>amount</b>：分类总金额（数值类型）</li>
     *     <li><b>colorCode</b>：柱状图颜色标识（可选，用于前端渲染）</li>
     * </ul>
     * @example 返回数据示例：[{"category":"线上","amount":15000.00,"colorCode":"#FF6B6B"}]
     */
    List<Map<String, Object>> selectTotalAmountColumnChartData(@Param("userId") Long userId);

    /**
     * 批量插入交易记录
     */
    public int insertBatch(List<TransactionRecords> records);
}
