package com.make.flowable.flow;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
//import com.greenpineyu.fel.FelEngine;
//import com.greenpineyu.fel.FelEngineImpl;
//import com.greenpineyu.fel.context.FelContext;
//import org.apache.commons.jexl2.JexlContext;
//import org.apache.commons.jexl2.JexlEngine;
//import org.apache.commons.jexl2.MapContext;
//import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.*;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;

import java.util.*;

/**
 * Flowable流程节点查找工具类
 * <p>
 * 该工具类提供了在Flowable流程定义中查找下一节点的相关方法，
 * 主要用于在流程运行时根据当前节点和流程变量确定后续可能执行的用户任务节点。
 * </p>
 *
 * @author 27
 * @date 2021/4/19 20:51
 */
public class FindNextNodeUtil {

    /**
     * 根据当前任务获取下一步骤的用户任务列表
     * <p>
     * 该方法通过分析流程定义中的节点连接关系和条件表达式，
     * 计算出从当前任务节点可能到达的所有用户任务节点。
     * </p>
     *
     * @param repositoryService Flowable仓库服务，用于查询流程定义和BPMN模型
     * @param task              当前任务对象，包含流程定义ID和任务定义Key等信息
     * @param map               流程变量Map，用于条件表达式的计算
     * @return 下一步可能执行的用户任务节点列表
     */
    public static List<UserTask> getNextUserTasks(RepositoryService repositoryService, org.flowable.task.api.Task task, Map<String, Object> map) {
        // 创建用于存储结果的用户任务列表
        List<UserTask> data = new ArrayList<>();
        // 根据流程定义ID查询流程定义对象
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(task.getProcessDefinitionId()).singleResult();
        // 根据流程定义ID获取BPMN模型
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        // 获取主流程定义
        Process mainProcess = bpmnModel.getMainProcess();
        // 获取流程中的所有元素集合
        Collection<FlowElement> flowElements = mainProcess.getFlowElements();
        // 获取当前任务的定义Key
        String key = task.getTaskDefinitionKey();
        // 根据Key获取当前流程元素
        FlowElement flowElement = bpmnModel.getFlowElement(key);
        // 递归查找下一用户任务节点
        next(flowElements, flowElement, map, data);
        // 返回结果
        return data;
    }

    /**
     * 启动流程时获取下一步骤的用户任务列表
     * <p>
     * 该方法用于在流程启动时，根据流程定义和初始变量计算出可能执行的第一个用户任务节点。
     * </p>
     *
     * @param repositoryService  Flowable仓库服务，用于查询流程定义和BPMN模型
     * @param processDefinition  流程定义对象
     * @param map                流程变量Map，用于条件表达式的计算
     * @return 下一步可能执行的用户任务节点列表
     */
    public static List<UserTask> getNextUserTasksByStart(RepositoryService repositoryService, ProcessDefinition processDefinition, Map<String, Object> map) {
        // 创建用于存储结果的用户任务列表
        List<UserTask> data = new ArrayList<>();
        // 根据流程定义ID获取BPMN模型
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        // 获取主流程定义
        Process mainProcess = bpmnModel.getMainProcess();
        // 获取流程中的所有元素集合
        Collection<FlowElement> flowElements = mainProcess.getFlowElements();
        // 初始化节点Key为空
        String key = null;
        // 查找开始节点并获取其唯一Key
        for (FlowElement flowElement : flowElements) {
            // 判断是否为开始事件节点
            if (flowElement instanceof StartEvent) {
                // 获取开始节点的ID作为Key
                key = flowElement.getId();
                // 找到开始节点后跳出循环
                break;
            }
        }
        // 根据Key获取开始节点元素
        FlowElement flowElement = bpmnModel.getFlowElement(key);
        // 获取开始节点的出口顺序流列表
        List<SequenceFlow> sequenceFlows = ((StartEvent)flowElement).getOutgoingFlows();
        // 获取出口连线，此时从开始节点往后，只能是一个出口
        if (!sequenceFlows.isEmpty()) {
            // 获取第一个也是唯一的出口顺序流
            SequenceFlow sequenceFlow = sequenceFlows.get(0);
            // 获取目标流程元素
            FlowElement targetFlowElement = sequenceFlow.getTargetFlowElement();
            // 递归查找下一用户任务节点
            next(flowElements, targetFlowElement, map, data);
        }
        // 返回结果
        return data;
    }


