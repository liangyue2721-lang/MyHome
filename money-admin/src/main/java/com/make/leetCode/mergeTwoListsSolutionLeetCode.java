package com.make.leetCode;

import com.make.leetCode.dto.ListNode;

/**
 * 21. 合并两个有序链表
 * 简单
 * 将两个升序链表合并为一个新的 升序 链表并返回。新链表是通过拼接给定的两个链表的所有节点组成的。
 * <p>
 * 示例 1：
 * 输入：l1 = [1,2,4], l2 = [1,3,4]
 * 输出：[1,1,2,3,4,4]
 * 示例 2：
 * 输入：l1 = [], l2 = []
 * 输出：[]
 * 示例 3：
 * 输入：l1 = [], l2 = [0]
 * 输出：[0]
 * <p>
 * 提示：
 * 两个链表的节点数目范围是 [0, 50]
 * -100 <= Node.val <= 100
 * l1 和 l2 均按 非递减顺序 排列
 */
public class mergeTwoListsSolutionLeetCode {
    /**
     * 合并两个升序链表为一个新的升序链表
     * <p>
     * 算法思路：
     * 1. 使用哑节点 (dummy node) 简化边界处理
     * 2. 使用尾指针 (tail) 追踪新链表的末尾
     * 3. 双指针遍历两个链表，每次选择较小的节点接入新链表
     * 4. 将剩余未处理的链表部分直接连接到新链表末尾
     * <p>
     * 时间复杂度：O(m + n)，其中 m 和 n 分别是两个链表的长度
     * 空间复杂度：O(1)，只使用了常数级别的额外空间
     *
     * @param list1 第一个升序链表
     * @param list2 第二个升序链表
     * @return 合并后的新升序链表
     */
    public static ListNode mergeTwoLists(ListNode list1, ListNode list2) {
        // 创建哑节点，值为 -1（任意值），用于简化头节点的处理
        // dummy.next 将指向合并后链表的头节点
        ListNode dummy = new ListNode(-1);

        // tail 指针始终指向新链表的最后一个节点
        // 初始时指向 dummy，后续节点将依次连接在 tail 之后
        ListNode tail = dummy;

        // 双指针遍历两个链表，当任一链表未遍历完时继续
        while (list1 != null && list2 != null) {
            // 比较两个链表当前节点的值
            if (list1.val <= list2.val) {
                // list1 的节点值较小或相等，将 list1 当前节点接入新链表
                tail.next = list1;
                // list1 指针后移，指向下一个待比较节点
                list1 = list1.next;
            } else {
                // list2 的节点值较小，将 list2 当前节点接入新链表
                tail.next = list2;
                // list2 指针后移，指向下一个待比较节点
                list2 = list2.next;
            }
            // tail 指针后移，始终指向新链表的末尾节点
            tail = tail.next;
        }

        // 处理剩余节点：如果 list1 还有剩余，直接接入新链表末尾
        // 因为链表本身是有序的，剩余部分必然都大于已合并的部分
        if (list1 != null) {
            tail.next = list1;
        }

        // 处理剩余节点：如果 list2 还有剩余，直接接入新链表末尾
        if (list2 != null) {
            tail.next = list2;
        }

        // 返回合并后的链表头节点
        // dummy.next 指向真正合并后链表的第一个节点（跳过了哑节点）
        return dummy.next;
    }

    /**
     * 打印链表内容，用于调试和验证结果
     * <p>
     * 格式：节点值之间用 " -> " 分隔，末尾添加 " -> null" 表示链表结束
     *
     * @param head 链表头节点
     */
    public static void printList(ListNode head) {
        // 定义当前遍历指针，从头节点开始
        ListNode current = head;

        // 遍历链表直到末尾
        while (current != null) {
            // 打印当前节点的值
            System.out.print(current.val);

            // 如果还有下一个节点，打印箭头符号作为分隔
            if (current.next != null) {
                System.out.print(" -> ");
            }

            // 指针后移，继续处理下一个节点
            current = current.next;
        }

        // 打印链表结束标记
        System.out.println(" -> null");
    }

    /**
     * 主方法：测试合并两个有序链表的功能
     * <p>
     * 测试用例：
     * - 输入：l1 = [1,2,4], l2 = [1,3,4]
     * - 预期输出：[1,1,2,3,4,4]
     */
    public static void main(String[] args) {
        // 创建第一个有序链表：1 -> 2 -> 4
        ListNode list1 = new ListNode(1, new ListNode(2, new ListNode(4)));

        // 创建第二个有序链表：1 -> 3 -> 4
        ListNode list2 = new ListNode(1, new ListNode(3, new ListNode(4)));

        // 调用合并方法，得到新的合并后链表
        ListNode mergedList = mergeTwoLists(list1, list2);

        // 打印合并后的链表结果
        printList(mergedList);
    }
}
