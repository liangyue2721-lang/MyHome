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
import com.make.stock.domain.StockInfoDongfangHis;
import com.make.stock.service.IStockInfoDongfangHisService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 东方财富历史Controller
 *
 * @author erqi
 * @date 2025-05-28
 */
@RestController
@RequestMapping("/stock/stock_info_dongfang_his")
public class StockInfoDongfangHisController extends BaseController {

    @Autowired
    private IStockInfoDongfangHisService stockInfoDongfangHisService;

    /**
     * 查询东方财富历史列表
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_info_dongfang_his:list')")
    @GetMapping("/list")
    public TableDataInfo list(StockInfoDongfangHis stockInfoDongfangHis) {
        startPage();
        List<StockInfoDongfangHis> list = stockInfoDongfangHisService.selectStockInfoDongfangHisList(stockInfoDongfangHis);
        return getDataTable(list);
    }

    /**
     * 导出东方财富历史列表
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_info_dongfang_his:export')")
    @Log(title = "东方财富历史", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StockInfoDongfangHis stockInfoDongfangHis) {
        List<StockInfoDongfangHis> list = stockInfoDongfangHisService.selectStockInfoDongfangHisList(stockInfoDongfangHis);
        ExcelUtil<StockInfoDongfangHis> util = new ExcelUtil<StockInfoDongfangHis>(StockInfoDongfangHis.class);
        util.exportExcel(response, list, "东方财富历史数据");
    }

    /**
     * 获取东方财富历史详细信息
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_info_dongfang_his:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(stockInfoDongfangHisService.selectStockInfoDongfangHisById(id));
    }

    /**
     * 新增东方财富历史
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_info_dongfang_his:add')")
    @Log(title = "东方财富历史", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody StockInfoDongfangHis stockInfoDongfangHis) {
        return toAjax(stockInfoDongfangHisService.insertStockInfoDongfangHis(stockInfoDongfangHis));
    }

    /**
     * 修改东方财富历史
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_info_dongfang_his:edit')")
    @Log(title = "东方财富历史", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody StockInfoDongfangHis stockInfoDongfangHis) {
        return toAjax(stockInfoDongfangHisService.updateStockInfoDongfangHis(stockInfoDongfangHis));
    }

    /**
     * 删除东方财富历史
     */
    @PreAuthorize("@ss.hasPermi('stock:stock_info_dongfang_his:remove')")
    @Log(title = "东方财富历史", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(stockInfoDongfangHisService.deleteStockInfoDongfangHisByIds(ids));
    }
}
