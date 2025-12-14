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
import com.make.stock.domain.SalesData;
import com.make.stock.service.ISalesDataService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 利润折线图数据Controller
 *
 * @author erqi
 * @date 2025-05-28
 */
@RestController
@RequestMapping("/stock/sales_data")
public class SalesDataController extends BaseController {

    @Autowired
    private ISalesDataService salesDataService;

    /**
     * 查询利润折线图数据列表
     */
    @PreAuthorize("@ss.hasPermi('stock:sales_data:list')")
    @GetMapping("/list")
    public TableDataInfo list(SalesData salesData) {
        startPage();
        List<SalesData> list = salesDataService.selectSalesDataList(salesData);
        return getDataTable(list);
    }

    /**
     * 导出利润折线图数据列表
     */
    @PreAuthorize("@ss.hasPermi('stock:sales_data:export')")
    @Log(title = "利润折线图数据", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SalesData salesData) {
        List<SalesData> list = salesDataService.selectSalesDataList(salesData);
        ExcelUtil<SalesData> util = new ExcelUtil<SalesData>(SalesData.class);
        util.exportExcel(response, list, "利润折线图数据数据");
    }

    /**
     * 获取利润折线图数据详细信息
     */
    @PreAuthorize("@ss.hasPermi('stock:sales_data:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(salesDataService.selectSalesDataById(id));
    }

    /**
     * 新增利润折线图数据
     */
    @PreAuthorize("@ss.hasPermi('stock:sales_data:add')")
    @Log(title = "利润折线图数据", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SalesData salesData) {
        return toAjax(salesDataService.insertSalesData(salesData));
    }

    /**
     * 修改利润折线图数据
     */
    @PreAuthorize("@ss.hasPermi('stock:sales_data:edit')")
    @Log(title = "利润折线图数据", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SalesData salesData) {
        return toAjax(salesDataService.updateSalesData(salesData));
    }

    /**
     * 删除利润折线图数据
     */
    @PreAuthorize("@ss.hasPermi('stock:sales_data:remove')")
    @Log(title = "利润折线图数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(salesDataService.deleteSalesDataByIds(ids));
    }
}
