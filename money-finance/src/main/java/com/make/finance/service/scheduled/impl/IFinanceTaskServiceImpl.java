package com.make.finance.service.scheduled.impl;

import com.make.finance.service.scheduled.IFinanceTaskService;
import com.make.finance.service.scheduled.finance.DepositService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 财务任务服务实现类 (Facade)
 *
 * 历史原因：原实现类逻辑已迁移至 com.make.quartz.service.finance.DepositService
 * 本类现作为 Facade 委托调用，确保原有依赖不报错。
 */
@Service
public class IFinanceTaskServiceImpl implements IFinanceTaskService {

    private static final Logger log = LoggerFactory.getLogger(IFinanceTaskServiceImpl.class);

    @Resource
    private DepositService depositService;

    @Override
    public void refreshDepositAmount() {
        log.info("【Facade】IFinanceTaskService 委托调用 DepositService.refreshDepositAmount");
        depositService.refreshDepositAmount();
    }
}
