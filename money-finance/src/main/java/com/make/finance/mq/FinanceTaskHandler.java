package com.make.finance.mq;

import com.make.common.annotation.IdempotentConsumer;
import com.make.finance.domain.dto.CCBCreditCardTransactionEmail;
import com.make.finance.service.scheduled.finance.CreditCardService;
import com.make.finance.service.scheduled.finance.DepositService;
import com.make.finance.util.email.EmailReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * Finance Task Handler (Logic Layer)
 * Separated from Consumer to allow AOP proxying for Idempotency.
 */
@Service
public class FinanceTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(FinanceTaskHandler.class);

    @Resource
    private DepositService depositService;

    @Resource
    private CreditCardService creditCardService;

    @IdempotentConsumer(key = "#traceId")
    public void handleDepositUpdate(String traceId) {
        depositService.updateAnnualDepositSummary();
    }

    @IdempotentConsumer(key = "#traceId")
    public void handleICBCDepositUpdate(String traceId) {
        // Legacy FixedTimeTask calls with 1L, 7L
        depositService.updateICBCDepositAmount(1L, 7L);
    }

    @IdempotentConsumer(key = "#traceId")
    public void handleCcbCreditCard(String traceId) {
        try {
            List<CCBCreditCardTransactionEmail> list = EmailReader.getSmsCode();
            creditCardService.saveCCBCreditCardTransaction(list);
        } catch (Exception e) {
            log.error("Failed to handle CCB Credit Card task", e);
        }
    }
}
