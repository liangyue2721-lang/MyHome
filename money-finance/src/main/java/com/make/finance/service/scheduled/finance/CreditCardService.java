package com.make.finance.service.scheduled.finance;

import com.make.finance.domain.dto.CCBCreditCardTransactionEmail;
import java.util.List;

/**
 * 信用卡服务接口
 * 负责处理信用卡交易记录的保存和解析
 */
public interface CreditCardService {

    /**
     * 保存工商银行信用卡交易记录
     *
     * @param emailList 从邮件解析出的交易记录列表
     */
    void saveCCBCreditCardTransaction(List<CCBCreditCardTransactionEmail> emailList);
}
