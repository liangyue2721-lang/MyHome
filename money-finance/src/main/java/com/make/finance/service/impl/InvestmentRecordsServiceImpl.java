package com.make.finance.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.make.common.utils.DateUtils;
import com.make.finance.mapper.InvestmentRecordsMapper;
import com.make.finance.domain.InvestmentRecords;
import com.make.finance.service.IInvestmentRecordsService;

/**
 * 投资利润回报记录Service业务层处理
 *
 * @author make
 * @date 2025-08-23
 */
@Service
public class InvestmentRecordsServiceImpl implements IInvestmentRecordsService
{
    @Autowired
    private InvestmentRecordsMapper investmentRecordsMapper;

    /**
     * 查询投资利润回报记录
     *
     * @param id 投资利润回报记录主键
     * @return 投资利润回报记录
     */
    @Override
    public InvestmentRecords selectInvestmentRecordsById(Long id)
    {
        return investmentRecordsMapper.selectInvestmentRecordsById(id);
    }

    /**
     * 查询投资利润回报记录列表
     *
     * @param investmentRecords 投资利润回报记录
     * @return 投资利润回报记录
     */
    @Override
    public List<InvestmentRecords> selectInvestmentRecordsList(InvestmentRecords investmentRecords)
    {
        return investmentRecordsMapper.selectInvestmentRecordsList(investmentRecords);
    }

    /**
     * 新增投资利润回报记录
     *
     * @param investmentRecords 投资利润回报记录
     * @return 结果
     */
    @Override
    public int insertInvestmentRecords(InvestmentRecords investmentRecords)
    {
        investmentRecords.setCreateTime(DateUtils.getNowDate());

        // 检查userId是否设置
        if (investmentRecords.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

//        // 如果没有手动设置成交金额，则根据成交价位和成交量计算
//        if (investmentRecords.getTradeAmount() == null &&
//            investmentRecords.getTradePrice() != null &&
//            investmentRecords.getTradeVolume() != null) {
//            investmentRecords.setTradeAmount(
//                investmentRecords.getTradePrice()
//                    .multiply(new BigDecimal(investmentRecords.getTradeVolume()))
//                    .setScale(2, RoundingMode.HALF_UP)
//            );
//        }
//
//        // 如果没有手动设置本期收益，则根据当期总资产和本金计算
//        if (investmentRecords.getPeriodProfit() == null) {
//            investmentRecords.setPeriodProfit(
//                calculatePeriodProfit(investmentRecords.getTotalAsset(), investmentRecords.getPrincipalAmount())
//            );
//        }
//
//        // 如果没有手动设置收益率，则根据当期总资产和本金计算
//        if (investmentRecords.getProfitRate() == null) {
//            investmentRecords.setProfitRate(
//                calculateProfitRate(investmentRecords.getTotalAsset(), investmentRecords.getPrincipalAmount())
//            );
//        }
//
//        // 如果没有手动设置目标达成率，则根据当期总资产计算
//        if (investmentRecords.getTargetProgress() == null) {
//            investmentRecords.setTargetProgress(
//                calculateTargetProgress(investmentRecords.getTotalAsset())
//            );
//        }
//
        return investmentRecordsMapper.insertInvestmentRecords(investmentRecords);
    }

    /**
     * 修改投资利润回报记录
     *
     * @param investmentRecords 投资利润回报记录
     * @return 结果
     */
    @Override
    public int updateInvestmentRecords(InvestmentRecords investmentRecords)
    {
        investmentRecords.setUpdateTime(DateUtils.getNowDate());

        // 检查userId是否设置
        if (investmentRecords.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

//        // 如果没有手动设置成交金额，则根据成交价位和成交量计算
//        if (investmentRecords.getTradeAmount() == null &&
//            investmentRecords.getTradePrice() != null &&
//            investmentRecords.getTradeVolume() != null) {
//            investmentRecords.setTradeAmount(
//                investmentRecords.getTradePrice()
//                    .multiply(new BigDecimal(investmentRecords.getTradeVolume()))
//                    .setScale(2, RoundingMode.HALF_UP)
//            );
//        }
//
//        // 如果没有手动设置本期收益，则根据当期总资产和本金计算
//        if (investmentRecords.getPeriodProfit() == null) {
//            investmentRecords.setPeriodProfit(
//                calculatePeriodProfit(investmentRecords.getTotalAsset(), investmentRecords.getPrincipalAmount())
//            );
//        }
//
//        // 如果没有手动设置收益率，则根据当期总资产和本金计算
//        if (investmentRecords.getProfitRate() == null) {
//            investmentRecords.setProfitRate(
//                calculateProfitRate(investmentRecords.getTotalAsset(), investmentRecords.getPrincipalAmount())
//            );
//        }
//
//        // 如果没有手动设置目标达成率，则根据当期总资产计算
//        if (investmentRecords.getTargetProgress() == null) {
//            investmentRecords.setTargetProgress(
//                calculateTargetProgress(investmentRecords.getTotalAsset())
//            );
//        }

        return investmentRecordsMapper.updateInvestmentRecords(investmentRecords);
    }

    /**
     * 批量删除投资利润回报记录
     *
     * @param ids 需要删除的投资利润回报记录主键
     * @return 结果
     */
    @Override
    public int deleteInvestmentRecordsByIds(Long[] ids)
    {
        return investmentRecordsMapper.deleteInvestmentRecordsByIds(ids);
    }

    /**
     * 删除投资利润回报记录信息
     *
     * @param id 投资利润回报记录主键
     * @return 结果
     */
    @Override
    public int deleteInvestmentRecordsById(Long id)
    {
        return investmentRecordsMapper.deleteInvestmentRecordsById(id);
    }

    private static final BigDecimal TARGET_AMOUNT = new BigDecimal("1000000");

    /**
     * 计算目标达成率（总资产 / 100 万）* 100
     *
     * @param totalAsset 当前总资产（本金 + 收益）
     * @return 目标达成率（百分比），保留两位小数
     */
    public static BigDecimal calculateTargetProgress(BigDecimal totalAsset) {
        if (totalAsset == null || totalAsset.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return totalAsset
                .divide(TARGET_AMOUNT, 6, RoundingMode.HALF_UP) // 保留中间值精度
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP); // 最终两位小数
    }

    /**
     * 计算当前收益率（针对本期投入）
     *
     * @param totalAsset      当前总资产（含收益）
     * @param principalAmount 本期投入本金
     * @return 当前收益率（百分比），保留两位小数；本金为0时返回0
     */
    public static BigDecimal calculateProfitRate(BigDecimal totalAsset, BigDecimal principalAmount) {
        if (principalAmount == null || principalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (totalAsset == null) {
            totalAsset = BigDecimal.ZERO;
        }

        return totalAsset.subtract(principalAmount)
                .divide(principalAmount, 6, RoundingMode.HALF_UP) // 保留中间计算精度
                .multiply(new BigDecimal("100")) // 转为百分比
                .setScale(2, RoundingMode.HALF_UP); // 保留两位小数
    }

    /**
     * 计算本期收益金额（总资产 - 本期本金）
     *
     * @param totalAsset      当前总资产（含收益）
     * @param principalAmount 本期投入本金
     * @return 本期收益金额（可以为负数），保留两位小数
     */
    public static BigDecimal calculatePeriodProfit(BigDecimal totalAsset, BigDecimal principalAmount) {
        if (totalAsset == null) {
            totalAsset = BigDecimal.ZERO;
        }
        if (principalAmount == null) {
            principalAmount = BigDecimal.ZERO;
        }

        return totalAsset.subtract(principalAmount)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
