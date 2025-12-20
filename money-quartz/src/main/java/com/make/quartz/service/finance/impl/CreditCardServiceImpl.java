package com.make.quartz.service.finance.impl;

import com.make.finance.domain.dto.CCBCreditCardTransactionEmail;
import com.make.quartz.service.finance.CreditCardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 信用卡服务实现类
 */
@Service
public class CreditCardServiceImpl implements CreditCardService {

    private static final Logger log = LoggerFactory.getLogger(CreditCardServiceImpl.class);

    @Override
    public void saveCCBCreditCardTransaction(List<CCBCreditCardTransactionEmail> emailList) {
        log.info("【CreditCardService】保存信用卡交易记录，数量: {}", emailList != null ? emailList.size() : 0);
    }
}
