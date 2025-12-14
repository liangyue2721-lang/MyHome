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
import com.make.stock.domain.EtfData;
import com.make.stock.service.IEtfDataService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * ETF交易数据Controller
 *
 * @author erqi
 * @date 2025-05-28
 */
@RestController
@RequestMapping("/stock/etf_data")
public class EtfDataController extends BaseController {

    @Autowired
    private IEtfDataService etfDataService;

    /**
     * 查询ETF交易数据列表
     */
    @PreAuthorize("@ss.hasPermi('stock:etf_data:list')")
    @GetMapping("/list")
    public TableDataInfo list(EtfData etfData) {
        startPage();
        List<EtfData> list = etfDataService.selectEtfDataList(etfData);
        return getDataTable(list);
    }

    /**
     * 导出ETF交易数据列表
     */
    @PreAuthorize("@ss.hasPermi('stock:etf_data:export')")
    @Log(title = "ETF交易数据", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, EtfData etfData) {
        List<EtfData> list = etfDataService.selectEtfDataList(etfData);
        ExcelUtil<EtfData> util = new ExcelUtil<EtfData>(EtfData.class);
        util.exportExcel(response, list, "ETF交易数据数据");
    }

    /**
     * 获取ETF交易数据详细信息
     */
    @PreAuthorize("@ss.hasPermi('stock:etf_data:query')")
    @GetMapping(value = "/{etfCode}")
    public AjaxResult getInfo(@PathVariable("etfCode") String etfCode) {
        return success(etfDataService.selectEtfDataByEtfCode(etfCode));
    }

    /**
     * 新增ETF交易数据
     */
    @PreAuthorize("@ss.hasPermi('stock:etf_data:add')")
    @Log(title = "ETF交易数据", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody EtfData etfData) {
        return toAjax(etfDataService.insertEtfData(etfData));
    }

    /**
     * 修改ETF交易数据
     */
    @PreAuthorize("@ss.hasPermi('stock:etf_data:edit')")
    @Log(title = "ETF交易数据", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody EtfData etfData) {
        return toAjax(etfDataService.updateEtfData(etfData));
    }

    /**
     * 删除ETF交易数据
     */
    @PreAuthorize("@ss.hasPermi('stock:etf_data:remove')")
    @Log(title = "ETF交易数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/{etfCodes}")
    public AjaxResult remove(@PathVariable String[] etfCodes) {
        return toAjax(etfDataService.deleteEtfDataByEtfCodes(etfCodes));
    }
}