    /**
     * 递归查找下一节点
     * <p>
     * 该方法通过递归方式遍历流程定义中的节点连接关系，
     * 根据条件表达式的计算结果确定可能执行的路径，并收集所有可能到达的用户任务节点。
     * </p>
     *
     * @param flowElements 流程元素集合
     * @param flowElement  当前流程元素
     * @param map          流程变量Map，用于条件表达式的计算
     * @param nextUser     用于存储查找到的用户任务节点的列表
     */
    public static void next(Collection<FlowElement> flowElements, FlowElement flowElement, Map<String, Object> map, List<UserTask> nextUser) {
        // 如果是结束节点
        if (flowElement instanceof EndEvent) {
            // 如果是子任务的结束节点
            if (getSubProcess(flowElements, flowElement) != null) {
                // 将当前元素更新为子流程元素
                flowElement = getSubProcess(flowElements, flowElement);
            }
        }
        // 获取Task的出线信息--可以拥有多个
        List<SequenceFlow> outGoingFlows = null;
        // 根据不同类型的节点获取其出口顺序流
        if (flowElement instanceof Task) {
            // 如果是任务节点，获取其出口顺序流
            outGoingFlows = ((Task) flowElement).getOutgoingFlows();
        } else if (flowElement instanceof Gateway) {
            // 如果是网关节点，获取其出口顺序流
            outGoingFlows = ((Gateway) flowElement).getOutgoingFlows();
        } else if (flowElement instanceof StartEvent) {
            // 如果是开始事件节点，获取其出口顺序流
            outGoingFlows = ((StartEvent) flowElement).getOutgoingFlows();
        } else if (flowElement instanceof SubProcess) {
            // 如果是子流程节点，获取其出口顺序流
            outGoingFlows = ((SubProcess) flowElement).getOutgoingFlows();
        } else if (flowElement instanceof CallActivity) {
            // 如果是调用活动节点，获取其出口顺序流
            outGoingFlows = ((CallActivity) flowElement).getOutgoingFlows();
        }
        // 如果存在出口顺序流
        if (outGoingFlows != null && outGoingFlows.size() > 0) {
            // 遍历所有的出线--找到可以正确执行的那一条
            for (SequenceFlow sequenceFlow : outGoingFlows) {
                // 获取顺序流上的条件表达式
                String expression = sequenceFlow.getConditionExpression();
                // 判断条件：
                // 1. 没有条件表达式
                // 2. 有条件表达式且计算结果为true
                if (expression == null ||
                        expressionResult(map, expression.substring(expression.lastIndexOf("{") + 1, expression.lastIndexOf("}")))) {
                    // 获取出线的目标节点ID
                    String nextFlowElementID = sequenceFlow.getTargetRef();
                    // 检查是否为多实例子流程
                    if (checkSubProcess(nextFlowElementID, flowElements, nextUser)) {
                        // 如果是多实例子流程，跳过后续处理
                        continue;
                    }

                    // 根据ID查询下一节点的信息
                    FlowElement nextFlowElement = getFlowElementById(nextFlowElementID, flowElements);
                    // 如果是调用流程
                    if (nextFlowElement instanceof CallActivity) {
                        // 强制转换为调用活动节点
                        CallActivity ca = (CallActivity) nextFlowElement;
                        // 检查是否具有循环特性
                        if (ca.getLoopCharacteristics() != null) {
                            // 创建用户任务对象
                            UserTask userTask = new UserTask();
                            // 设置用户任务ID
                            userTask.setId(ca.getId());

                            // 重复设置用户任务ID（可能是代码错误）
                            userTask.setId(ca.getId());
                            // 设置循环特性
                            userTask.setLoopCharacteristics(ca.getLoopCharacteristics());
                            // 设置用户任务名称
                            userTask.setName(ca.getName());
                            // 将用户任务添加到结果列表中
                            nextUser.add(userTask);
                        }
                        // 递归查找下一节点
                        next(flowElements, nextFlowElement, map, nextUser);
                    }
                    // 如果是用户任务节点
                    if (nextFlowElement instanceof UserTask) {
                        // 将用户任务添加到结果列表中
                        nextUser.add((UserTask) nextFlowElement);
                    }
                    // 如果是排他网关节点
                    else if (nextFlowElement instanceof ExclusiveGateway) {
                        // 递归查找下一节点
                        next(flowElements, nextFlowElement, map, nextUser);
                    }
                    // 如果是并行网关节点
                    else if (nextFlowElement instanceof ParallelGateway) {
                        // 递归查找下一节点
                        next(flowElements, nextFlowElement, map, nextUser);
                    }
                    // 如果是接收任务节点
                    else if (nextFlowElement instanceof ReceiveTask) {
                        // 递归查找下一节点
                        next(flowElements, nextFlowElement, map, nextUser);
                    }
                    // 如果是服务任务节点
                    else if (nextFlowElement instanceof ServiceTask) {
                        // 递归查找下一节点
                        next(flowElements, nextFlowElement, map, nextUser);
                    }
                    // 如果是子任务的起点（开始事件）
                    else if (nextFlowElement instanceof StartEvent) {
                        // 递归查找下一节点
                        next(flowElements, nextFlowElement, map, nextUser);
                    }
                    // 如果是结束节点
                    else if (nextFlowElement instanceof EndEvent) {
                        // 递归查找下一节点
                        next(flowElements, nextFlowElement, map, nextUser);
                    }
                }
            }
        }
    }

