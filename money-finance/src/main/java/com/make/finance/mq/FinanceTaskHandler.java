package com.make.finance.mq;

import com.make.common.annotation.IdempotentConsumer;
import com.make.finance.service.scheduled.finance.DepositService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * Finance Task Handler (Logic Layer)
 * Separated from Consumer to allow AOP proxying for Idempotency.
 */
@Service
public class FinanceTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(FinanceTaskHandler.class);

    @Resource
    private DepositService depositService;

    @IdempotentConsumer(key = "#traceId")
    public void handleDepositUpdate(String traceId) {
        depositService.updateAnnualDepositSummary();
    }

    @IdempotentConsumer(key = "#traceId")
    public void handleICBCDepositUpdate(String traceId) {
        // Legacy FixedTimeTask calls with 1L, 7L
        depositService.updateICBCDepositAmount(1L, 7L);
    }
}
