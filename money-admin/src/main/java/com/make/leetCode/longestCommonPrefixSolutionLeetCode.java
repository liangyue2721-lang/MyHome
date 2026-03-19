package com.make.leetCode;

/**
 * 14. 最长公共前缀
 * 简单
 * 编写一个函数来查找字符串数组中的最长公共前缀。
 * 如果不存在公共前缀，返回空字符串 ""。
 *
 * 示例 1：
 * 输入：strs = ["flower","flow","flight"]
 * 输出："fl"
 * 示例 2：
 * 输入：strs = ["dog","racecar","car"]
 * 输出：""
 * 解释：输入不存在公共前缀。
 *
 *
 * 提示：
 * 1 <= strs.length <= 200
 * 0 <= strs[i].length <= 200
 * strs[i] 如果非空，则仅由小写英文字母组成
 */
public class longestCommonPrefixSolutionLeetCode {

    public static void main(String[] args) {
        String[] strs = {"flower","flow","flight"};
        System.out.println(new longestCommonPrefixSolutionLeetCode().longestCommonPrefix(strs));
    }

    /**
     * 查找字符串数组中的最长公共前缀
     *
     * 实现逻辑：
     * 1. 边界检查：如果数组为空或长度为 0，直接返回空字符串
     * 2. 以第一个字符串为基准，获取其长度作为外层循环的上限
     * 3. 遍历第一个字符串的每个字符位置（纵向扫描）
     * 4. 对于每个位置 i，检查其他所有字符串在位置 i 的字符
     * 5. 如果发现以下情况之一，立即返回已匹配的前缀：
     *    - 某个字符串的长度不足以到达位置 i
     *    - 某个字符串在位置 i 的字符与基准字符不同
     * 6. 如果所有字符串在位置 i 的字符都相同，将该字符加入结果
     * 7. 当第一个字符串遍历完成或提前发现不匹配时，返回累积的结果
     *
     * @param strs 字符串数组
     * @return 最长公共前缀字符串
     */
    public String longestCommonPrefix(String[] strs) {
        // 边界检查：数组为空或长度为 0 时返回空字符串
        if (strs == null || strs.length == 0) {
            return "";
        }

        // 使用 StringBuilder 高效构建结果字符串
        StringBuilder result = new StringBuilder();

        // 获取第一个字符串作为基准，其长度决定最大可能的公共前缀长度
        String firstStr = strs[0];
        int firstLen = firstStr.length();

        // 纵向扫描：遍历第一个字符串的每个字符位置
        for (int i = 0; i < firstLen; i++) {
            // 获取基准字符（第一个字符串在位置 i 的字符）
            char benchmark = firstStr.charAt(i);

            // 横向比较：检查其他所有字符串在位置 i 的字符
            for (int j = 1; j < strs.length; j++) {
                // 获取当前字符串
                String currentStr = strs[j];

                // 条件判断 1：当前字符串长度不足，无法提供位置 i 的字符
                if (i >= currentStr.length()) {
                    return result.toString();
                }

                // 条件判断 2：当前字符与基准字符不匹配
                if (currentStr.charAt(i) != benchmark) {
                    return result.toString();
                }
            }

            // 所有字符串在位置 i 的字符都匹配，将基准字符加入结果
            result.append(benchmark);
        }

        // 返回完整的最长公共前缀
        return result.toString();
    }
}
