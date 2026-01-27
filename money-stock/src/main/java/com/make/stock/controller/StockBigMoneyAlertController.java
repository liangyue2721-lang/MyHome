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
import com.make.stock.domain.StockBigMoneyAlert;
import com.make.stock.service.IStockBigMoneyAlertService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 大资金入场异动预警Controller
 *
 * @author erqi
 * @date 2026-01-27
 */
@RestController
@RequestMapping("/stock/stockAlert")
public class StockBigMoneyAlertController extends BaseController {

    @Autowired
    private IStockBigMoneyAlertService stockBigMoneyAlertService;

    /**
     * 查询大资金入场异动预警列表
     */
    @PreAuthorize("@ss.hasPermi('stock:stockAlert:list')")
    @GetMapping("/list")
    public TableDataInfo list(StockBigMoneyAlert stockBigMoneyAlert) {
        startPage();
        List<StockBigMoneyAlert> list = stockBigMoneyAlertService.selectStockBigMoneyAlertList(stockBigMoneyAlert);
        return getDataTable(list);
    }

    /**
     * 导出大资金入场异动预警列表
     */
    @PreAuthorize("@ss.hasPermi('stock:stockAlert:export')")
    @Log(title = "大资金入场异动预警", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StockBigMoneyAlert stockBigMoneyAlert) {
        List<StockBigMoneyAlert> list = stockBigMoneyAlertService.selectStockBigMoneyAlertList(stockBigMoneyAlert);
        ExcelUtil<StockBigMoneyAlert> util = new ExcelUtil<StockBigMoneyAlert>(StockBigMoneyAlert.class);
        util.exportExcel(response, list, "大资金入场异动预警数据");
    }

    /**
     * 获取大资金入场异动预警详细信息
     */
    @PreAuthorize("@ss.hasPermi('stock:stockAlert:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id) {
        return success(stockBigMoneyAlertService.selectStockBigMoneyAlertById(id));
    }

    /**
     * 新增大资金入场异动预警
     */
    @PreAuthorize("@ss.hasPermi('stock:stockAlert:add')")
    @Log(title = "大资金入场异动预警", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody StockBigMoneyAlert stockBigMoneyAlert) {
        return toAjax(stockBigMoneyAlertService.insertStockBigMoneyAlert(stockBigMoneyAlert));
    }

    /**
     * 修改大资金入场异动预警
     */
    @PreAuthorize("@ss.hasPermi('stock:stockAlert:edit')")
    @Log(title = "大资金入场异动预警", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody StockBigMoneyAlert stockBigMoneyAlert) {
        return toAjax(stockBigMoneyAlertService.updateStockBigMoneyAlert(stockBigMoneyAlert));
    }

    /**
     * 删除大资金入场异动预警
     */
    @PreAuthorize("@ss.hasPermi('stock:stockAlert:remove')")
    @Log(title = "大资金入场异动预警", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(stockBigMoneyAlertService.deleteStockBigMoneyAlertByIds(ids));
    }
}
