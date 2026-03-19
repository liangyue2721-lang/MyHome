package com.make.leetCode;

/**
 * 26. 删除有序数组中的重复项
 * 简单
 * 提示
 * 给你一个 非严格递增排列 的数组 nums ，请你 原地 删除重复出现的元素，使每个元素 只出现一次 ，返回删除后数组的新长度。元素的 相对顺序 应该保持 一致 。然后返回 nums 中唯一元素的个数。
 * 考虑 nums 的唯一元素的数量为 k。去重后，返回唯一元素的数量 k。
 * nums 的前 k 个元素应包含 排序后 的唯一数字。下标 k - 1 之后的剩余元素可以忽略。
 * <p>
 * 判题标准:
 * 系统会用下面的代码来测试你的题解:
 * int[] nums = [...]; // 输入数组
 * int[] expectedNums = [...]; // 长度正确的期望答案
 * int k = removeDuplicates(nums); // 调用
 * assert k == expectedNums.length;
 * for (int i = 0; i < k; i++) {
 * assert nums[i] == expectedNums[i];
 * }
 * 如果所有断言都通过，那么您的题解将被 通过。
 * <p>
 * 示例 1：
 * 输入：nums = [1,1,2]
 * 输出：2, nums = [1,2,_]
 * 解释：函数应该返回新的长度 2 ，并且原数组 nums 的前两个元素被修改为 1, 2 。不需要考虑数组中超出新长度后面的元素。
 * 示例 2：
 * 输入：nums = [0,0,1,1,1,2,2,3,3,4]
 * 输出：5, nums = [0,1,2,3,4,_,_,_,_,_]
 * 解释：函数应该返回新的长度 5 ， 并且原数组 nums 的前五个元素被修改为 0, 1, 2, 3, 4 。不需要考虑数组中超出新长度后面的元素。
 * <p>
 * <p>
 * 提示：
 * 1 <= nums.length <= 3 * 104
 * -100 <= nums[i] <= 100
 * nums 已按 非递减 顺序排列。
 */
public class removeDuplicatesSolutionLeetCode {

    public static void main(String[] args) {
        int[] nums = {1, 1, 2};
        System.out.println(removeDuplicates(nums));
    }

    /**
     * 删除有序数组中的重复项
     * 使用双指针法：指针 i 指向不重复序列的最后一个位置，指针 j 用于遍历数组
     * 由于数组已排序，相同元素必定相邻，只需比较相邻元素即可
     *
     * @param nums 非严格递增排列的输入数组
     * @return 删除重复元素后数组的新长度（唯一元素的个数）
     */
    public static int removeDuplicates(int[] nums) {
        // 边界条件处理：空数组或长度为 0 的数组直接返回 0
        if (nums == null || nums.length == 0) {
            return 0;
        }

        // 初始化慢指针 i，指向第一个元素（下标 0）
        // i 表示不重复序列的最后一个位置的下标
        int i = 0;

        // 快指针 j 从第二个元素开始遍历数组（下标 1）
        for (int j = 1; j < nums.length; j++) {
            // 当前元素 nums[j] 与不重复序列的最后一个元素 nums[i] 进行比较
            // 如果不相等，说明找到了一个新的不重复元素
            if (nums[j] != nums[i]) {
                // 慢指针 i 向前移动一位
                i++;
                // 将新发现的不重复元素 nums[j] 赋值到 nums[i] 位置
                nums[i] = nums[j];
            }
            // 如果相等，则 j 继续向前移动，跳过这个重复元素
        }

        // 返回不重复元素的个数
        // 由于下标从 0 开始，所以个数为 i + 1
        return i + 1;
    }
}
