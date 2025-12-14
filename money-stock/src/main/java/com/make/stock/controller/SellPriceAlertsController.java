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
import com.make.stock.domain.SellPriceAlerts;
import com.make.stock.service.ISellPriceAlertsService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 卖出价位提醒Controller
 *
 * @author 贰柒
 * @date 2025-05-28
 */
@RestController
@RequestMapping("/stock/sell_price_alerts")
public class SellPriceAlertsController extends BaseController {
    @Autowired
    private ISellPriceAlertsService sellPriceAlertsService;

    /**
     * 查询卖出价位提醒列表
     */
    @PreAuthorize("@ss.hasPermi('stock:sell_price_alerts:list')")
    @GetMapping("/list")
    public TableDataInfo list(SellPriceAlerts sellPriceAlerts) {
        startPage();
        List<SellPriceAlerts> list = sellPriceAlertsService.selectSellPriceAlertsList(sellPriceAlerts);
        return getDataTable(list);
    }

    /**
     * 导出卖出价位提醒列表
     */
    @PreAuthorize("@ss.hasPermi('stock:sell_price_alerts:export')")
    @Log(title = "卖出价位提醒", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SellPriceAlerts sellPriceAlerts) {
        List<SellPriceAlerts> list = sellPriceAlertsService.selectSellPriceAlertsList(sellPriceAlerts);
        ExcelUtil<SellPriceAlerts> util = new ExcelUtil<SellPriceAlerts>(SellPriceAlerts.class);
        util.exportExcel(response, list, "卖出价位提醒数据");
    }

    /**
     * 获取卖出价位提醒详细信息
     */
    @PreAuthorize("@ss.hasPermi('stock:sell_price_alerts:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(sellPriceAlertsService.selectSellPriceAlertsById(id));
    }

    /**
     * 新增卖出价位提醒
     */
    @PreAuthorize("@ss.hasPermi('stock:sell_price_alerts:add')")
    @Log(title = "卖出价位提醒", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SellPriceAlerts sellPriceAlerts) {
        return toAjax(sellPriceAlertsService.insertSellPriceAlerts(sellPriceAlerts));
    }

    /**
     * 修改卖出价位提醒
     */
    @PreAuthorize("@ss.hasPermi('stock:sell_price_alerts:edit')")
    @Log(title = "卖出价位提醒", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SellPriceAlerts sellPriceAlerts) {
        return toAjax(sellPriceAlertsService.updateSellPriceAlerts(sellPriceAlerts));
    }

    /**
     * 删除卖出价位提醒
     */
    @PreAuthorize("@ss.hasPermi('stock:sell_price_alerts:remove')")
    @Log(title = "卖出价位提醒", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(sellPriceAlertsService.deleteSellPriceAlertsByIds(ids));
    }
}
