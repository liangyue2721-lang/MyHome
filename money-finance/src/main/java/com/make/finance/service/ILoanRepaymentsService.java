package com.make.finance.service;

import java.util.Date;
import java.util.List;

import com.make.finance.domain.LoanRepayments;
import com.make.finance.domain.vo.LoanRepaymentsChart;

/**
 * 贷款剩余计算Service接口
 *
 * @author 贰柒
 * @date 2025-05-28
 */
public interface ILoanRepaymentsService {

    /**
     * 查询贷款剩余计算
     *
     * @param id 贷款剩余计算主键
     * @return 贷款剩余计算
     */
    public LoanRepayments selectLoanRepaymentsById(Long id);

    /**
     * 查询贷款剩余计算列表
     *
     * @param loanRepayments 贷款剩余计算
     * @return 贷款剩余计算集合
     */
    public List<LoanRepayments> selectLoanRepaymentsList(LoanRepayments loanRepayments);

    /**
     * 新增贷款剩余计算
     *
     * @param loanRepayments 贷款剩余计算
     * @return 结果
     */
    public int insertLoanRepayments(LoanRepayments loanRepayments);

    /**
     * 修改贷款剩余计算
     *
     * @param loanRepayments 贷款剩余计算
     * @return 结果
     */
    public int updateLoanRepayments(LoanRepayments loanRepayments);

    /**
     * 批量删除贷款剩余计算
     *
     * @param ids 需要删除的贷款剩余计算主键集合
     * @return 结果
     */
    public int deleteLoanRepaymentsByIds(Long[] ids);

    /**
     * 删除贷款剩余计算信息
     *
     * @param id 贷款剩余计算主键
     * @return 结果
     */
    public int deleteLoanRepaymentsById(Long id);

    /**
     * 重置贷款利率至系统默认值
     *
     * <p>该方法会将当前贷款产品的利率配置恢复为系统预设的基准值，
     * 通常用于重新计算贷款利息或调整利率策略时调用</p>
     *
     * @throws IllegalStateException 当利率配置处于锁定状态时抛出
     * @apiNote 重置操作会覆盖所有自定义利率设置
     * @since 2.3.0 新增批量重置功能
     */
    void resetRate();

    /**
     * 根据贷款ID更新还款计划记录
     *
     * <p>该方法通过贷款唯一标识更新对应的还款计划表，支持部分字段更新：
     * <ul>
     *   <li>{@linkplain LoanRepayments#getRepaymentDate() 还款日期}</li>
     *   <li>{@linkplain LoanRepayments#getPrincipal() 本金}</li>
     *   <li>{@linkplain LoanRepayments#getInterest() 利息}</li>
     * </ul>
     *
     * @param loanRepaymentsObj 包含更新数据的实体对象，需满足：
     *                          <ul>
     *                              <li>{@code loanRepaymentsObj.getId()} 不能为null</li>
     *                              <li>{@code loanRepaymentsObj.getVersion()} 需与数据库版本一致</li>
     *                          </ul>
     * @return 受影响的数据库行数（0表示未更新，1表示成功）
     * @throws IllegalArgumentException          当参数对象为空或关键字段缺失时抛出
     * @apiNote 该方法会触发贷款余额重新计算
     * @reference 参考银保监发[2023](@ ref)12号文关于贷款数据更新规范
     */
    int updateLoanRepaymentsById(LoanRepayments loanRepaymentsObj);


    LoanRepayments selectLoanRepaymentsByDate(Date date);

    List<LoanRepaymentsChart> queryLoanRepaymentsChartList(Long userId);

    List<LoanRepayments> selectLoanRepaymentsChart(LoanRepayments query);
}
