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
import com.make.finance.domain.TransactionCategories;
import com.make.finance.service.ITransactionCategoriesService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 交易分类关键词Controller
 *
 * @author 贰柒
 * @date 2025-05-28
 */
@RestController
@RequestMapping("/finance/categories")
public class TransactionCategoriesController extends BaseController {
    @Autowired
    private ITransactionCategoriesService transactionCategoriesService;

    /**
     * 查询交易分类关键词列表
     */
    @PreAuthorize("@ss.hasPermi('finance:categories:list')")
    @GetMapping("/list")
    public TableDataInfo list(TransactionCategories transactionCategories) {
        startPage();
        List<TransactionCategories> list = transactionCategoriesService.selectTransactionCategoriesList(transactionCategories);
        return getDataTable(list);
    }

    /**
     * 导出交易分类关键词列表
     */
    @PreAuthorize("@ss.hasPermi('finance:categories:export')")
    @Log(title = "交易分类关键词", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TransactionCategories transactionCategories) {
        List<TransactionCategories> list = transactionCategoriesService.selectTransactionCategoriesList(transactionCategories);
        ExcelUtil<TransactionCategories> util = new ExcelUtil<TransactionCategories>(TransactionCategories.class);
        util.exportExcel(response, list, "交易分类关键词数据");
    }

    /**
     * 获取交易分类关键词详细信息
     */
    @PreAuthorize("@ss.hasPermi('finance:categories:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(transactionCategoriesService.selectTransactionCategoriesById(id));
    }

    /**
     * 新增交易分类关键词
     */
    @PreAuthorize("@ss.hasPermi('finance:categories:add')")
    @Log(title = "交易分类关键词", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TransactionCategories transactionCategories) {
        return toAjax(transactionCategoriesService.insertTransactionCategories(transactionCategories));
    }

    /**
     * 修改交易分类关键词
     */
    @PreAuthorize("@ss.hasPermi('finance:categories:edit')")
    @Log(title = "交易分类关键词", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TransactionCategories transactionCategories) {
        return toAjax(transactionCategoriesService.updateTransactionCategories(transactionCategories));
    }

    /**
     * 删除交易分类关键词
     */
    @PreAuthorize("@ss.hasPermi('finance:categories:remove')")
    @Log(title = "交易分类关键词", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(transactionCategoriesService.deleteTransactionCategoriesByIds(ids));
    }
}
