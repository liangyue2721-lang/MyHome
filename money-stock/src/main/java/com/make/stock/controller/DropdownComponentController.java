package com.make.stock.controller;

import com.alibaba.fastjson2.JSON;
import com.make.common.core.controller.BaseController;
import com.make.common.core.domain.entity.SysUser;
import com.make.common.core.page.TableDataInfo;
import com.make.system.service.ISysUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 费用缴纳记录Controller
 *
 * @author 贰柒
 * @date 2025-03-08
 */
@RestController
@RequestMapping("/stock/dropdownComponentCont")
public class DropdownComponentController extends BaseController {

    @Resource
    private ISysUserService userService;

    /**
     * 获取全部用户列表
     */
//    @PreAuthorize("@ss.hasPermi('stock:payments:list')")
    @GetMapping("/listUser")
    public TableDataInfo list(SysUser user) {
        //确保分页处理
//        startPage();
        // 新建 params 空值，无意义内容
        user.setParams(Collections.emptyMap());
        List<SysUser> list = userService.queryUserAll(user);
        return getDataTable(list);
    }

}
