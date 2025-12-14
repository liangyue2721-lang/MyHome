package com.make.stock.controller;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
import com.make.stock.domain.StockTrades;
import com.make.stock.service.IStockTradesService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 股票利润Controller
 *
 * @author make
 * @date 2025-05-28
 */
@RestController
@RequestMapping("/stock/stock_trades")
public class StockTradesController extends BaseController {
    @Autowired
    private IStockTradesService stockTradesService;

    /**
     * 查询股票利润列表
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_trades:list')")
    @GetMapping("/list")
    public TableDataInfo list(StockTrades stockTrades) {
        // 防御性编程：若传入参数为空，则直接返回空数据表
        if (stockTrades == null || stockTrades.getUserId() == null) {
            return getDataTable(Collections.emptyList());
        }

        // 管理员ID常量（建议定义在类级别）
        final long ADMIN_USER_ID = 1L;

        // 管理员特殊逻辑：不限制userId
        if (Objects.equals(stockTrades.getUserId(), ADMIN_USER_ID)) {
            stockTrades.setUserId(null);
        }

        // 启动分页查询
        startPage();

        // 查询结果列表
        List<StockTrades> list = stockTradesService.selectStockTradesList(stockTrades);

        // 返回分页封装结果
        return getDataTable(list);
    }

    /**
     * 导出股票利润列表
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_trades:export')")
    @Log(title = "股票利润", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StockTrades stockTrades) {
        List<StockTrades> list = stockTradesService.selectStockTradesList(stockTrades);
        ExcelUtil<StockTrades> util = new ExcelUtil<StockTrades>(StockTrades.class);
        util.exportExcel(response, list, "股票利润数据");
    }

    /**
     * 获取股票利润详细信息
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_trades:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(stockTradesService.selectStockTradesById(id));
    }

    /**
     * 新增股票利润
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_trades:add')")
    @Log(title = "股票利润", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody StockTrades stockTrades) {
        return toAjax(stockTradesService.insertStockTrades(stockTrades));
    }

    /**
     * 修改股票利润
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_trades:edit')")
    @Log(title = "股票利润", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody StockTrades stockTrades) {
        return toAjax(stockTradesService.updateStockTrades(stockTrades));
    }

    /**
     * 删除股票利润
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_trades:remove')")
    @Log(title = "股票利润", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(stockTradesService.deleteStockTradesByIds(ids));
    }
}
