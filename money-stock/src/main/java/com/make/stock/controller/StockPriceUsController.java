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
import com.make.stock.domain.StockPriceUs;
import com.make.stock.service.IStockPriceUsService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 美股阶段行情信息Controller
 *
 * @author erqi
 * @date 2025-10-26
 */
@RestController
@RequestMapping("/stock/us")
public class StockPriceUsController extends BaseController {

    @Autowired
    private IStockPriceUsService stockPriceUsService;

/**
 * 查询美股阶段行情信息列表
 */
@PreAuthorize("@ss.hasPermi('stock:us:list')")
@GetMapping("/list")
    public TableDataInfo list(StockPriceUs stockPriceUs) {
        startPage();
        List<StockPriceUs> list = stockPriceUsService.selectStockPriceUsList(stockPriceUs);
        return getDataTable(list);
    }

    /**
     * 导出美股阶段行情信息列表
     */
    @PreAuthorize("@ss.hasPermi('stock:us:export')")
    @Log(title = "美股阶段行情信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StockPriceUs stockPriceUs) {
        List<StockPriceUs> list = stockPriceUsService.selectStockPriceUsList(stockPriceUs);
        ExcelUtil<StockPriceUs> util = new ExcelUtil<StockPriceUs>(StockPriceUs. class);
        util.exportExcel(response, list, "美股阶段行情信息数据");
    }

    /**
     * 获取美股阶段行情信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('stock:us:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(stockPriceUsService.selectStockPriceUsById(id));
    }

    /**
     * 新增美股阶段行情信息
     */
    @PreAuthorize("@ss.hasPermi('stock:us:add')")
    @Log(title = "美股阶段行情信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody StockPriceUs stockPriceUs) {
        return toAjax(stockPriceUsService.insertStockPriceUs(stockPriceUs));
    }

    /**
     * 修改美股阶段行情信息
     */
    @PreAuthorize("@ss.hasPermi('stock:us:edit')")
    @Log(title = "美股阶段行情信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody StockPriceUs stockPriceUs) {
        return toAjax(stockPriceUsService.updateStockPriceUs(stockPriceUs));
    }

    /**
     * 删除美股阶段行情信息
     */
    @PreAuthorize("@ss.hasPermi('stock:us:remove')")
    @Log(title = "美股阶段行情信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(stockPriceUsService.deleteStockPriceUsByIds(ids));
    }
}
