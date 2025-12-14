package com.make.flowable.common.expand.el;

import com.make.system.service.ISysDeptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Flowable流程引擎扩展表达式实现类
 * <p>
 * 该类提供了Flowable流程定义中可以使用的自定义表达式方法，
 * 可在流程定义的表达式中通过#{flowEl.methodName()}方式调用
 * </p>
 *
 * @author 27
 * @date 2023-03-04 12:10
 * @see BaseEl
 */
@Component
@Slf4j
public class FlowEl implements BaseEl {

    /**
     * 注入系统部门服务接口，用于获取部门相关信息
     */
    @Resource
    private ISysDeptService sysDeptService;

    /**
     * 查找部门领导方法
     * <p>
     * 该方法用于在流程定义中根据部门名称查找对应的部门领导
     * </p>
     *
     * @param name 部门名称
     * @return 部门领导名称
     */
    public String findDeptLeader(String name){
        // 记录开始查询表达式变量值的日志
        log.info("开始查询表达式变量值,findDeptLeader: {}", name);
        // 直接返回传入的名称参数（实际应用中应该根据部门名称查询对应的领导）
        return name;
    }

    /**
     * 获取名称方法
     * <p>
     * 该方法用于在流程定义中获取指定的名称值
     * </p>
     *
     * @param name 输入的名称参数
     * @return 返回传入的名称参数
     */
    public String getName(String name){
        // 记录开始查询表达式变量值的日志
        log.info("开始查询表达式变量值,getName: {}", name);
        // 直接返回传入的名称参数
        return name;
    }
}