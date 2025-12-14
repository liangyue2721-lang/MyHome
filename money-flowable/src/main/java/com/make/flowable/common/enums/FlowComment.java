package com.make.flowable.common.enums;

/**
 * Flowable流程意见类型枚举类
 * <p>
 * 该枚举定义了在Flowable工作流处理过程中可能产生的各种操作意见类型，
 * 用于在流程任务处理时记录相应的操作类型和说明信息
 * </p>
 *
 * @author 27
 * @date 2021/4/19
 */
public enum FlowComment {

    /**
     * 正常意见类型
     * <p>
     * 表示正常的流程审批意见，无特殊操作
     * </p>
     */
    NORMAL("1", "正常意见"),
    
    /**
     * 退回意见类型
     * <p>
     * 表示将流程任务退回至上一节点的操作意见
     * </p>
     */
    REBACK("2", "退回意见"),
    
    /**
     * 驳回意见类型
     * <p>
     * 表示将流程任务驳回至发起人或指定节点的操作意见
     * </p>
     */
    REJECT("3", "驳回意见"),
    
    /**
     * 委派意见类型
     * <p>
     * 表示将流程任务委派给其他人处理的操作意见
     * </p>
     */
    DELEGATE("4", "委派意见"),
    
    /**
     * 转办意见类型
     * <p>
     * 表示将流程任务转办给其他人处理的操作意见
     * </p>
     */
    ASSIGN("5", "转办意见"),
    
    /**
     * 终止流程意见类型
     * <p>
     * 表示终止整个流程实例的操作意见
     * </p>
     */
    STOP("6", "终止流程");

    /**
     * 意见类型编码
     * <p>
     * 用于在系统中标识不同意见类型的唯一编码
     * </p>
     */
    private final String type;

    /**
     * 意见类型说明
     * <p>
     * 用于向用户展示意见类型的中文说明信息
     * </p>
     */
    private final String remark;

    /**
     * 构造函数，初始化枚举实例
     *
     * @param type   意见类型编码
     * @param remark 意见类型说明
     */
    FlowComment(String type, String remark) {
        // 初始化意见类型编码
        this.type = type;
        // 初始化意见类型说明
        this.remark = remark;
    }

    /**
     * 获取意见类型编码
     *
     * @return 意见类型编码
     */
    public String getType() {
        return type;
    }

    /**
     * 获取意见类型说明
     *
     * @return 意见类型说明
     */
    public String getRemark() {
        return remark;
    }
}