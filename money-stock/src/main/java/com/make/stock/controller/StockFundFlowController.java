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
import com.make.stock.domain.StockFundFlow;
import com.make.stock.service.IStockFundFlowService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 主力资金流向Controller
 *
 * @author erqi
 * @date 2026-05-07
 */
@RestController
@RequestMapping("/stock/stockFlow")
public class StockFundFlowController extends BaseController {

    @Autowired
    private IStockFundFlowService stockFundFlowService;

    /**
     * 查询主力资金流向列表
     */
    @PreAuthorize("@ss.hasPermi('stock:stockFlow:list')")
    @GetMapping("/list")
    public TableDataInfo list(StockFundFlow stockFundFlow) {
        startPage();
        List<StockFundFlow> list = stockFundFlowService.selectStockFundFlowList(stockFundFlow);
        return getDataTable(list);
    }

    /**
     * 获取主力资金流向图表数据
     */
    @PreAuthorize("@ss.hasPermi('stock:stockFlow:list')")
    @GetMapping("/chartData")
    public AjaxResult getChartData() {
        return success(stockFundFlowService.selectChartData());
    }

    /**
     * 导出主力资金流向列表
     */
    @PreAuthorize("@ss.hasPermi('stock:stockFlow:export')")
    @Log(title = "主力资金流向", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StockFundFlow stockFundFlow) {
        List<StockFundFlow> list = stockFundFlowService.selectStockFundFlowList(stockFundFlow);
        ExcelUtil<StockFundFlow> util = new ExcelUtil<StockFundFlow>(StockFundFlow.class);
        util.exportExcel(response, list, "主力资金流向数据");
    }

    /**
     * 获取主力资金流向详细信息
     */
    @PreAuthorize("@ss.hasPermi('stock:stockFlow:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id) {
        return success(stockFundFlowService.selectStockFundFlowById(id));
    }

    /**
     * 新增主力资金流向
     */
    @PreAuthorize("@ss.hasPermi('stock:stockFlow:add')")
    @Log(title = "主力资金流向", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody StockFundFlow stockFundFlow) {
        return toAjax(stockFundFlowService.insertStockFundFlow(stockFundFlow));
    }

    /**
     * 修改主力资金流向
     */
    @PreAuthorize("@ss.hasPermi('stock:stockFlow:edit')")
    @Log(title = "主力资金流向", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody StockFundFlow stockFundFlow) {
        return toAjax(stockFundFlowService.updateStockFundFlow(stockFundFlow));
    }

    /**
     * 删除主力资金流向
     */
    @PreAuthorize("@ss.hasPermi('stock:stockFlow:remove')")
    @Log(title = "主力资金流向", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(stockFundFlowService.deleteStockFundFlowByIds(ids));
    }
}
