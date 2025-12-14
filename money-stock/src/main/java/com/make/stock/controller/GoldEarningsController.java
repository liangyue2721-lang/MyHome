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
import com.make.stock.domain.GoldEarnings;
import com.make.stock.service.IGoldEarningsService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 攒金收益记录Controller
 *
 * @author erqi
 * @date 2025-05-28
 */
@RestController
@RequestMapping("/stock/gold_earnings")
public class GoldEarningsController extends BaseController {

    @Autowired
    private IGoldEarningsService goldEarningsService;

    /**
     * 查询攒金收益记录列表
     */
    @PreAuthorize("@ss.hasPermi('stock:gold_earnings:list')")
    @GetMapping("/list")
    public TableDataInfo list(GoldEarnings goldEarnings) {
        startPage();
        List<GoldEarnings> list = goldEarningsService.selectGoldEarningsList(goldEarnings);
        return getDataTable(list);
    }

    /**
     * 导出攒金收益记录列表
     */
    @PreAuthorize("@ss.hasPermi('stock:gold_earnings:export')")
    @Log(title = "攒金收益记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, GoldEarnings goldEarnings) {
        List<GoldEarnings> list = goldEarningsService.selectGoldEarningsList(goldEarnings);
        ExcelUtil<GoldEarnings> util = new ExcelUtil<GoldEarnings>(GoldEarnings.class);
        util.exportExcel(response, list, "攒金收益记录数据");
    }

    /**
     * 获取攒金收益记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('stock:gold_earnings:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(goldEarningsService.selectGoldEarningsById(id));
    }

    /**
     * 新增攒金收益记录
     */
    @PreAuthorize("@ss.hasPermi('stock:gold_earnings:add')")
    @Log(title = "攒金收益记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody GoldEarnings goldEarnings) {
        return toAjax(goldEarningsService.insertGoldEarnings(goldEarnings));
    }

    /**
     * 修改攒金收益记录
     */
    @PreAuthorize("@ss.hasPermi('stock:gold_earnings:edit')")
    @Log(title = "攒金收益记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody GoldEarnings goldEarnings) {
        return toAjax(goldEarningsService.updateGoldEarnings(goldEarnings));
    }

    /**
     * 删除攒金收益记录
     */
    @PreAuthorize("@ss.hasPermi('stock:gold_earnings:remove')")
    @Log(title = "攒金收益记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(goldEarningsService.deleteGoldEarningsByIds(ids));
    }
}
