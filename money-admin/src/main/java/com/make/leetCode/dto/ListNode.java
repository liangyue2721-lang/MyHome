package com.make.leetCode.dto;

/**
 * 单链表节点定义
 * 用于表示单向链表中的节点结构
 * <p>
 * 链表特点：
 * 1. 每个节点包含数据域（val）和指针域（next）
 * 2. 通过 next 指针将多个节点串联形成链式结构
 * 3. 最后一个节点的 next 指向 null，表示链表结束
 */
public class ListNode {
    /**
     * 数据域：存储节点的值
     * 对于整数链表，存储整数值
     * 可根据需要修改为其他数据类型
     */
    public int val;

    /**
     * 指针域：指向下一个节点的引用
     * 如果是尾节点，该值为 null
     * 通过该引用可以遍历整个链表
     */
    public ListNode next;

    /**
     * 无参构造函数
     * 创建一个空节点，值为默认值 0，next 为 null
     * 适用于需要先创建节点再赋值的场景
     */
    public ListNode() {
    }

    /**
     * 单参数构造函数
     * 创建带有指定值的节点，next 默认为 null
     * 适用于创建单个节点或尾节点的场景
     *
     * @param val 节点要存储的值
     */
    public ListNode(int val) {
        this.val = val;
    }

    /**
     * 双参数构造函数
     * 创建带有指定值和下一个节点引用的节点
     * 适用于在已知后继节点的情况下创建节点
     *
     * @param val  节点要存储的值
     * @param next 指向下一个节点的引用
     */
    public ListNode(int val, ListNode next) {
        this.val = val;
        this.next = next;
    }
}
