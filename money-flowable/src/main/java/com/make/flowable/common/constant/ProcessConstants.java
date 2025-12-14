package com.make.flowable.common.constant;

/**
 * Flowable流程引擎相关常量定义类
 * <p>
 * 该类定义了在Flowable工作流引擎中使用的所有常量，
 * 包括任务分配类型、流程变量名称、命名空间等
 * </p>
 *
 * @author 27
 * @date 2021/4/17 22:46
 */
public class ProcessConstants {

    /**
     * 动态数据标识
     * <p>
     * 用于标识流程任务分配类型为动态分配，即在流程运行时动态指定任务处理人
     * </p>
     */
    public static final String DYNAMIC = "dynamic";

    /**
     * 固定任务接收标识
     * <p>
     * 用于标识流程任务分配类型为固定分配，即在流程定义时已指定固定的任务处理人
     * </p>
     */
    public static final String FIXED = "fixed";

    /**
     * 单个审批人标识
     * <p>
     * 用于标识流程任务只分配给单个具体的审批人处理
     * </p>
     */
    public static final String ASSIGNEE = "assignee";


    /**
     * 候选人标识
     * <p>
     * 用于标识流程任务可以由多个候选人中的任意一人处理
     * </p>
     */
    public static final String CANDIDATE_USERS = "candidateUsers";


    /**
     * 审批组标识
     * <p>
     * 用于标识流程任务可以由指定组中的任意成员处理
     * </p>
     */
    public static final String CANDIDATE_GROUPS = "candidateGroups";

    /**
     * 流程审批标识
     * <p>
     * 用于标识流程变量中存储审批人信息的键名
     * </p>
     */
    public static final String PROCESS_APPROVAL = "approval";

    /**
     * 会签人员列表标识
     * <p>
     * 用于标识流程变量中存储会签人员列表信息的键名
     * </p>
     */
    public static final String PROCESS_MULTI_INSTANCE_USER = "userList";

    /**
     * Flowable BPMN命名空间
     * <p>
     * Flowable流程定义文件中使用的标准命名空间URI
     * </p>
     */
    public static final String NAMASPASE = "http://flowable.org/bpmn";

    /**
     * 会签节点标识
     * <p>
     * 用于标识流程节点为会签节点（多实例节点）
     * </p>
     */
    public static final String PROCESS_MULTI_INSTANCE = "multiInstance";

    /**
     * 自定义属性数据类型标识
     * <p>
     * 用于标识流程节点自定义属性中的数据类型配置
     * </p>
     */
    public static final String PROCESS_CUSTOM_DATA_TYPE = "dataType";

    /**
     * 自定义属性用户类型标识
     * <p>
     * 用于标识流程节点自定义属性中的用户类型配置
     * </p>
     */
    public static final String PROCESS_CUSTOM_USER_TYPE = "userType";

    /**
     * 流程发起人标识
     * <p>
     * 用于标识流程变量中存储流程发起人信息的键名
     * </p>
     */
    public static final String PROCESS_INITIATOR = "INITIATOR";


    /**
     * 流程跳过表达式启用标识
     * <p>
     * 用于标识是否启用流程节点跳过表达式功能的流程变量键名
     * </p>
     */
    public static final String FLOWABLE_SKIP_EXPRESSION_ENABLED = "_FLOWABLE_SKIP_EXPRESSION_ENABLED";


}