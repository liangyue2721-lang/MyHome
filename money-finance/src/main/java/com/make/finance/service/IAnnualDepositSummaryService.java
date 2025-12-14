package com.make.finance.service;

import java.util.List;

import com.make.finance.domain.AnnualDepositSummary;

/**
 * 年度存款统计Service接口
 *
 * @author erqi
 * @date 2025-07-20
 */
public interface IAnnualDepositSummaryService {

    /**
     * 查询年度存款统计
     *
     * @param id 年度存款统计主键
     * @return 年度存款统计
     */
    public AnnualDepositSummary selectAnnualDepositSummaryById(Long id);

    /**
     * 查询年度存款统计列表
     *
     * @param annualDepositSummary 年度存款统计
     * @return 年度存款统计集合
     */
    public List<AnnualDepositSummary> selectAnnualDepositSummaryList(AnnualDepositSummary annualDepositSummary);

    /**
     * 新增年度存款统计
     *
     * @param annualDepositSummary 年度存款统计
     * @return 结果
     */
    public int insertAnnualDepositSummary(AnnualDepositSummary annualDepositSummary);

    /**
     * 修改年度存款统计
     *
     * @param annualDepositSummary 年度存款统计
     * @return 结果
     */
    public int updateAnnualDepositSummary(AnnualDepositSummary annualDepositSummary);

    /**
     * 批量删除年度存款统计
     *
     * @param ids 需要删除的年度存款统计主键集合
     * @return 结果
     */
    public int deleteAnnualDepositSummaryByIds(Long[] ids);

    /**
     * 删除年度存款统计信息
     *
     * @param id 年度存款统计主键
     * @return 结果
     */
    public int deleteAnnualDepositSummaryById(Long id);

    /**
     * 根据指定年份查询年度存款汇总信息
     * <p>
     * 该方法通过年份精确检索对应的年度存款统计记录
     *
     * @param currentYear 查询年份（格式：yyyy）
     *        必须为有效年份值（如2025），不可为未来未统计年份
     * @return 对应年份的存款汇总实体对象
     *         当指定年份无记录时返回null
     */
    AnnualDepositSummary queryAnnualDepositSummaryByYearAndUser(int currentYear,Long userId);
}
