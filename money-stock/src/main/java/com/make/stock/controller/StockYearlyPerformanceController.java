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
import com.make.stock.domain.StockYearlyPerformance;
import com.make.stock.service.IStockYearlyPerformanceService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 股票当年现数据Controller
 *
 * @author erqi
 * @date 2025-10-19
 */
@RestController
@RequestMapping("/stock/stock_performance")
public class StockYearlyPerformanceController extends BaseController {

    @Autowired
    private IStockYearlyPerformanceService stockYearlyPerformanceService;

    /**
     * 查询股票当年现数据列表
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_performance:list')")
    @GetMapping("/list")
    public TableDataInfo list(StockYearlyPerformance stockYearlyPerformance) {
        startPage();
        List<StockYearlyPerformance> list = stockYearlyPerformanceService.selectStockYearlyPerformanceList(stockYearlyPerformance);
        return getDataTable(list);
    }

    /**
     * 导出股票当年现数据列表
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_performance:export')")
    @Log(title = "股票当年现数据", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StockYearlyPerformance stockYearlyPerformance) {
        List<StockYearlyPerformance> list = stockYearlyPerformanceService.selectStockYearlyPerformanceList(stockYearlyPerformance);
        ExcelUtil<StockYearlyPerformance> util = new ExcelUtil<StockYearlyPerformance>(StockYearlyPerformance.class);
        util.exportExcel(response, list, "股票当年现数据数据");
    }

    /**
     * 获取股票当年现数据详细信息
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_performance:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(stockYearlyPerformanceService.selectStockYearlyPerformanceById(id));
    }

    /**
     * 新增股票当年现数据
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_performance:add')")
    @Log(title = "股票当年现数据", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody StockYearlyPerformance stockYearlyPerformance) {
        return toAjax(stockYearlyPerformanceService.insertStockYearlyPerformance(stockYearlyPerformance));
    }

    /**
     * 修改股票当年现数据
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_performance:edit')")
    @Log(title = "股票当年现数据", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody StockYearlyPerformance stockYearlyPerformance) {
        return toAjax(stockYearlyPerformanceService.updateStockYearlyPerformance(stockYearlyPerformance));
    }

    /**
     * 删除股票当年现数据
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_performance:remove')")
    @Log(title = "股票当年现数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(stockYearlyPerformanceService.deleteStockYearlyPerformanceByIds(ids));
    }
}
