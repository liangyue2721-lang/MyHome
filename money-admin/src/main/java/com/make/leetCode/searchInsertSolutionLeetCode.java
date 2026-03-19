package com.make.leetCode;

/**
 * 35. 搜索插入位置
 * 简单
 * 给定一个排序数组和一个目标值，在数组中找到目标值，并返回其索引。如果目标值不存在于数组中，返回它将会被按顺序插入的位置。
 * 请必须使用时间复杂度为 O(log n) 的算法。
 * <p>
 * 示例 1:
 * 输入: nums = [1,3,5,6], target = 5
 * 输出: 2
 * 示例 2:
 * 输入: nums = [1,3,5,6], target = 2
 * 输出: 1
 * 示例 3:
 * 输入: nums = [1,3,5,6], target = 7
 * 输出: 4
 * <p>
 * 提示:
 * 1 <= nums.length <= 104
 * -104 <= nums[i] <= 104
 * nums 为 无重复元素 的 升序 排列数组
 * -104 <= target <= 104
 */
public class searchInsertSolutionLeetCode {

    public static void main(String[] args) {
        int[] nums = new int[]{1, 3, 5, 6};
        int target = 5;
        System.out.println(searchInsert(nums, target));
    }

    /**
     * 在有序数组中搜索目标值，如果不存在则返回其应该插入的位置
     * 使用二分查找算法，时间复杂度 O(log n)
     * 实现逻辑说明
     * 题目要求在有序数组中查找目标值或其应该插入的位置，并要求时间复杂度为 O(log n)，因此必须使用二分查找：
     * 初始化指针：left 指向数组起始位置，right 指向数组末尾位置
     * 二分查找：
     * 计算中间位置 mid
     * 如果 nums[mid] == target，直接返回 mid
     * 如果 nums[mid] < target，目标在右半部分，移动 left = mid + 1
     * 如果 nums[mid] > target，目标在左半部分，移动 right = mid - 1
     * 返回插入位置：如果未找到目标值，left 指针的位置就是应该插入的位置
     *
     * @param nums   升序排列的无重复元素数组
     * @param target 要查找的目标值
     * @return 目标值的索引，或应该插入的位置
     */
    public static int searchInsert(int[] nums, int target) {
        // 初始化左指针，指向数组起始位置
        int left = 0;

        // 初始化右指针，指向数组末尾位置
        int right = nums.length - 1;

        // 当左指针不超过右指针时，继续查找
        while (left <= right) {
            // 计算中间位置，使用位运算避免整数溢出
            // 等价于 (left + right) / 2，但更安全
            int mid = left + ((right - left) >> 1);

            // 如果中间位置的元素等于目标值
            if (nums[mid] == target) {
                // 直接返回中间索引
                return mid;
            }
            // 如果中间位置的元素小于目标值
            else if (nums[mid] < target) {
                // 目标值应该在右半部分，移动左指针到 mid + 1
                left = mid + 1;
            }
            // 如果中间位置的元素大于目标值
            else {
                // 目标值应该在左半部分，移动右指针到 mid - 1
                right = mid - 1;
            }
        }

        // 循环结束时，left 就是目标值应该插入的位置
        // 因为 left 左边的元素都小于 target，right 右边的元素都大于 target
        return left;
    }
}
