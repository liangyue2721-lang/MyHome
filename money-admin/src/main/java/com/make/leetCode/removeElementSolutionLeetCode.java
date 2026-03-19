package com.make.leetCode;

/**
 * 27. 移除元素
 * 简单
 * 提示
 * 给你一个数组 nums 和一个值 val，你需要 原地 移除所有数值等于 val 的元素。元素的顺序可能发生改变。然后返回 nums 中与 val 不同的元素的数量。
 * 假设 nums 中不等于 val 的元素数量为 k，要通过此题，您需要执行以下操作：
 * 更改 nums 数组，使 nums 的前 k 个元素包含不等于 val 的元素。nums 的其余元素和 nums 的大小并不重要。
 * 返回 k。
 * 用户评测：
 * 评测机将使用以下代码测试您的解决方案：
 * int[] nums = [...]; // 输入数组
 * int val = ...; // 要移除的值
 * int[] expectedNums = [...]; // 长度正确的预期答案。
 * // 它以不等于 val 的值排序。
 * int k = removeElement(nums, val); // 调用你的实现
 * assert k == expectedNums.length;
 * sort(nums, 0, k); // 排序 nums 的前 k 个元素
 * for (int i = 0; i < actualLength; i++) {
 * assert nums[i] == expectedNums[i];
 * }
 * 如果所有的断言都通过，你的解决方案将会 通过。
 * <p>
 * 示例 1：
 * 输入：nums = [3,2,2,3], val = 3
 * 输出：2, nums = [2,2,_,_]
 * 解释：你的函数应该返回 k = 2, 并且 nums 中的前两个元素均为 2。
 * 你在返回的 k 个元素之外留下了什么并不重要（因此它们并不计入评测）。
 * 示例 2：
 * 输入：nums = [0,1,2,2,3,0,4,2], val = 2
 * 输出：5, nums = [0,1,4,0,3,_,_,_]
 * 解释：你的函数应该返回 k = 5，并且 nums 中的前五个元素为 0,0,1,3,4。
 * 注意这五个元素可以任意顺序返回。
 * 你在返回的 k 个元素之外留下了什么并不重要（因此它们并不计入评测）。
 * <p>
 * 提示：
 * 0 <= nums.length <= 100
 * 0 <= nums[i] <= 50
 * 0 <= val <= 100
 */
public class removeElementSolutionLeetCode {

    public static void main(String[] args) {
        int[] nums = {3, 2, 2, 3};
        System.out.println(removeElement(nums, 3));
    }

    /**
     * 原地移除数组中所有等于 val 的元素，返回剩余元素数量
     *
     * 这道题要求原地移除数组中等于特定值的元素，并返回新数组的长度。我使用双指针法来解决：
     * 慢指针 left：指向下一个要放置非目标值元素的位置
     * 快指针 right：遍历整个数组
     * 当快指针遇到不等于 val 的元素时，将其复制到慢指针位置，然后慢指针前移
     * 最终慢指针的位置就是新数组的长度
     * 这种方法的时间复杂度是 O(n)，空间复杂度是 O(1)。
     *
     * @param nums 输入数组
     * @param val  要移除的值
     * @return 移除指定值后数组的新长度
     */
    public static int removeElement(int[] nums, int val) {
        // 处理边界情况：空数组直接返回 0
        if (nums == null || nums.length == 0) {
            return 0;
        }

        // 慢指针：指向下一个要放置非目标值元素的位置
        int left = 0;

        // 快指针：遍历整个数组
        for (int right = 0; right < nums.length; right++) {
            // 如果当前元素不等于要移除的值
            if (nums[right] != val) {
                // 将该元素复制到慢指针位置
                nums[left] = nums[right];
                // 慢指针前移，准备放置下一个元素
                left++;
            }
        }

        // 返回新数组的长度（慢指针的位置即为新长度）
        return left;
    }
}
