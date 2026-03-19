package com.make.leetCode;

import java.util.Deque;
import java.util.LinkedList;

/**
 * 20. 有效的括号
 * 简单
 * 提示
 * 给定一个只包括 '('，')'，'{'，'}'，'['，']' 的字符串 s ，判断字符串是否有效。
 *
 * 有效字符串需满足：
 * 左括号必须用相同类型的右括号闭合。
 * 左括号必须以正确的顺序闭合。
 * 每个右括号都有一个对应的相同类型的左括号。
 *
 * 示例 1：
 * 输入：s = "()"
 * 输出：true
 *
 * 示例 2：
 * 输入：s = "()[]{}"
 * 输出：true
 *
 * 示例 3：
 * 输入：s = "(]"
 * 输出：false
 *
 * 示例 4：
 * 输入：s = "([])"
 * 输出：true
 *
 * 示例 5：
 * 输入：s = "([)]"
 * 输出：false
 *
 * 提示：
 * 1 <= s.length <= 104
 * s 仅由括号 '()[]{}' 组成
 */
public class isValidSolutionLeetCode {

    public static void main(String[] args) {
        String s = "()[]{}";
        System.out.println(new isValidSolutionLeetCode().isValid(s));
    }

    /**
     * 判断字符串中的括号是否有效匹配
     *
     * 实现逻辑：
     * 1. 边界检查：如果字符串为空或长度为奇数，直接返回 false（偶数个字符才可能完全匹配）
     * 2. 创建栈用于存储期望的右括号
     * 3. 遍历字符串中的每个字符：
     *    - 遇到左括号 '('、'{'、'[' 时，将对应的右括号压入栈中
     *    - 遇到右括号时，进行以下检查：
     *      a) 检查栈是否为空：如果为空说明没有对应的左括号，返回 false
     *      b) 弹出栈顶元素，检查是否与当前右括号匹配：不匹配则返回 false
     * 4. 遍历完成后，检查栈是否为空：
     *    - 空：说明所有括号都正确匹配
     *    - 非空：说明有未闭合的左括号
     *
     * @param s 只包含括号字符的字符串
     * @return 括号是否有效匹配
     */
    public boolean isValid(String s) {
        // 边界检查：字符串为空直接返回 false
        if (s == null || s.isEmpty()) {
            return false;
        }

        // 优化：奇数长度的字符串必然无法完全匹配，提前返回
        if (s.length() % 2 != 0) {
            return false;
        }

        // 使用双端队列作为栈（比 Stack 性能更好）
        Deque<Character> stack = new LinkedList<>();

        // 遍历字符串中的每个字符
        for (int i = 0; i < s.length(); i++) {
            // 获取当前位置的字符
            char currentChar = s.charAt(i);

            // 判断是否为左括号，如果是则将对应的右括号压入栈
            if (currentChar == '(') {
                // 遇到左圆括号，压入期望的右圆括号
                stack.push(')');
            } else if (currentChar == '{') {
                // 遇到左花括号，压入期望的右花括号
                stack.push('}');
            } else if (currentChar == '[') {
                // 遇到左方括号，压入期望的右方括号
                stack.push(']');
            } else {
                // 遇到右括号，需要检查是否与栈顶的期望括号匹配

                // 检查 1：栈为空说明没有对应的左括号，直接返回 false
                if (stack.isEmpty()) {
                    return false;
                }

                // 检查 2：弹出栈顶元素，检查是否与当前右括号类型一致
                Character expectedBracket = stack.pop();
                if (expectedBracket != currentChar) {
                    // 括号类型不匹配，返回 false
                    return false;
                }
            }
        }

        // 最终检查：栈为空说明所有括号都正确匹配，否则说明有未闭合的左括号
        return stack.isEmpty();
    }
}
