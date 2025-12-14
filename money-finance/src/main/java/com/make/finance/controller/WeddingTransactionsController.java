package com.make.finance.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.make.common.annotation.Log;
import com.make.common.core.controller.BaseController;
import com.make.common.core.domain.AjaxResult;
import com.make.common.enums.BusinessType;
import com.make.finance.domain.WeddingTransactions;
import com.make.finance.service.IWeddingTransactionsService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 婚礼收支明细Controller
 *
 * @author 贰柒
 * @date 2025-05-28
 */
@RestController
@RequestMapping("/finance/wedding_transactions")
public class WeddingTransactionsController extends BaseController {
    @Autowired
    private IWeddingTransactionsService weddingTransactionsService;

    /**
     * 查询婚礼收支明细列表
     */
    @PreAuthorize("@ss.hasPermi('finance:wedding_transactions:list')")
    @GetMapping("/list")
    public TableDataInfo list(WeddingTransactions weddingTransactions) {
        startPage();
        boolean equals = weddingTransactions.getUserId().equals(1L);
        if (equals) {
            weddingTransactions.setUserId(null);
        }
        List<WeddingTransactions> list = weddingTransactionsService.selectWeddingTransactionsList(weddingTransactions);
        return getDataTable(list);
    }

    /**
     * 导出婚礼收支明细列表
     */
    @PreAuthorize("@ss.hasPermi('finance:wedding_transactions:export')")
    @Log(title = "婚礼收支明细", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, WeddingTransactions weddingTransactions) {
        List<WeddingTransactions> list = weddingTransactionsService.selectWeddingTransactionsList(weddingTransactions);
        ExcelUtil<WeddingTransactions> util = new ExcelUtil<WeddingTransactions>(WeddingTransactions.class);
        util.exportExcel(response, list, "婚礼收支明细数据");
    }

    /**
     * 获取婚礼收支明细详细信息
     */
    @PreAuthorize("@ss.hasPermi('finance:wedding_transactions:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(weddingTransactionsService.selectWeddingTransactionsById(id));
    }

    /**
     * 新增婚礼收支明细
     */
    @PreAuthorize("@ss.hasPermi('finance:wedding_transactions:add')")
    @Log(title = "婚礼收支明细", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WeddingTransactions weddingTransactions) {
        return toAjax(weddingTransactionsService.insertWeddingTransactions(weddingTransactions));
    }

    /**
     * 修改婚礼收支明细
     */
    @PreAuthorize("@ss.hasPermi('finance:wedding_transactions:edit')")
    @Log(title = "婚礼收支明细", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WeddingTransactions weddingTransactions) {
        return toAjax(weddingTransactionsService.updateWeddingTransactions(weddingTransactions));
    }

    /**
     * 删除婚礼收支明细
     */
    @PreAuthorize("@ss.hasPermi('finance:wedding_transactions:remove')")
    @Log(title = "婚礼收支明细", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(weddingTransactionsService.deleteWeddingTransactionsByIds(ids));
    }
}
