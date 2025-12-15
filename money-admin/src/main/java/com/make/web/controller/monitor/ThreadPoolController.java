package com.make.web.controller.monitor;

import com.make.common.core.domain.AjaxResult;
import com.make.framework.config.ThreadPoolMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 线程池监控控制器
 * 提供对系统线程池状态的监控功能
 *
 * @author make
 */
@RestController
@RequestMapping("/monitor/threadPool")
public class ThreadPoolController {

    @Autowired
    private ThreadPoolMonitor threadPoolMonitor;

    /**
     * 获取线程池状态信息
     *
     * @return 线程池状态信息
     */
    @PreAuthorize("@ss.hasPermi('monitor:threadPool:list')")
    @GetMapping()
    public AjaxResult getInfo() {
        List<Map<String, Object>> info = threadPoolMonitor.getThreadPoolInfo();
        return AjaxResult.success(info);
    }
}