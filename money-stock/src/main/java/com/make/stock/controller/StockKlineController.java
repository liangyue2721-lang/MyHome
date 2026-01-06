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
import com.make.stock.domain.StockKline;
import com.make.stock.service.IStockKlineService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 股票K线数据Controller
 *
 * @author erqi
 * @date 2025-11-03
 */
@RestController
@RequestMapping("/stock/kline")
public class StockKlineController extends BaseController {

    @Autowired
    private IStockKlineService stockKlineService;

/**
 * 查询股票K线数据列表
 */
@PreAuthorize("@ss.hasPermi('stock:kline:list')")
@GetMapping("/list")
    public TableDataInfo list(StockKline stockKline) {
        startPage();
        List<StockKline> list = stockKlineService.selectStockKlineList(stockKline);
        return getDataTable(list);
    }

    /**
     * 导出股票K线数据列表
     */
    @PreAuthorize("@ss.hasPermi('stock:kline:export')")
    @Log(title = "股票K线数据", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StockKline stockKline) {
        List<StockKline> list = stockKlineService.selectStockKlineList(stockKline);
        ExcelUtil<StockKline> util = new ExcelUtil<StockKline>(StockKline. class);
        util.exportExcel(response, list, "股票K线数据数据");
    }

    /**
     * 获取股票K线数据详细信息
     */
    @PreAuthorize("@ss.hasPermi('stock:kline:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id) {
        return success(stockKlineService.selectStockKlineById(id));
    }

    /**
     * Get Ranking Statistics
     */
    @PreAuthorize("@ss.hasPermi('stock:kline:list')")
    @GetMapping(value = "/ranking/{type}")
    public AjaxResult getRanking(@PathVariable("type") String type) {
        return success(stockKlineService.selectStockRanking(type));
    }

    /**
     * 新增股票K线数据
     */
    @PreAuthorize("@ss.hasPermi('stock:kline:add')")
    @Log(title = "股票K线数据", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody StockKline stockKline) {
        return toAjax(stockKlineService.insertStockKline(stockKline));
    }

    /**
     * 修改股票K线数据
     */
    @PreAuthorize("@ss.hasPermi('stock:kline:edit')")
    @Log(title = "股票K线数据", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody StockKline stockKline) {
        return toAjax(stockKlineService.updateStockKline(stockKline));
    }

    /**
     * 删除股票K线数据
     */
    @PreAuthorize("@ss.hasPermi('stock:kline:remove')")
    @Log(title = "股票K线数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(stockKlineService.deleteStockKlineByIds(ids));
    }
}
