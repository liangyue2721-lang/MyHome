package com.make.leetCode;

/**
 * 9. 回文数
 * 简单
 * 提示
 * 给你一个整数 x ，如果 x 是一个回文整数，返回 true ；否则，返回 false 。
 * 回文数是指正序（从左向右）和倒序（从右向左）读都是一样的整数。
 * 例如，121 是回文，而 123 不是。
 *
 * 示例 1：
 * 输入：x = 121
 * 输出：true
 * 示例 2：
 * 输入：x = -121
 * 输出：false
 * 解释：从左向右读, 为 -121 。 从右向左读, 为 121- 。因此它不是一个回文数。
 * 示例 3：
 * 输入：x = 10
 * 输出：false
 * 解释：从右向左读, 为 01 。因此它不是一个回文数。
 * 提示：
 * -231 <= x <= 231 - 1
 *
 * 进阶：你能不将整数转为字符串来解决这个问题吗？
 */
public class isPalindromeSolutionLeetCode {

    public static void main(String[] args) {
        int x = 121;
        System.out.println(new isPalindromeSolutionLeetCode().isPalindrome(x));
    }
    /**
     * 判断一个整数是否是回文数
     *
     * 实现思路：反转一半数字进行比较
     * 1. 负数一定不是回文数（负号位置固定）
     * 2. 0-9 的个位数都是回文数
     * 3. 末尾是 0 的数（除了 0 本身）都不是回文数（首位不能为 0）
     * 4. 通过数学运算反转数字的后半部分，与前半部分比较
     *    - 避免完全反转可能导致的整数溢出问题
     *    - 提高效率，只处理一半的数字
     *
     * @param x 待判断的整数
     * @return 如果是回文数返回 true，否则返回 false
     */
    public boolean isPalindrome(int x) {
        // 负数不是回文数（负号只在开头，不在结尾）
        if (x < 0) {
            return false;
        }

        // 个位数（0-9）都是回文数
        if (x >= 0 && x < 10) {
            return true;
        }

        // 末尾是 0 的数不是回文数（除了 0 本身）
        // 原因：回文数的首位和末位相同，如果末位是 0，首位也必须是 0，但整数的首位不能为 0
        if (x % 10 == 0) {
            return false;
        }

        // 反转后半部分数字
        int reversedHalf = 0;

        // 循环直到原始数字小于或等于反转后的数字
        // 这意味着已经处理了一半的数字
        while (x > reversedHalf) {
            // 取当前位的个位数，添加到反转数字的末尾
            reversedHalf = reversedHalf * 10 + x % 10;
            // 去掉原始数字的个位
            x /= 10;
        }

        // 比较前半部分和后半部分是否相等
        // 情况 1（偶数位）：x == reversedHalf，例如 1221 -> x=12, reversedHalf=12
        // 情况 2（奇数位）：x == reversedHalf / 10，例如 12321 -> x=12, reversedHalf=123
        // 奇数位时，中间的数字在 reversedHalf 的个位上，需要去除
        return x == reversedHalf || x == reversedHalf / 10;
    }


}
