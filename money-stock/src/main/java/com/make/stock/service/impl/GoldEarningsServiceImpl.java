package com.make.stock.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.make.stock.domain.GoldProductPrice;
import com.make.stock.enums.Institution;
import com.make.stock.mapper.GoldProductPriceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.stock.mapper.GoldEarningsMapper;
import com.make.stock.domain.GoldEarnings;
import com.make.stock.service.IGoldEarningsService;

/**
 * 攒金收益记录Service业务层处理
 *
 * @author erqi
 * @date 2025-05-28
 */
@Service
public class GoldEarningsServiceImpl implements IGoldEarningsService {

    @Autowired
    private GoldEarningsMapper goldEarningsMapper;

    @Autowired
    private GoldProductPriceMapper goldProductPriceMapper;


    /**
     * 查询攒金收益记录
     *
     * @param id 攒金收益记录主键
     * @return 攒金收益记录
     */
    @Override
    public GoldEarnings selectGoldEarningsById(Long id) {
        return goldEarningsMapper.selectGoldEarningsById(id);
    }

    /**
     * 查询攒金收益记录列表
     *
     * @param goldEarnings 攒金收益记录
     * @return 攒金收益记录
     */
    @Override
    public List<GoldEarnings> selectGoldEarningsList(GoldEarnings goldEarnings) {
        // 查询黄金收益记录
        List<GoldEarnings> goldEarningsList = goldEarningsMapper.selectGoldEarningsList(goldEarnings);

        // 使用 JDK8 的 forEach 处理逻辑
        goldEarningsList.forEach(goldEarnings2 -> {
            // 根据机构简称获取对应中文名，再查询最新基准金价
            String institutionName = Institution.getChineseNameByAbbreviation(goldEarnings2.getInstitution());
            GoldProductPrice goldProductPrice = goldProductPriceMapper.selectGoldProductPriceByName(institutionName);

            if (goldProductPrice != null) {
                // 设置基准价格
                goldEarnings2.setBenchmarkPrice(goldProductPrice.getPrice());

                // 计算回收单价 * 买入重量
                BigDecimal multiply = goldEarnings2.getBuyWeight().multiply(goldEarnings2.getRecyclePrice());

                // 计算收益 = 基准价格 * 买入重量 - (回收单价 * 买入重量)
                BigDecimal profitAmount = goldEarnings2.getBenchmarkPrice()
                        .multiply(goldEarnings2.getBuyWeight())
                        .subtract(multiply);

                // 更新收益金额和时间
                goldEarnings2.setProfitAmount(profitAmount);
                goldEarnings2.setProfitDate(new Date());

                // 更新数据库中的记录
                goldEarningsMapper.updateGoldEarnings(goldEarnings2);
            }
        });

        // 返回最新的收益记录
        return goldEarningsMapper.selectGoldEarningsList(goldEarnings);
    }


    /**
     * 新增攒金收益记录
     *
     * @param goldEarnings 攒金收益记录
     * @return 结果
     */
    @Override
    public int insertGoldEarnings(GoldEarnings goldEarnings) {
        return goldEarningsMapper.insertGoldEarnings(goldEarnings);
    }

    /**
     * 修改攒金收益记录
     *
     * @param goldEarnings 攒金收益记录
     * @return 结果
     */
    @Override
    public int updateGoldEarnings(GoldEarnings goldEarnings) {
        return goldEarningsMapper.updateGoldEarnings(goldEarnings);
    }

    /**
     * 批量删除攒金收益记录
     *
     * @param ids 需要删除的攒金收益记录主键
     * @return 结果
     */
    @Override
    public int deleteGoldEarningsByIds(Long[] ids) {
        return goldEarningsMapper.deleteGoldEarningsByIds(ids);
    }

    /**
     * 删除攒金收益记录信息
     *
     * @param id 攒金收益记录主键
     * @return 结果
     */
    @Override
    public int deleteGoldEarningsById(Long id) {
        return goldEarningsMapper.deleteGoldEarningsById(id);
    }
}
