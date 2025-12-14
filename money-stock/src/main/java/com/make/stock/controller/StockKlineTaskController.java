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
import com.make.stock.domain.StockKlineTask;
import com.make.stock.service.IStockKlineTaskService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 股票K线数据任务Controller
 *
 * @author erqi
 * @date 2025-11-03
 */
@RestController
@RequestMapping("/stock/kline_task")
public class StockKlineTaskController extends BaseController {

    @Autowired
    private IStockKlineTaskService stockKlineTaskService;

/**
 * 查询股票K线数据任务列表
 */
@PreAuthorize("@ss.hasPermi('stock:kline_task:list')")
@GetMapping("/list")
    public TableDataInfo list(StockKlineTask stockKlineTask) {
        startPage();
        List<StockKlineTask> list = stockKlineTaskService.selectStockKlineTaskList(stockKlineTask);
        return getDataTable(list);
    }

    /**
     * 导出股票K线数据任务列表
     */
    @PreAuthorize("@ss.hasPermi('stock:kline_task:export')")
    @Log(title = "股票K线数据任务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StockKlineTask stockKlineTask) {
        List<StockKlineTask> list = stockKlineTaskService.selectStockKlineTaskList(stockKlineTask);
        ExcelUtil<StockKlineTask> util = new ExcelUtil<StockKlineTask>(StockKlineTask. class);
        util.exportExcel(response, list, "股票K线数据任务数据");
    }

    /**
     * 获取股票K线数据任务详细信息
     */
    @PreAuthorize("@ss.hasPermi('stock:kline_task:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id) {
        return success(stockKlineTaskService.selectStockKlineTaskById(id));
    }

    /**
     * 新增股票K线数据任务
     */
    @PreAuthorize("@ss.hasPermi('stock:kline_task:add')")
    @Log(title = "股票K线数据任务", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody StockKlineTask stockKlineTask) {
        return toAjax(stockKlineTaskService.insertStockKlineTask(stockKlineTask));
    }

    /**
     * 修改股票K线数据任务
     */
    @PreAuthorize("@ss.hasPermi('stock:kline_task:edit')")
    @Log(title = "股票K线数据任务", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody StockKlineTask stockKlineTask) {
        return toAjax(stockKlineTaskService.updateStockKlineTask(stockKlineTask));
    }

    /**
     * 删除股票K线数据任务
     */
    @PreAuthorize("@ss.hasPermi('stock:kline_task:remove')")
    @Log(title = "股票K线数据任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(stockKlineTaskService.deleteStockKlineTaskByIds(ids));
    }
}
