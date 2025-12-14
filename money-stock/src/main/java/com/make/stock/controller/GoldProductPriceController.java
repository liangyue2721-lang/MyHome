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
import com.make.stock.domain.GoldProductPrice;
import com.make.stock.service.IGoldProductPriceService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 黄金价格Controller
 *
 * @author erqi
 * @date 2025-05-28
 */
@RestController
@RequestMapping("/stock/gold_product_price")
public class GoldProductPriceController extends BaseController {

    @Autowired
    private IGoldProductPriceService goldProductPriceService;

    /**
     * 查询黄金价格列表
     */
    @PreAuthorize("@ss.hasPermi('stock:gold_product_price:list')")
    @GetMapping("/list")
    public TableDataInfo list(GoldProductPrice goldProductPrice) {
        startPage();
        List<GoldProductPrice> list = goldProductPriceService.selectGoldProductPriceList(goldProductPrice);
        return getDataTable(list);
    }

    /**
     * 导出黄金价格列表
     */
    @PreAuthorize("@ss.hasPermi('stock:gold_product_price:export')")
    @Log(title = "黄金价格", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, GoldProductPrice goldProductPrice) {
        List<GoldProductPrice> list = goldProductPriceService.selectGoldProductPriceList(goldProductPrice);
        ExcelUtil<GoldProductPrice> util = new ExcelUtil<GoldProductPrice>(GoldProductPrice.class);
        util.exportExcel(response, list, "黄金价格数据");
    }

    /**
     * 获取黄金价格详细信息
     */
    @PreAuthorize("@ss.hasPermi('stock:gold_product_price:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(goldProductPriceService.selectGoldProductPriceById(id));
    }

    /**
     * 新增黄金价格
     */
    @PreAuthorize("@ss.hasPermi('stock:gold_product_price:add')")
    @Log(title = "黄金价格", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody GoldProductPrice goldProductPrice) {
        return toAjax(goldProductPriceService.insertGoldProductPrice(goldProductPrice));
    }

    /**
     * 修改黄金价格
     */
    @PreAuthorize("@ss.hasPermi('stock:gold_product_price:edit')")
    @Log(title = "黄金价格", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody GoldProductPrice goldProductPrice) {
        return toAjax(goldProductPriceService.updateGoldProductPrice(goldProductPrice));
    }

    /**
     * 删除黄金价格
     */
    @PreAuthorize("@ss.hasPermi('stock:gold_product_price:remove')")
    @Log(title = "黄金价格", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(goldProductPriceService.deleteGoldProductPriceByIds(ids));
    }
}
