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
import com.make.stock.domain.EtfPriceAlerts;
import com.make.stock.service.IEtfPriceAlertsService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * ETF买入卖出价格提醒Controller
 *
 * @author erqi
 * @date 2025-06-24
 */
@RestController
@RequestMapping("/stock/etf_price_alerts")
public class EtfPriceAlertsController extends BaseController {

    @Autowired
    private IEtfPriceAlertsService etfPriceAlertsService;

    /**
     * 查询ETF买入卖出价格提醒列表
     */
    @PreAuthorize("@ss.hasPermi('stock:etf_price_alerts:list')")
    @GetMapping("/list")
    public TableDataInfo list(EtfPriceAlerts etfPriceAlerts) {
        startPage();
        List<EtfPriceAlerts> list = etfPriceAlertsService.selectEtfPriceAlertsList(etfPriceAlerts);
        return getDataTable(list);
    }

    /**
     * 导出ETF买入卖出价格提醒列表
     */
    @PreAuthorize("@ss.hasPermi('stock:etf_price_alerts:export')")
    @Log(title = "ETF买入卖出价格提醒", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, EtfPriceAlerts etfPriceAlerts) {
        List<EtfPriceAlerts> list = etfPriceAlertsService.selectEtfPriceAlertsList(etfPriceAlerts);
        ExcelUtil<EtfPriceAlerts> util = new ExcelUtil<EtfPriceAlerts>(EtfPriceAlerts.class);
        util.exportExcel(response, list, "ETF买入卖出价格提醒数据");
    }

    /**
     * 获取ETF买入卖出价格提醒详细信息
     */
    @PreAuthorize("@ss.hasPermi('stock:etf_price_alerts:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(etfPriceAlertsService.selectEtfPriceAlertsById(id));
    }

    /**
     * 新增ETF买入卖出价格提醒
     */
    @PreAuthorize("@ss.hasPermi('stock:etf_price_alerts:add')")
    @Log(title = "ETF买入卖出价格提醒", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody EtfPriceAlerts etfPriceAlerts) {
        return toAjax(etfPriceAlertsService.insertEtfPriceAlerts(etfPriceAlerts));
    }

    /**
     * 修改ETF买入卖出价格提醒
     */
    @PreAuthorize("@ss.hasPermi('stock:etf_price_alerts:edit')")
    @Log(title = "ETF买入卖出价格提醒", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody EtfPriceAlerts etfPriceAlerts) {
        return toAjax(etfPriceAlertsService.updateEtfPriceAlerts(etfPriceAlerts));
    }

    /**
     * 删除ETF买入卖出价格提醒
     */
    @PreAuthorize("@ss.hasPermi('stock:etf_price_alerts:remove')")
    @Log(title = "ETF买入卖出价格提醒", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(etfPriceAlertsService.deleteEtfPriceAlertsByIds(ids));
    }
}
