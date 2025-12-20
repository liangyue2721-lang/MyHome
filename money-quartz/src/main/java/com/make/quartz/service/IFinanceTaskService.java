package com.make.quartz.service;

/**
 * 财务任务服务接口
 * @deprecated 建议直接使用 {@link com.make.quartz.service.finance.DepositService}
 */
@Deprecated
public interface IFinanceTaskService {

    void refreshDepositAmount();

    // Note: updateICBCDepositAmount was not in the original interface but was in the impl.
    // Since we are preserving the interface, we don't add it here if it wasn't here.
    // The previous implementation had updateBankDepositAmount and updateICBCDepositAmount as private/public methods
    // but the interface only exposed refreshDepositAmount.
    // Wait, let me check the previous `read_file` of IFinanceTaskService again.
}
