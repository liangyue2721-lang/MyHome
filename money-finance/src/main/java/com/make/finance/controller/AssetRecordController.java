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
import com.make.finance.domain.AssetRecord;
import com.make.finance.service.IAssetRecordService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 个人资产明细Controller
 *
 * @author 贰柒
 * @date 2025-05-28
 */
@RestController
@RequestMapping("/finance/asset_record")
public class AssetRecordController extends BaseController {
    @Autowired
    private IAssetRecordService assetRecordService;

    /**
     * 查询个人资产明细列表
     */
    @PreAuthorize("@ss.hasPermi('finance:asset_record:list')")
    @GetMapping("/list")
    public TableDataInfo list(AssetRecord assetRecord) {
        startPage();
        List<AssetRecord> list = assetRecordService.selectAssetRecordList(assetRecord);
        return getDataTable(list);
    }

    /**
     * 导出个人资产明细列表
     */
    @PreAuthorize("@ss.hasPermi('finance:asset_record:export')")
    @Log(title = "个人资产明细", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AssetRecord assetRecord) {
        List<AssetRecord> list = assetRecordService.selectAssetRecordList(assetRecord);
        ExcelUtil<AssetRecord> util = new ExcelUtil<AssetRecord>(AssetRecord.class);
        util.exportExcel(response, list, "个人资产明细数据");
    }

    /**
     * 获取个人资产明细详细信息
     */
    @PreAuthorize("@ss.hasPermi('finance:asset_record:query')")
    @GetMapping(value = "/{assetId}")
    public AjaxResult getInfo(@PathVariable("assetId") Long assetId) {
        return success(assetRecordService.selectAssetRecordByAssetId(assetId));
    }

    /**
     * 新增个人资产明细
     */
    @PreAuthorize("@ss.hasPermi('finance:asset_record:add')")
    @Log(title = "个人资产明细", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AssetRecord assetRecord) {
        return toAjax(assetRecordService.insertAssetRecord(assetRecord));
    }

    /**
     * 修改个人资产明细
     */
    @PreAuthorize("@ss.hasPermi('finance:asset_record:edit')")
    @Log(title = "个人资产明细", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AssetRecord assetRecord) {
        return toAjax(assetRecordService.updateAssetRecord(assetRecord));
    }

    /**
     * 删除个人资产明细
     */
    @PreAuthorize("@ss.hasPermi('finance:asset_record:remove')")
    @Log(title = "个人资产明细", businessType = BusinessType.DELETE)
    @DeleteMapping("/{assetIds}")
    public AjaxResult remove(@PathVariable Long[] assetIds) {
        return toAjax(assetRecordService.deleteAssetRecordByAssetIds(assetIds));
    }

    /**
     * 获取资产类型
     */
    @GetMapping("/getAssetType")
    public AjaxResult getAssetType() {
        return new AjaxResult(200, "成功", assetRecordService.getAssetType());
    }

    /**
     * 获取资产状态
     */
    @GetMapping("/getAssetStatus")
    public AjaxResult getAssetStatus() {
        return new AjaxResult(200, "成功", assetRecordService.getAssetStatus());
    }

    /**
     * 获取资产扇形图
     */
    @GetMapping("/getRecordColumnChart/{userId}")
    public AjaxResult getRecordColumnChart(@PathVariable Long userId) {
        if (userId == null) {
            return new AjaxResult(100, "失败", "userID is empty");
        }
        return new AjaxResult(200, "成功", assetRecordService.getRecordColumnChart(userId));
    }



}
