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
import com.make.finance.domain.ServerInfo;
import com.make.finance.service.IServerInfoService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 服务器有效期管理（MySQL5.7兼容版）Controller
 *
 * @author erqi
 * @date 2025-10-15
 */
@RestController
@RequestMapping("/finance/serverInfo")
public class ServerInfoController extends BaseController {

    @Autowired
    private IServerInfoService serverInfoService;

    /**
     * 查询服务器有效期管理（MySQL5.7兼容版）列表
     */
    @PreAuthorize("@ss.hasPermi('finance:serverInfo:list')")
    @GetMapping("/list")
    public TableDataInfo list(ServerInfo serverInfo) {
        startPage();
        List<ServerInfo> list = serverInfoService.selectServerInfoList(serverInfo);
        return getDataTable(list);
    }

    /**
     * 导出服务器有效期管理（MySQL5.7兼容版）列表
     */
    @PreAuthorize("@ss.hasPermi('finance:serverInfo:export')")
    @Log(title = "服务器有效期管理（MySQL5.7兼容版）", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, ServerInfo serverInfo) {
        List<ServerInfo> list = serverInfoService.selectServerInfoList(serverInfo);
        ExcelUtil<ServerInfo> util = new ExcelUtil<ServerInfo>(ServerInfo.class);
        util.exportExcel(response, list, "服务器有效期管理（MySQL5.7兼容版）数据");
    }

    /**
     * 获取服务器有效期管理（MySQL5.7兼容版）详细信息
     */
    @PreAuthorize("@ss.hasPermi('finance:serverInfo:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(serverInfoService.selectServerInfoById(id));
    }

    /**
     * 新增服务器有效期管理（MySQL5.7兼容版）
     */
    @PreAuthorize("@ss.hasPermi('finance:serverInfo:add')")
    @Log(title = "服务器有效期管理（MySQL5.7兼容版）", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ServerInfo serverInfo) {
        return toAjax(serverInfoService.insertServerInfo(serverInfo));
    }

    /**
     * 修改服务器有效期管理（MySQL5.7兼容版）
     */
    @PreAuthorize("@ss.hasPermi('finance:serverInfo:edit')")
    @Log(title = "服务器有效期管理（MySQL5.7兼容版）", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ServerInfo serverInfo) {
        return toAjax(serverInfoService.updateServerInfo(serverInfo));
    }

    /**
     * 删除服务器有效期管理（MySQL5.7兼容版）
     */
    @PreAuthorize("@ss.hasPermi('finance:serverInfo:remove')")
    @Log(title = "服务器有效期管理（MySQL5.7兼容版）", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(serverInfoService.deleteServerInfoByIds(ids));
    }
}
