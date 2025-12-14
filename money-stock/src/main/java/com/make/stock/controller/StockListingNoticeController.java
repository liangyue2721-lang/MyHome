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
import com.make.stock.domain.StockListingNotice;
import com.make.stock.service.IStockListingNoticeService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 证券上市通知Controller
 *
 * @author erqi
 * @date 2025-07-31
 */
@RestController
@RequestMapping("/stock/notice")
public class StockListingNoticeController extends BaseController {

    @Autowired
    private IStockListingNoticeService stockListingNoticeService;

    /**
     * 查询证券上市通知列表
     */
    @PreAuthorize("@ss.hasPermi('stock:notice:list')")
    @GetMapping("/list")
    public TableDataInfo list(StockListingNotice stockListingNotice) {
        startPage();
        List<StockListingNotice> list = stockListingNoticeService.selectStockListingNoticeList(stockListingNotice);
        return getDataTable(list);
    }

    /**
     * 导出证券上市通知列表
     */
    @PreAuthorize("@ss.hasPermi('stock:notice:export')")
    @Log(title = "证券上市通知", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, StockListingNotice stockListingNotice) {
        List<StockListingNotice> list = stockListingNoticeService.selectStockListingNoticeList(stockListingNotice);
        ExcelUtil<StockListingNotice> util = new ExcelUtil<StockListingNotice>(StockListingNotice.class);
        util.exportExcel(response, list, "证券上市通知数据");
    }

    /**
     * 获取证券上市通知详细信息
     */
    @PreAuthorize("@ss.hasPermi('stock:notice:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id) {
        return success(stockListingNoticeService.selectStockListingNoticeById(id));
    }

    /**
     * 新增证券上市通知
     */
    @PreAuthorize("@ss.hasPermi('stock:notice:add')")
    @Log(title = "证券上市通知", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody StockListingNotice stockListingNotice) {
        return toAjax(stockListingNoticeService.insertStockListingNotice(stockListingNotice));
    }

    /**
     * 修改证券上市通知
     */
    @PreAuthorize("@ss.hasPermi('stock:notice:edit')")
    @Log(title = "证券上市通知", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody StockListingNotice stockListingNotice) {
        return toAjax(stockListingNoticeService.updateStockListingNotice(stockListingNotice));
    }

    /**
     * 删除证券上市通知
     */
    @PreAuthorize("@ss.hasPermi('stock:notice:remove')")
    @Log(title = "证券上市通知", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) {
        return toAjax(stockListingNoticeService.deleteStockListingNoticeByIds(ids));
    }
}
