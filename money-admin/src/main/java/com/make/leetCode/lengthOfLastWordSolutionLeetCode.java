package com.make.leetCode;

/**
 * 58. 最后一个单词的长度
 * 简单
 * 给你一个字符串 s，由若干单词组成，单词前后用一些空格字符隔开。返回字符串中 最后一个 单词的长度。
 * 单词 是指仅由字母组成、不包含任何空格字符的最大子字符串。
 * <p>
 * 示例 1：
 * 输入：s = "Hello World"
 * 输出：5
 * 解释：最后一个单词是“World”，长度为 5。
 * 示例 2：
 * 输入：s = "   fly me   to   the moon  "
 * 输出：4
 * 解释：最后一个单词是“moon”，长度为 4。
 * 示例 3：
 * 输入：s = "luffy is still joyboy"
 * 输出：6
 * 解释：最后一个单词是长度为 6 的“joyboy”。
 * <p>
 * <p>
 * 提示：
 * 1 <= s.length <= 104
 * s 仅有英文字母和空格 ' ' 组成
 * s 中至少存在一个单词
 */
public class lengthOfLastWordSolutionLeetCode {

    public static void main(String[] args) {
        System.out.println(new lengthOfLastWordSolutionLeetCode().lengthOfLastWord("Hello World"));
    }

    /**
     * 计算字符串中最后一个单词的长度
     * 采用反向遍历的方式，从字符串末尾开始向前扫描
     * <p>
     * 这道题要求返回字符串中最后一个单词的长度。最优解法是从后向前遍历：
     * 跳过尾部空格：从字符串末尾开始，先跳过所有空格字符
     * 统计单词长度：遇到非空格字符开始计数，直到再次遇到空格或到达字符串开头
     * 返回结果：统计的字符数就是最后一个单词的长度
     * 这种方法只需要一次遍历，时间复杂度 O(n)，空间复杂度 O(1)。
     *
     * @param s 输入字符串，由若干单词组成，单词间用空格分隔
     * @return 最后一个单词的长度
     */
    public int lengthOfLastWord(String s) {
        // 获取字符串长度
        int len = s.length();

        // 初始化指针，指向字符串的最后一个字符
        int index = len - 1;

        // 第一步：跳过字符串末尾的所有空格
        // 从后向前遍历，如果遇到空格就继续向前移动
        while (index >= 0 && s.charAt(index) == ' ') {
            index--;
        }

        // 第二步：统计最后一个单词的长度
        // 记录当前单词的长度
        int wordLength = 0;

        // 从最后一个非空格字符开始，向前遍历统计单词长度
        // 当遇到空格或到达字符串开头时停止
        while (index >= 0 && s.charAt(index) != ' ') {
            // 每遇到一个非空格字符，长度加 1
            wordLength++;
            // 指针继续向前移动
            index--;
        }

        // 返回统计得到的单词长度
        return wordLength;
    }
}
