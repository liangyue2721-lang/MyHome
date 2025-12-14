package com.make.finance.mapper;

import java.util.List;

import com.make.finance.domain.BankCardTransactions;
import org.apache.ibatis.annotations.Param;

/**
 * 银行流水Mapper接口
 *
 * @author erqi
 * @date 2025-05-27
 */
public interface BankCardTransactionsMapper {
    /**
     * 查询银行流水
     *
     * @param id 银行流水主键
     * @return 银行流水
     */
    public BankCardTransactions selectBankCardTransactionsById(Long id);

    /**
     * 查询银行流水列表
     *
     * @param bankCardTransactions 银行流水
     * @return 银行流水集合
     */
    public List<BankCardTransactions> selectBankCardTransactionsList(BankCardTransactions bankCardTransactions);

    /**
     * 新增银行流水
     *
     * @param bankCardTransactions 银行流水
     * @return 结果
     */
    public int insertBankCardTransactions(BankCardTransactions bankCardTransactions);

    /**
     * 修改银行流水
     *
     * @param bankCardTransactions 银行流水
     * @return 结果
     */
    public int updateBankCardTransactions(BankCardTransactions bankCardTransactions);

    /**
     * 删除银行流水
     *
     * @param id 银行流水主键
     * @return 结果
     */
    public int deleteBankCardTransactionsById(Long id);

    /**
     * 批量删除银行流水
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteBankCardTransactionsByIds(Long[] ids);

    /**
     * 查询近一年银行卡流水解析列表
     *
     * @return 银行卡流水解析集合
     */
    public List<BankCardTransactions> selectBankCardTransactionsYearList(@Param("userId") Long userId, @Param("startDate") String startDate, @Param("endDate") String endDate);

}
