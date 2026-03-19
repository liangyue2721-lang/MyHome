package com.make.leetCode;

/**
 * 28. 找出字符串中第一个匹配项的下标
 * 简单
 * 给你两个字符串 haystack 和 needle ，请你在 haystack 字符串中找出 needle 字符串的第一个匹配项的下标（下标从 0 开始）。如果 needle 不是 haystack 的一部分，则返回  -1 。
 *
 * 示例 1：
 * 输入：haystack = "sadbutsad", needle = "sad"
 * 输出：0
 * 解释："sad" 在下标 0 和 6 处匹配。
 * 第一个匹配项的下标是 0 ，所以返回 0 。
 * 示例 2：
 * 输入：haystack = "leetcode", needle = "leeto"
 * 输出：-1
 * 解释："leeto" 没有在 "leetcode" 中出现，所以返回 -1 。
 *
 *
 * 提示：
 * 1 <= haystack.length, needle.length <= 104
 * haystack 和 needle 仅由小写英文字符组成
 */
public class strStrSolutionLeetCode {

    public static void main(String[] args) {
        System.out.println(new strStrSolutionLeetCode().strStr("sadbutsad", "sad"));
        System.out.println(new strStrSolutionLeetCode().strStr("leetcode", "leeto"));
    }

    /**
     * 在 haystack 字符串中查找 needle 字符串第一次出现的位置
     * 这道题要求在 haystack 字符串中找出 needle 字符串第一次出现的位置。我使用暴力匹配法（Brute Force）：
     * 边界检查：如果 needle 为空或长度大于 haystack，直接返回 -1
     * 滑动窗口：遍历 haystack，以每个字符作为起点，尝试匹配 needle
     * 逐字符比较：对于每个起点，逐个字符比较 needle 和 haystack 对应位置的字符
     * 完全匹配：如果所有字符都匹配，返回当前起点索引
     * 无匹配：遍历完所有位置仍未找到，返回 -1
     * 时间复杂度：O((m-n) × n)，其中 m 是 haystack 长度，n 是 needle 长度 空间复杂度：O(1)
     * @param haystack 主字符串
     * @param needle 要查找的子字符串
     * @return 第一次匹配的下标，如果未找到则返回 -1
     */
    public int strStr(String haystack, String needle) {
        // 获取两个字符串的长度
        int m = haystack.length();
        int n = needle.length();

        // 边界情况：如果 needle 为空字符串，返回 0
        if (n == 0) {
            return 0;
        }

        // 边界情况：如果 needle 长度大于 haystack，肯定无法匹配
        if (n > m) {
            return -1;
        }

        // 遍历 haystack，只需要遍历到 m - n 的位置
        // 因为剩余长度不足以容纳 needle
        for (int i = 0; i <= m - n; i++) {
            // 标记是否匹配成功
            boolean match = true;

            // 从当前位置开始，逐个字符比较 needle
            for (int j = 0; j < n; j++) {
                // 如果字符不匹配
                if (haystack.charAt(i + j) != needle.charAt(j)) {
                    // 标记匹配失败
                    match = false;
                    // 跳出内层循环，尝试下一个起始位置
                    break;
                }
            }

            // 如果所有字符都匹配成功
            if (match) {
                // 返回当前起始位置
                return i;
            }
        }

        // 遍历完所有位置仍未找到匹配，返回 -1
        return -1;
    }
}
