package com.make.web.controller.system;

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
import com.make.system.domain.CacheMetadata;
import com.make.system.service.ICacheMetadataService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 缓存元数据Controller
 *
 * @author erqi
 * @date 2025-05-29
 */
@RestController
@RequestMapping("/system/cache_metadata")
public class CacheMetadataController extends BaseController {

    @Autowired
    private ICacheMetadataService cacheMetadataService;

    /**
     * 查询缓存元数据列表
     */
    @PreAuthorize("@ss.hasPermi('system:cache_metadata:list')")
    @GetMapping("/list")
    public TableDataInfo list(CacheMetadata cacheMetadata) {
        startPage();
        List<CacheMetadata> list = cacheMetadataService.selectCacheMetadataList(cacheMetadata);
        return getDataTable(list);
    }

    /**
     * 导出缓存元数据列表
     */
    @PreAuthorize("@ss.hasPermi('system:cache_metadata:export')")
    @Log(title = "缓存元数据", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CacheMetadata cacheMetadata) {
        List<CacheMetadata> list = cacheMetadataService.selectCacheMetadataList(cacheMetadata);
        ExcelUtil<CacheMetadata> util = new ExcelUtil<CacheMetadata>(CacheMetadata.class);
        util.exportExcel(response, list, "缓存元数据数据");
    }

    /**
     * 获取缓存元数据详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:cache_metadata:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(cacheMetadataService.selectCacheMetadataById(id));
    }

    /**
     * 新增缓存元数据
     */
    @PreAuthorize("@ss.hasPermi('system:cache_metadata:add')")
    @Log(title = "缓存元数据", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CacheMetadata cacheMetadata) {
        return toAjax(cacheMetadataService.insertCacheMetadata(cacheMetadata));
    }

    /**
     * 修改缓存元数据
     */
    @PreAuthorize("@ss.hasPermi('system:cache_metadata:edit')")
    @Log(title = "缓存元数据", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CacheMetadata cacheMetadata) {
        return toAjax(cacheMetadataService.updateCacheMetadata(cacheMetadata));
    }

    /**
     * 删除缓存元数据
     */
    @PreAuthorize("@ss.hasPermi('system:cache_metadata:remove')")
    @Log(title = "缓存元数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(cacheMetadataService.deleteCacheMetadataByIds(ids));
    }
}
