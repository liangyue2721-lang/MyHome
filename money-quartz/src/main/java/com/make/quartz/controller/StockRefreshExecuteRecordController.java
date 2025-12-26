package com.make.quartz.controller;

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
import com.make.quartz.domain.StockRefreshExecuteRecord;
import com.make.quartz.service.IStockRefreshExecuteRecordService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 刷新任务执行记录Controller
 *
 * @author erqi
 * @date 2025-12-26
 */
@RestController
@RequestMapping("/quartz/refresh_execute_record")
public class StockRefreshExecuteRecordController extends BaseController {

    @Autowired
    private IStockRefreshExecuteRecordService stockRefreshExecuteRecordService;

    /**
     * 查询刷新任务执行记录列表
     */
    @PreAuthorize("@ss.hasPermi('quartz:refresh_execute_record:list')")
    @GetMapping("/list")
    public TableDataInfo list(StockRefreshExecuteRecord stockRefreshExecuteRecord) {
        startPage();
        List<StockRefreshExecuteRecord> list = stockRefreshExecuteRecordService.selectStockRefreshExecuteRecordList(stockRefreshExecuteRecord);
        return getDataTable(list);
    }

    /**
     * 导出刷新任务执行记录列表
     */
    @PreAuthorize("@ss.hasPermi('quartz:refresh_execute_record:export')")
    @Log(title = "刷新任务执行记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StockRefreshExecuteRecord stockRefreshExecuteRecord) {
        List<StockRefreshExecuteRecord> list = stockRefreshExecuteRecordService.selectStockRefreshExecuteRecordList(stockRefreshExecuteRecord);
        ExcelUtil<StockRefreshExecuteRecord> util = new ExcelUtil<StockRefreshExecuteRecord>(StockRefreshExecuteRecord.class);
        util.exportExcel(response, list, "刷新任务执行记录数据");
    }

    /**
     * 获取刷新任务执行记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('quartz:refresh_execute_record:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id) {
        return success(stockRefreshExecuteRecordService.selectStockRefreshExecuteRecordById(id));
    }

    /**
     * 新增刷新任务执行记录
     */
    @PreAuthorize("@ss.hasPermi('quartz:refresh_execute_record:add')")
    @Log(title = "刷新任务执行记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody StockRefreshExecuteRecord stockRefreshExecuteRecord) {
        return toAjax(stockRefreshExecuteRecordService.insertStockRefreshExecuteRecord(stockRefreshExecuteRecord));
    }

    /**
     * 修改刷新任务执行记录
     */
    @PreAuthorize("@ss.hasPermi('quartz:refresh_execute_record:edit')")
    @Log(title = "刷新任务执行记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody StockRefreshExecuteRecord stockRefreshExecuteRecord) {
        return toAjax(stockRefreshExecuteRecordService.updateStockRefreshExecuteRecord(stockRefreshExecuteRecord));
    }

    /**
     * 删除刷新任务执行记录
     */
    @PreAuthorize("@ss.hasPermi('quartz:refresh_execute_record:remove')")
    @Log(title = "刷新任务执行记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(stockRefreshExecuteRecordService.deleteStockRefreshExecuteRecordByIds(ids));
    }
}
