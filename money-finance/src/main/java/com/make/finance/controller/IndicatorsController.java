package com.make.finance.controller;

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
import com.make.finance.domain.Indicators;
import com.make.finance.service.IIndicatorsService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 指标信息Controller
 *
 * @author 贰柒
 * @date 2025-05-28
 */
@RestController
@RequestMapping("/finance/indicators")
public class IndicatorsController extends BaseController {
    @Autowired
    private IIndicatorsService indicatorsService;

    /**
     * 查询指标信息列表
     */
    @PreAuthorize("@ss.hasPermi('finance:indicators:list')")
    @GetMapping("/list")
    public TableDataInfo list(Indicators indicators) {
        startPage();
        List<Indicators> list = indicatorsService.selectIndicatorsList(indicators);
        return getDataTable(list);
    }

    /**
     * 导出指标信息列表
     */
    @PreAuthorize("@ss.hasPermi('finance:indicators:export')")
    @Log(title = "指标信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Indicators indicators) {
        List<Indicators> list = indicatorsService.selectIndicatorsList(indicators);
        ExcelUtil<Indicators> util = new ExcelUtil<Indicators>(Indicators.class);
        util.exportExcel(response, list, "指标信息数据");
    }

    /**
     * 获取指标信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('finance:indicators:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(indicatorsService.selectIndicatorsById(id));
    }

    /**
     * 新增指标信息
     */
    @PreAuthorize("@ss.hasPermi('finance:indicators:add')")
    @Log(title = "指标信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Indicators indicators) {
        return toAjax(indicatorsService.insertIndicators(indicators));
    }

    /**
     * 修改指标信息
     */
    @PreAuthorize("@ss.hasPermi('finance:indicators:edit')")
    @Log(title = "指标信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Indicators indicators) {
        return toAjax(indicatorsService.updateIndicators(indicators));
    }

    /**
     * 删除指标信息
     */
    @PreAuthorize("@ss.hasPermi('finance:indicators:remove')")
    @Log(title = "指标信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(indicatorsService.deleteIndicatorsByIds(ids));
    }
}