    /**
     * 判断是否是多实例子流程并且需要设置集合类型变量
     * <p>
     * 该方法用于检查指定ID的节点是否为具有多实例特性的子流程节点，
     * 如果是，则创建相应的用户任务对象并添加到结果列表中。
     * </p>
     *
     * @param id           节点ID
     * @param flowElements 流程元素集合
     * @param nextUser     用于存储查找到的用户任务节点的列表
     * @return 如果是指定的多实例子流程节点则返回true，否则返回false
     */
    public static boolean checkSubProcess(String id, Collection<FlowElement> flowElements, List<UserTask> nextUser) {
        // 遍历所有流程元素
        for (FlowElement flowElement1 : flowElements) {
            // 判断是否为子流程节点且ID匹配
            if (flowElement1 instanceof SubProcess && flowElement1.getId().equals(id)) {
                // 强制转换为子流程节点
                SubProcess sp = (SubProcess) flowElement1;
                // 检查是否具有循环特性
                if (sp.getLoopCharacteristics() != null) {
                    // 创建用户任务对象
                    UserTask userTask = new UserTask();
                    // 设置用户任务ID
                    userTask.setId(sp.getId());
                    // 设置循环特性
                    userTask.setLoopCharacteristics(sp.getLoopCharacteristics());
                    // 设置用户任务名称
                    userTask.setName(sp.getName());
                    // 将用户任务添加到结果列表中
                    nextUser.add(userTask);
                    // 返回true表示找到了匹配的多实例子流程
                    return true;
                }
            }
        }
        // 返回false表示未找到匹配的多实例子流程
        return false;
    }

    /**
     * 查询一个节点是否为子任务中的节点，如果是，则返回子任务
     * <p>
     * 该方法用于判断指定的流程结束节点是否属于某个子流程，
     * 如果是，则返回该子流程节点。
     * </p>
     *
     * @param flowElements 全流程的节点集合
     * @param flowElement  当前节点（通常为结束节点）
     * @return 如果当前节点属于某个子流程则返回该子流程节点，否则返回null
     */
    public static FlowElement getSubProcess(Collection<FlowElement> flowElements, FlowElement flowElement) {
        // 遍历所有流程元素
        for (FlowElement flowElement1 : flowElements) {
            // 判断是否为子流程节点
            if (flowElement1 instanceof SubProcess) {
                // 遍历子流程中的所有元素
                for (FlowElement flowElement2 : ((SubProcess) flowElement1).getFlowElements()) {
                    // 判断是否与指定节点相等
                    if (flowElement.equals(flowElement2)) {
                        // 返回父级子流程节点
                        return flowElement1;
                    }
                }
            }
        }
        // 未找到对应的子流程节点，返回null
        return null;
    }


    /**
     * 根据ID查询流程节点对象，如果是子任务，则返回子任务的开始节点
     * <p>
     * 该方法根据指定的节点ID在流程元素集合中查找对应的流程节点，
     * 如果找到的节点是子流程，则返回子流程中的开始节点。
     * </p>
     *
     * @param Id           节点ID
     * @param flowElements 流程节点集合
     * @return 找到的流程节点对象，如果未找到则返回null
     */
    public static FlowElement getFlowElementById(String Id, Collection<FlowElement> flowElements) {
        // 遍历所有流程元素
        for (FlowElement flowElement : flowElements) {
            // 判断节点ID是否匹配
            if (flowElement.getId().equals(Id)) {
                // 如果是子任务，则查询出子任务的开始节点
                if (flowElement instanceof SubProcess) {
                    // 返回子流程中的开始节点
                    return getStartFlowElement(((SubProcess) flowElement).getFlowElements());
                }
                // 返回找到的节点
                return flowElement;
            }
            // 如果是子流程节点
            if (flowElement instanceof SubProcess) {
                // 递归在子流程元素中查找指定ID的节点
                FlowElement flowElement1 = getFlowElementById(Id, ((SubProcess) flowElement).getFlowElements());
                // 如果找到了对应的节点
                if (flowElement1 != null) {
                    // 返回找到的节点
                    return flowElement1;
                }
            }
        }
        // 未找到对应的节点，返回null
        return null;
    }

    /**
     * 返回流程的开始节点
     * <p>
     * 该方法在指定的流程元素集合中查找开始事件节点。
     * </p>
     *
     * @param flowElements 节点集合
     * @return 找到的开始事件节点，如果未找到则返回null
     */
    public static FlowElement getStartFlowElement(Collection<FlowElement> flowElements) {
        // 遍历所有流程元素
        for (FlowElement flowElement : flowElements) {
            // 判断是否为开始事件节点
            if (flowElement instanceof StartEvent) {
                // 返回找到的开始节点
                return flowElement;
            }
        }
        // 未找到开始节点，返回null
        return null;
    }

    /**
     * 校验EL表达式
     * <p>
     * 该方法使用Aviator表达式引擎计算指定表达式的值，
     * 通常用于计算流程顺序流上的条件表达式。
     * </p>
     *
     * @param map        流程变量Map，作为表达式计算的上下文
     * @param expression 需要计算的表达式字符串
     * @return 表达式计算结果，通常为布尔值
     */
    public static boolean expressionResult(Map<String, Object> map, String expression) {
        // 编译表达式
        Expression exp = AviatorEvaluator.compile(expression);
        // 执行表达式并返回布尔结果
        return (Boolean) exp.execute(map);
//        return true;
    }
}