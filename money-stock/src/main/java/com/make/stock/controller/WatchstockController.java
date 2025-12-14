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
import com.make.stock.domain.Watchstock;
import com.make.stock.service.IWatchstockService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 买入价位提醒Controller
 *
 * @author erqi
 * @date 2025-05-28
 */
@RestController
@RequestMapping("/stock/watch_stock")
public class WatchstockController extends BaseController {
    @Autowired
    private IWatchstockService watchstockService;

    /**
     * 查询买入价位提醒列表
     */
    @PreAuthorize("@ss.hasPermi('stock:watch_stock:list')")
    @GetMapping("/list")
    public TableDataInfo list(Watchstock watchstock) {
        startPage();
        List<Watchstock> list = watchstockService.selectWatchstockList(watchstock);
        return getDataTable(list);
    }

    /**
     * 导出买入价位提醒列表
     */
    @PreAuthorize("@ss.hasPermi('stock:watch_stock:export')")
    @Log(title = "买入价位提醒", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Watchstock watchstock) {
        List<Watchstock> list = watchstockService.selectWatchstockList(watchstock);
        ExcelUtil<Watchstock> util = new ExcelUtil<Watchstock>(Watchstock.class);
        util.exportExcel(response, list, "买入价位提醒数据");
    }

    /**
     * 获取买入价位提醒详细信息
     */
    @PreAuthorize("@ss.hasPermi('stock:watch_stock:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(watchstockService.selectWatchstockById(id));
    }

    /**
     * 新增买入价位提醒
     */
    @PreAuthorize("@ss.hasPermi('stock:watch_stock:add')")
    @Log(title = "买入价位提醒", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Watchstock watchstock) {
        return toAjax(watchstockService.insertWatchstock(watchstock));
    }

    /**
     * 修改买入价位提醒
     */
    @PreAuthorize("@ss.hasPermi('stock:watch_stock:edit')")
    @Log(title = "买入价位提醒", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Watchstock watchstock) {
        return toAjax(watchstockService.updateWatchstock(watchstock));
    }

    /**
     * 删除买入价位提醒
     */
    @PreAuthorize("@ss.hasPermi('stock:watch_stock:remove')")
    @Log(title = "买入价位提醒", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(watchstockService.deleteWatchstockByIds(ids));
    }
}
