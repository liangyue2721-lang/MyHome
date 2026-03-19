package com.make.leetCode;

/**
 * 1. 两数之和
 * 简单
 * 给定一个整数数组 nums 和一个整数目标值 target，请你在该数组中找出 和为目标值 target  的那 两个 整数，并返回它们的数组下标。
 * <p>
 * 你可以假设每种输入只会对应一个答案，并且你不能使用两次相同的元素。
 * <p>
 * 你可以按任意顺序返回答案。
 * <p>
 * <p>
 * <p>
 * 示例 1：
 * <p>
 * 输入：nums = [2,7,11,15], target = 9
 * 输出：[0,1]
 * 解释：因为 nums[0] + nums[1] == 9 ，返回 [0, 1] 。
 * 示例 2：
 * <p>
 * 输入：nums = [3,2,4], target = 6
 * 输出：[1,2]
 * 示例 3：
 * <p>
 * 输入：nums = [3,3], target = 6
 * 输出：[0,1]
 * <p>
 * <p>
 * 提示：
 * <p>
 * 2 <= nums.length <= 104
 * -109 <= nums[i] <= 109
 * -109 <= target <= 109
 * 只会存在一个有效答案
 * <p>
 * <p>
 * 进阶：你可以想出一个时间复杂度小于 O(n2) 的算法吗？
 */
public class twoSumSolutionLeetCode {
    /**
     * 程序入口方法
     * 测试两数之和算法，使用示例数据进行验证
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        // 初始化测试数组，包含四个整数元素
        int[] nums = {2, 7, 11, 15};

        // 设置目标和值为 9
        int target = 9;

        // 创建当前类的实例对象用于调用实例方法
        twoSumSolutionLeetCode solution = new twoSumSolutionLeetCode();

        // 调用 twoSum 方法查找数组中和为 target 的两个数的索引
        int[] result = solution.twoSum(nums, target);

        // 打印结果，输出两个索引值，用空格分隔
        System.out.println(result[0] + " " + result[1]);
    }

    /**
     * 两数之和核心方法
     * 使用暴力枚举法查找数组中两个数的索引，使得这两个数的和等于目标值
     * 时间复杂度：O(n²)，空间复杂度：O(1)
     *
     * @param nums   整数数组，包含待查找的元素
     * @param target 目标和值，需要找到的两个数的和
     * @return 包含两个索引的数组，表示满足条件的两个数在原数组中的位置；如果未找到则返回空数组
     */
    public int[] twoSum(int[] nums, int target) {
        // 外层循环：遍历数组中的每个元素，i 指向第一个数的索引
        for (int i = 0; i < nums.length; i++) {
            // 内层循环：从 i 的下一个位置开始遍历，j 指向第二个数的索引
            // 这样确保不会重复使用相同的元素，且避免重复组合
            for (int j = i + 1; j < nums.length; j++) {
                // 判断条件：检查当前位置的两个数之和是否等于目标值
                if (nums[i] + nums[j] == target) {
                    // 找到满足条件的两个数，返回它们的索引组成的数组
                    return new int[]{i, j};
                }
            }
        }

        // 如果遍历完整个数组都没有找到满足条件的两个数，返回空数组
        // 根据题目保证，正常情况下不会执行到这里
        return new int[]{};
    }
}
