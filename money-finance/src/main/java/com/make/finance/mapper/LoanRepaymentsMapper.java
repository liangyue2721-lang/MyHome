package com.make.finance.mapper;

import java.util.Date;
import java.util.List;

import com.make.finance.domain.LoanRepayments;
import org.apache.ibatis.annotations.Param;

/**
 * 贷款剩余计算Mapper接口
 *
 * @author 贰柒
 * @date 2025-05-28
 */
public interface LoanRepaymentsMapper {
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
     * 查询贷款剩余计算列表
     *
     * @param loanRepayments 贷款剩余计算
     * @return 贷款剩余计算集合
     */
    public List<LoanRepayments> selectLoanRepaymentsChart(LoanRepayments loanRepayments);

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
     * 删除贷款剩余计算
     *
     * @param id 贷款剩余计算主键
     * @return 结果
     */
    public int deleteLoanRepaymentsById(Long id);

    /**
     * 批量删除贷款剩余计算
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteLoanRepaymentsByIds(Long[] ids);

    /**
     * 查询所有未完成还款的贷款记录
     *
     * <p>该方法会检索系统中所有处于未结清状态的贷款合同，包含以下关键信息：
     *
     * @return 未还款贷款列表，若无数据则返回空集合
     * @apiNote 该方法包含所有状态的未结清贷款（正常还款中/逾期中）
     * @example <pre>{@code
     * List<LoanRepayments> list = service.selectUnpaidLoansList();
     * // 输出：[LoanRepayments{id=1001, dueDate=2025-06-01, ...}]</pre>
     * @see LoanStatus#UNPAID
     */
    List<LoanRepayments> selectUnpaidLoansList();

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
     * <b>参数约束：</b>
     * <ul>
     *   <li>{@code loanRepaymentsObj.getId()} 必须为非空且存在于数据库</li>
     *   <li>{@code loanRepaymentsObj.getVersion()} 需与数据库版本一致（乐观锁机制）</li>
     * </ul>
     *
     * @param loanRepaymentsObj 包含更新数据的实体对象
     * @return 受影响的数据库行数（0表示未更新，1表示成功）
     * @throws IllegalArgumentException 当参数对象为空或关键字段缺失时抛出
     * @apiNote 该方法会触发贷款余额重新计算和逾期状态更新
     * @reference 参考银保监发[2023](@ ref)12号文关于贷款数据更新规范
     * @since 2.3.0 新增批量更新支持
     */
    int updateLoanRepaymentsById(LoanRepayments loanRepaymentsObj);


    LoanRepayments selectLoanRepaymentsByDate(@Param("repaymentDate") Date date);
}
