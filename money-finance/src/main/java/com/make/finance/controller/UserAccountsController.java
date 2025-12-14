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
import com.make.finance.domain.UserAccounts;
import com.make.finance.service.IUserAccountsService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;

/**
 * 用户账户银行卡信息Controller
 *
 * @author erqi
 * @date 2025-06-03
 */
@RestController
@RequestMapping("/finance/user_accounts")
public class UserAccountsController extends BaseController {

    @Autowired
    private IUserAccountsService userAccountsService;

/**
 * 查询用户账户银行卡信息列表
 */
@PreAuthorize("@ss.hasPermi('finance:user_accounts:list')")
@GetMapping("/list")
    public TableDataInfo list(UserAccounts userAccounts) {
        startPage();
        List<UserAccounts> list = userAccountsService.selectUserAccountsList(userAccounts);
        return getDataTable(list);
    }

    /**
     * 导出用户账户银行卡信息列表
     */
    @PreAuthorize("@ss.hasPermi('finance:user_accounts:export')")
    @Log(title = "用户账户银行卡信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, UserAccounts userAccounts) {
        List<UserAccounts> list = userAccountsService.selectUserAccountsList(userAccounts);
        ExcelUtil<UserAccounts> util = new ExcelUtil<UserAccounts>(UserAccounts. class);
        util.exportExcel(response, list, "用户账户银行卡信息数据");
    }

    /**
     * 获取用户账户银行卡信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('finance:user_accounts:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(userAccountsService.selectUserAccountsById(id));
    }

    /**
     * 新增用户账户银行卡信息
     */
    @PreAuthorize("@ss.hasPermi('finance:user_accounts:add')")
    @Log(title = "用户账户银行卡信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody UserAccounts userAccounts) {
        return toAjax(userAccountsService.insertUserAccounts(userAccounts));
    }

    /**
     * 修改用户账户银行卡信息
     */
    @PreAuthorize("@ss.hasPermi('finance:user_accounts:edit')")
    @Log(title = "用户账户银行卡信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody UserAccounts userAccounts) {
        return toAjax(userAccountsService.updateUserAccounts(userAccounts));
    }

    /**
     * 删除用户账户银行卡信息
     */
    @PreAuthorize("@ss.hasPermi('finance:user_accounts:remove')")
    @Log(title = "用户账户银行卡信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(userAccountsService.deleteUserAccountsByIds(ids));
    }
}
