package com.make.stock.controller;

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
import com.make.stock.domain.StockIssueInfo;
import com.make.stock.service.IStockIssueInfoService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 新股发行信息Controller
 *
 * @author erqi
 * @date 2025-05-28
 */
@RestController
@RequestMapping("/stock/stock_issue_info")
public class StockIssueInfoController extends BaseController {

    @Autowired
    private IStockIssueInfoService stockIssueInfoService;

    /**
     * 查询新股发行信息列表
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_issue_info:list')")
    @GetMapping("/list")
    public TableDataInfo list(StockIssueInfo stockIssueInfo) {
        startPage();
        List<StockIssueInfo> list = stockIssueInfoService.selectStockIssueInfoList(stockIssueInfo);
        return getDataTable(list);
    }

    /**
     * 导出新股发行信息列表
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_issue_info:export')")
    @Log(title = "新股发行信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StockIssueInfo stockIssueInfo) {
        List<StockIssueInfo> list = stockIssueInfoService.selectStockIssueInfoList(stockIssueInfo);
        ExcelUtil<StockIssueInfo> util = new ExcelUtil<StockIssueInfo>(StockIssueInfo.class);
        util.exportExcel(response, list, "新股发行信息数据");
    }

    /**
     * 获取新股发行信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_issue_info:query')")
    @GetMapping(value = "/{applyCode}")
    public AjaxResult getInfo(@PathVariable("applyCode") String applyCode) {
        return success(stockIssueInfoService.selectStockIssueInfoByApplyCode(applyCode));
    }

    /**
     * 新增新股发行信息
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_issue_info:add')")
    @Log(title = "新股发行信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody StockIssueInfo stockIssueInfo) {
        return toAjax(stockIssueInfoService.insertStockIssueInfo(stockIssueInfo));
    }

    /**
     * 修改新股发行信息
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_issue_info:edit')")
    @Log(title = "新股发行信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody StockIssueInfo stockIssueInfo) {
        return toAjax(stockIssueInfoService.updateStockIssueInfo(stockIssueInfo));
    }

    /**
     * 删除新股发行信息
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_issue_info:remove')")
    @Log(title = "新股发行信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{applyCodes}")
    public AjaxResult remove(@PathVariable String[] applyCodes) {
        return toAjax(stockIssueInfoService.deleteStockIssueInfoByApplyCodes(applyCodes));
    }
}
