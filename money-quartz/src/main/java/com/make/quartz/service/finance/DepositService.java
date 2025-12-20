package com.make.quartz.service.finance;

/**
 * 存款服务接口
 * 负责存款统计、资产汇总以及银行账户同步
 */
public interface DepositService {

    /**
     * 刷新存款金额（年度汇总与银行同步）
     */
    void refreshDepositAmount();

    /**
     * 更新年度资产汇总
     */
    void updateAnnualDepositSummary();

    /**
     * 更新指定银行存款数据
     *
     * @param loanRepaymentId 还款记录ID
     * @param assetId 资产ID
     */
    void updateICBCDepositAmount(Long loanRepaymentId, Long assetId);
}
