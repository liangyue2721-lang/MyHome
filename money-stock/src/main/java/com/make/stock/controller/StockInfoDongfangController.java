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
import com.make.stock.domain.StockInfoDongfang;
import com.make.stock.service.IStockInfoDongfangService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 东方财富股票Controller
 *
 * @author erqi
 * @date 2025-05-28
 */
@RestController
@RequestMapping("/stock/stock_info_dongfang")
public class StockInfoDongfangController extends BaseController {

    @Autowired
    private IStockInfoDongfangService stockInfoDongfangService;

    /**
     * 查询东方财富股票列表
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_info_dongfang:list')")
    @GetMapping("/list")
    public TableDataInfo list(StockInfoDongfang stockInfoDongfang) {
        startPage();
        List<StockInfoDongfang> list = stockInfoDongfangService.selectStockInfoDongfangList(stockInfoDongfang);
        return getDataTable(list);
    }

    /**
     * 导出东方财富股票列表
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_info_dongfang:export')")
    @Log(title = "东方财富股票", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StockInfoDongfang stockInfoDongfang) {
        List<StockInfoDongfang> list = stockInfoDongfangService.selectStockInfoDongfangList(stockInfoDongfang);
        ExcelUtil<StockInfoDongfang> util = new ExcelUtil<StockInfoDongfang>(StockInfoDongfang.class);
        util.exportExcel(response, list, "东方财富股票数据");
    }

    /**
     * 获取东方财富股票详细信息
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_info_dongfang:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(stockInfoDongfangService.selectStockInfoDongfangById(id));
    }

    /**
     * 新增东方财富股票
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_info_dongfang:add')")
    @Log(title = "东方财富股票", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody StockInfoDongfang stockInfoDongfang) {
        return toAjax(stockInfoDongfangService.insertStockInfoDongfang(stockInfoDongfang));
    }

    /**
     * 修改东方财富股票
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_info_dongfang:edit')")
    @Log(title = "东方财富股票", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody StockInfoDongfang stockInfoDongfang) {
        return toAjax(stockInfoDongfangService.updateStockInfoDongfang(stockInfoDongfang));
    }

    /**
     * 删除东方财富股票
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_info_dongfang:remove')")
    @Log(title = "东方财富股票", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(stockInfoDongfangService.deleteStockInfoDongfangByIds(ids));
    }
}
