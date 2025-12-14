package com.make.web.controller.pie;

import com.make.common.core.domain.AjaxResult;
import com.make.finance.domain.dto.CCBCreditCardTransactionEmail;
import com.make.finance.service.ICbcCreditCardTransactionService;
import com.make.quartz.service.IRealTimeService;
import com.make.quartz.util.email.EmailReader;
import com.make.web.service.ISyncService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;



@RestController
@RequestMapping("/finance/asset_record")
public class SyncController {

    // 日志记录器
    private static final Logger log = LoggerFactory.getLogger(SyncController.class);

    @Resource
    private ISyncService syncService;

    /**
     * 同步股票交易数据
     *
     * @return 同步结果
     */
    @PreAuthorize("@ss.hasPermi('finance:record:sync')")
    @GetMapping("/sync")
    public AjaxResult sync() {
        return new AjaxResult(200, "成功", syncService.syncStockTrades());
    }

    private static boolean queryStatus = false;

    @Resource
    private IRealTimeService realTimeService;

    @Resource
    private ICbcCreditCardTransactionService cbcCreditCardTransactionService;


    /**
     * 查询并记录建设银行信用卡流水
     *
     * @return 处理结果
     */
    @PreAuthorize("@ss.hasPermi('finance:ccb_credit_card_transaction:queryCCBCreditCardTransaction')")
    @GetMapping( "/handleQuerySync")
    public AjaxResult handleQuerySync() {
        long startTime = System.currentTimeMillis();
        try {
            if (queryStatus) {
                return new AjaxResult(200, "成功");
            }
            queryStatus = true;
            List<CCBCreditCardTransactionEmail> CCBCreditCardTransactionList = EmailReader.getSmsCode();
            if (CollectionUtils.isNotEmpty(CCBCreditCardTransactionList)) {
                realTimeService.saveCCBCreditCardTransaction(CCBCreditCardTransactionList);
            }
        } catch (Exception e) {
            log.error("执行realTimeTask查询并记录建设银行信用卡流水失败:", e);
            return new AjaxResult(500, "失败");
        } finally {
            queryStatus = false;
        }
        long endTime = System.currentTimeMillis();
        log.info("执行realTimeTask查询并记录建设银行信用卡流水任务完成,耗时{}ms", endTime - startTime);
        return new AjaxResult(200, "成功");
    }
}