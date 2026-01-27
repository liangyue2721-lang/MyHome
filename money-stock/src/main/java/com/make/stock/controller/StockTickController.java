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
import com.make.stock.domain.StockTick;
import com.make.stock.service.IStockTickService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 股票逐笔成交明细Controller
 *
 * @author erqi
 * @date 2026-01-27
 */
@RestController
@RequestMapping("/stock/stockTick")
public class StockTickController extends BaseController {

    @Autowired
    private IStockTickService stockTickService;

    /**
     * 查询股票逐笔成交明细列表
     */
    @PreAuthorize("@ss.hasPermi('stock:stockTick:list')")
    @GetMapping("/list")
    public TableDataInfo list(StockTick stockTick) {
        startPage();
        List<StockTick> list = stockTickService.selectStockTickList(stockTick);
        return getDataTable(list);
    }

    /**
     * 导出股票逐笔成交明细列表
     */
    @PreAuthorize("@ss.hasPermi('stock:stockTick:export')")
    @Log(title = "股票逐笔成交明细", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StockTick stockTick) {
        List<StockTick> list = stockTickService.selectStockTickList(stockTick);
        ExcelUtil<StockTick> util = new ExcelUtil<StockTick>(StockTick.class);
        util.exportExcel(response, list, "股票逐笔成交明细数据");
    }

    /**
     * 获取股票逐笔成交明细详细信息
     */
    @PreAuthorize("@ss.hasPermi('stock:stockTick:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id) {
        return success(stockTickService.selectStockTickById(id));
    }

    /**
     * 新增股票逐笔成交明细
     */
    @PreAuthorize("@ss.hasPermi('stock:stockTick:add')")
    @Log(title = "股票逐笔成交明细", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody StockTick stockTick) {
        return toAjax(stockTickService.insertStockTick(stockTick));
    }

    /**
     * 修改股票逐笔成交明细
     */
    @PreAuthorize("@ss.hasPermi('stock:stockTick:edit')")
    @Log(title = "股票逐笔成交明细", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody StockTick stockTick) {
        return toAjax(stockTickService.updateStockTick(stockTick));
    }

    /**
     * 删除股票逐笔成交明细
     */
    @PreAuthorize("@ss.hasPermi('stock:stockTick:remove')")
    @Log(title = "股票逐笔成交明细", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(stockTickService.deleteStockTickByIds(ids));
    }
}
