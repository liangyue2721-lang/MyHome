package com.make.finance.service.impl;

import java.util.List;

import com.make.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.finance.mapper.AnnualDepositSummaryMapper;
import com.make.finance.domain.AnnualDepositSummary;
import com.make.finance.service.IAnnualDepositSummaryService;

/**
 * 年度存款统计Service业务层处理
 *
 * @author erqi
 * @date 2025-07-20
 */
@Service
public class AnnualDepositSummaryServiceImpl implements IAnnualDepositSummaryService {

    @Autowired
    private AnnualDepositSummaryMapper annualDepositSummaryMapper;

    /**
     * 查询年度存款统计
     *
     * @param id 年度存款统计主键
     * @return 年度存款统计
     */
    @Override
    public AnnualDepositSummary selectAnnualDepositSummaryById(Long id) {
        return annualDepositSummaryMapper.selectAnnualDepositSummaryById(id);
    }

    /**
     * 查询年度存款统计列表
     *
     * @param annualDepositSummary 年度存款统计
     * @return 年度存款统计
     */
    @Override
    public List<AnnualDepositSummary> selectAnnualDepositSummaryList(AnnualDepositSummary annualDepositSummary) {
        return annualDepositSummaryMapper.selectAnnualDepositSummaryList(annualDepositSummary);
    }

    /**
     * 新增年度存款统计
     *
     * @param annualDepositSummary 年度存款统计
     * @return 结果
     */
    @Override
    public int insertAnnualDepositSummary(AnnualDepositSummary annualDepositSummary) {
        annualDepositSummary.setCreateTime(DateUtils.getNowDate());
        return annualDepositSummaryMapper.insertAnnualDepositSummary(annualDepositSummary);
    }

    /**
     * 修改年度存款统计
     *
     * @param annualDepositSummary 年度存款统计
     * @return 结果
     */
    @Override
    public int updateAnnualDepositSummary(AnnualDepositSummary annualDepositSummary) {
        annualDepositSummary.setUpdateTime(DateUtils.getNowDate());
        return annualDepositSummaryMapper.updateAnnualDepositSummary(annualDepositSummary);
    }

    /**
     * 批量删除年度存款统计
     *
     * @param ids 需要删除的年度存款统计主键
     * @return 结果
     */
    @Override
    public int deleteAnnualDepositSummaryByIds(Long[] ids) {
        return annualDepositSummaryMapper.deleteAnnualDepositSummaryByIds(ids);
    }

    /**
     * 删除年度存款统计信息
     *
     * @param id 年度存款统计主键
     * @return 结果
     */
    @Override
    public int deleteAnnualDepositSummaryById(Long id) {
        return annualDepositSummaryMapper.deleteAnnualDepositSummaryById(id);
    }

    @Override
    public AnnualDepositSummary queryAnnualDepositSummaryByYearAndUser(int currentYear, Long userId) {
        return annualDepositSummaryMapper.selectAnnualDepositSummaryByYear(currentYear, userId);
    }
}
