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
import com.make.stock.domain.EtfSalesData;
import com.make.stock.service.IEtfSalesDataService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * eft折线图数据Controller
 *
 * @author erqi
 * @date 2025-05-28
 */
@RestController
@RequestMapping("/stock/etf_sales_data")
public class EtfSalesDataController extends BaseController {

    @Autowired
    private IEtfSalesDataService etfSalesDataService;

    /**
     * 查询eft折线图数据列表
     */
    @PreAuthorize("@ss.hasPermi('stock:etf_sales_data:list')")
    @GetMapping("/list")
    public TableDataInfo list(EtfSalesData etfSalesData) {
        startPage();
        List<EtfSalesData> list = etfSalesDataService.selectEtfSalesDataList(etfSalesData);
        return getDataTable(list);
    }

    /**
     * 导出eft折线图数据列表
     */
    @PreAuthorize("@ss.hasPermi('stock:etf_sales_data:export')")
    @Log(title = "eft折线图数据", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, EtfSalesData etfSalesData) {
        List<EtfSalesData> list = etfSalesDataService.selectEtfSalesDataList(etfSalesData);
        ExcelUtil<EtfSalesData> util = new ExcelUtil<EtfSalesData>(EtfSalesData.class);
        util.exportExcel(response, list, "eft折线图数据数据");
    }

    /**
     * 获取eft折线图数据详细信息
     */
    @PreAuthorize("@ss.hasPermi('stock:etf_sales_data:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(etfSalesDataService.selectEtfSalesDataById(id));
    }

    /**
     * 新增eft折线图数据
     */
    @PreAuthorize("@ss.hasPermi('stock:etf_sales_data:add')")
    @Log(title = "eft折线图数据", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody EtfSalesData etfSalesData) {
        return toAjax(etfSalesDataService.insertEtfSalesData(etfSalesData));
    }

    /**
     * 修改eft折线图数据
     */
    @PreAuthorize("@ss.hasPermi('stock:etf_sales_data:edit')")
    @Log(title = "eft折线图数据", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody EtfSalesData etfSalesData) {
        return toAjax(etfSalesDataService.updateEtfSalesData(etfSalesData));
    }

    /**
     * 删除eft折线图数据
     */
    @PreAuthorize("@ss.hasPermi('stock:etf_sales_data:remove')")
    @Log(title = "eft折线图数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(etfSalesDataService.deleteEtfSalesDataByIds(ids));
    }
}
