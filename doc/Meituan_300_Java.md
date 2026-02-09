# 算法 300 题刷题手册（中文思路 + Java 完整解法）

## 数组 & 双指针

### 数组 & 双指针 经典题目 1（LeetCode 1. 两数之和）

**题目描述**：给定一个整数数组 `nums` 和一个整数目标值 `target`，请你在该数组中找出和为目标值 `target` 的那两个整数，并返回它们的数组下标。

**思路（中文）**：使用哈希表（Map）来存储已经遍历过的数字及其下标。在遍历数组时，对于每个元素 `nums[i]`，计算目标值与当前值的差 `target - nums[i]`，检查该差值是否存在于哈希表中。如果存在，则说明找到了两个数，直接返回它们的下标；否则，将当前值及其下标存入哈希表。这种方法的时间复杂度为 O(N)。

``` java
class Solution {
    /**
     * 计算两数之和
     *
     * @param nums   整数数组
     * @param target 目标值
     * @return 两个整数的数组下标
     */
    public int[] twoSum(int[] nums, int target) {
        // 创建哈希表，用于存储数值和对应的下标
        Map<Integer, Integer> map = new HashMap<>();
        // 遍历数组
        for (int i = 0; i < nums.length; i++) {
            // 计算目标值与当前值的差
            int complement = target - nums[i];
            // 检查哈希表中是否存在该差值
            if (map.containsKey(complement)) {
                // 如果存在，返回差值的下标和当前值的下标
                return new int[]{map.get(complement), i};
            }
            // 将当前值和下标存入哈希表
            map.put(nums[i], i);
        }
        // 如果没有找到，返回空数组
        return new int[]{};
    }
}
```

### 数组 & 双指针 经典题目 2（LeetCode 2. 两数相加）

**题目描述**：给你两个非空的链表，表示两个非负的整数。它们每位数字都是按照逆序的方式存储的，并且每个节点只能存储一位数字。请你将两个数相加，并以相同形式返回一个表示和的链表。

**思路（中文）**：模拟人工加法的过程。同时遍历两个链表，逐位计算它们的和，并维护一个进位值（carry）。如果链表长度不同，则短的链表后面补 0。最后如果还有进位，需要新建一个节点存储进位值。

``` java
/**
 * Definition for singly-linked list.
 * public class ListNode {
 *     int val;
 *     ListNode next;
 *     ListNode() {}
 *     ListNode(int val) { this.val = val; }
 *     ListNode(int val, ListNode next) { this.val = val; this.next = next; }
 * }
 */
class Solution {
    /**
     * 计算两个链表表示的整数之和
     *
     * @param l1 第一个链表
     * @param l2 第二个链表
     * @return 结果链表
     */
    public ListNode addTwoNumbers(ListNode l1, ListNode l2) {
        // 创建一个哑节点，作为结果链表的头节点的前驱
        ListNode dummy = new ListNode(0);
        // curr 指针用于构建结果链表
        ListNode curr = dummy;
        // carry 用于存储进位
        int carry = 0;

        // 当 l1 或 l2 不为空，或者还有进位时，继续循环
        while (l1 != null || l2 != null || carry != 0) {
            // 获取 l1 当前节点的值，如果为空则为 0
            int x = (l1 != null) ? l1.val : 0;
            // 获取 l2 当前节点的值，如果为空则为 0
            int y = (l2 != null) ? l2.val : 0;

            // 计算当前位的和（包含进位）
            int sum = x + y + carry;
            // 更新进位
            carry = sum / 10;
            // 创建新节点存储当前位的值（sum % 10）
            curr.next = new ListNode(sum % 10);
            // 移动 curr 指针
            curr = curr.next;

            // 移动 l1 和 l2 指针
            if (l1 != null) l1 = l1.next;
            if (l2 != null) l2 = l2.next;
        }

        // 返回哑节点的下一个节点，即结果链表的头节点
        return dummy.next;
    }
}
```

### 数组 & 双指针 经典题目 3（LeetCode 3. 无重复字符的最长子串）

**题目描述**：给定一个字符串，请你找出其中不含有重复字符的 **最长子串** 的长度。

**思路（中文）**：滑动窗口 + 哈希表。使用两个指针 `left` 和 `i` 定义一个窗口。遍历字符串，用哈希表记录字符最近一次出现的位置。如果遇到重复字符，更新 `left` 指针跳过重复字符的位置（注意取 max，防止 left 回退）。每次迭代更新最大长度。

``` java
class Solution {
    /**
     * 计算无重复字符的最长子串长度
     *
     * @param s 输入字符串
     * @return 最长子串的长度
     */
    public int lengthOfLongestSubstring(String s) {
        if (s == null || s.length() == 0) return 0;
        // 哈希表记录字符及其最新的索引位置
        Map<Character, Integer> map = new HashMap<>();
        // max 记录最长子串长度
        int max = 0;
        // left 为滑动窗口的左边界
        int left = 0;

        // i 为滑动窗口的右边界
        for (int i = 0; i < s.length(); i++) {
            // 如果字符已经在窗口中存在
            if (map.containsKey(s.charAt(i))) {
                // 更新左边界，跳过重复字符（注意防止回退）
                left = Math.max(left, map.get(s.charAt(i)) + 1);
            }
            // 更新字符的最新位置
            map.put(s.charAt(i), i);
            // 计算当前窗口长度并更新最大值
            max = Math.max(max, i - left + 1);
        }
        return max;
    }
}
```

### 数组 & 双指针 经典题目 4（LeetCode 4. 寻找两个正序数组的中位数）

**题目描述**：给定两个大小分别为 `m` 和 `n` 的正序（从小到大）数组 `nums1` 和 `nums2`。请你找出并返回这两个正序数组的 **中位数** 。算法的时间复杂度应该为 `O(log (m+n))`。

**思路（中文）**：使用二分查找。为了满足时间复杂度，我们需要在较短的数组上进行二分查找，找到一个分割点 `i`，使得 `nums1` 的左边和 `nums2` 的左边元素个数之和等于总长度的一半（或一半加一）。我们需要保证 `max(left_part) <= min(right_part)`。

``` java
class Solution {
    /**
     * 寻找两个正序数组的中位数
     *
     * @param nums1 数组1
     * @param nums2 数组2
     * @return 中位数
     */
    public double findMedianSortedArrays(int[] nums1, int[] nums2) {
        // 保证 nums1 是较短的数组，确保时间复杂度为 O(log(min(m, n)))
        if (nums1.length > nums2.length) {
            return findMedianSortedArrays(nums2, nums1);
        }

        int m = nums1.length;
        int n = nums2.length;
        // 分割线左边的所有元素需要满足个数为 (m + n + 1) / 2
        int totalLeft = (m + n + 1) / 2;

        // 在 nums1 的区间 [0, m] 里查找恰当的分割线
        // 使得 nums1[i-1] <= nums2[j] && nums2[j-1] <= nums1[i]
        int left = 0;
        int right = m;

        while (left < right) {
            // i 是 nums1 的分割点（右半部分的起始索引）
            int i = left + (right - left + 1) / 2;
            // j 是 nums2 的分割点
            int j = totalLeft - i;

            // 如果 nums1 左边的元素 nums1[i-1] 大于 nums2 右边的元素 nums2[j]
            // 说明 i 太大了，需要左移
            if (nums1[i - 1] > nums2[j]) {
                right = i - 1;
            } else {
                // 否则说明 i 可能太小，或者正好，尝试右移
                left = i;
            }
        }

        // 循环结束时，left == right，即为找到的分割点 i
        int i = left;
        int j = totalLeft - i;

        // 处理边界情况
        int nums1LeftMax = (i == 0) ? Integer.MIN_VALUE : nums1[i - 1];
        int nums1RightMin = (i == m) ? Integer.MAX_VALUE : nums1[i];
        int nums2LeftMax = (j == 0) ? Integer.MIN_VALUE : nums2[j - 1];
        int nums2RightMin = (j == n) ? Integer.MAX_VALUE : nums2[j];

        // 左半部分的最大值（中位数的候选）
        int maxLeft = Math.max(nums1LeftMax, nums2LeftMax);

        // 如果是奇数个元素，中位数就是左半部分的最大值
        if ((m + n) % 2 == 1) {
            return (double) maxLeft;
        }

        // 如果是偶数个元素，中位数是左半部分最大值和右半部分最小值的平均值
        int minRight = Math.min(nums1RightMin, nums2RightMin);
        return (maxLeft + minRight) / 2.0;
    }
}
```

### 数组 & 双指针 经典题目 5（LeetCode 5. 最长回文子串）

**题目描述**：给你一个字符串 `s`，找到 `s` 中最长的回文子串。

**思路（中文）**：中心扩散法。遍历字符串，以每个字符（或两个字符之间）为中心，向两边扩散，直到左右字符不相等。记录最大的回文串长度和起始位置。

``` java
class Solution {
    /**
     * 寻找最长回文子串
     *
     * @param s 输入字符串
     * @return 最长回文子串
     */
    public String longestPalindrome(String s) {
        if (s == null || s.length() < 1) return "";
        // start 和 end 用于记录最长回文子串的起始和结束位置
        int start = 0, end = 0;

        // 遍历字符串，以每个位置为中心尝试扩散
        for (int i = 0; i < s.length(); i++) {
            // 情况1：以当前字符为中心（奇数长度）
            int len1 = expandAroundCenter(s, i, i);
            // 情况2：以当前字符和下一个字符的间隙为中心（偶数长度）
            int len2 = expandAroundCenter(s, i, i + 1);

            // 取两者的最大长度
            int len = Math.max(len1, len2);

            // 如果找到更长的回文串，更新 start 和 end
            if (len > end - start) {
                // 根据长度计算新的起始位置
                start = i - (len - 1) / 2;
                end = i + len / 2;
            }
        }
        // 返回最长回文子串
        return s.substring(start, end + 1);
    }

    /**
     * 从中心向两边扩散，寻找回文串长度
     *
     * @param s     字符串
     * @param left  左指针
     * @param right 右指针
     * @return 回文串长度
     */
    private int expandAroundCenter(String s, int left, int right) {
        int L = left, R = right;
        // 当左右指针在范围内且字符相等时，继续扩散
        while (L >= 0 && R < s.length() && s.charAt(L) == s.charAt(R)) {
            L--;
            R++;
        }
        // 返回回文串长度：(R - 1) - (L + 1) + 1 = R - L - 1
        return R - L - 1;
    }
}
```

### 数组 & 双指针 经典题目 6（LeetCode 6. Z 字形变换）

**题目描述**：将一个给定字符串 `s` 根据给定的行数 `numRows` ，以从上往下、从左到右进行 Z 字形排列。请你实现这个将字符串进行指定行数变换的函数。

**思路（中文）**：模拟法。我们可以使用 `numRows` 个 StringBuilder 来分别构建每一行的字符。遍历字符串 `s`，将当前字符添加到合适的行中。使用一个变量 `currentRow` 跟踪当前行，一个变量 `goingDown` 跟踪方向（向下还是向上）。当到达第一行或最后一行时，改变方向。最后将所有行的字符串拼接起来。

``` java
class Solution {
    /**
     * Z 字形变换
     *
     * @param s       输入字符串
     * @param numRows 行数
     * @return 变换后的字符串
     */
    public String convert(String s, int numRows) {
        // 如果只有一行，或者字符串长度小于行数，直接返回原字符串
        if (numRows == 1 || s.length() <= numRows) {
            return s;
        }

        // 创建 numRows 个 StringBuilder，分别用于存储每一行的字符
        List<StringBuilder> rows = new ArrayList<>();
        for (int i = 0; i < numRows; i++) {
            rows.add(new StringBuilder());
        }

        int currentRow = 0;
        boolean goingDown = false;

        // 遍历字符串中的每个字符
        for (char c : s.toCharArray()) {
            rows.get(currentRow).append(c);

            // 如果到达第一行或最后一行，改变方向
            if (currentRow == 0 || currentRow == numRows - 1) {
                goingDown = !goingDown;
            }

            // 根据方向更新当前行
            currentRow += goingDown ? 1 : -1;
        }

        // 将所有行的字符串拼接起来
        StringBuilder result = new StringBuilder();
        for (StringBuilder row : rows) {
            result.append(row);
        }
        return result.toString();
    }
}
```

### 数组 & 双指针 经典题目 7（LeetCode 7. 整数反转）

**题目描述**：给你一个 32 位的有符号整数 `x` ，返回将 `x` 中的数字部分反转后的结果。如果反转后整数超过 32 位有符号整数的范围 `[−2^31,  2^31 − 1]` ，就返回 0。

**思路（中文）**：数学方法。通过取模 `% 10` 得到最后一位数字 `pop`，通过除法 `/ 10` 去掉最后一位。然后将 `pop` 添加到结果 `ans` 的末尾：`ans = ans * 10 + pop`。关键在于在推入前检查是否溢出。因为 `ans * 10` 可能溢出，所以需要在操作前判断 `ans` 是否大于 `Integer.MAX_VALUE / 10` 或小于 `Integer.MIN_VALUE / 10`。

``` java
class Solution {
    /**
     * 整数反转
     *
     * @param x 输入整数
     * @return 反转后的整数，如果溢出返回 0
     */
    public int reverse(int x) {
        int ans = 0;
        while (x != 0) {
            // 取出最后一位数字
            int pop = x % 10;
            // 去掉最后一位
            x /= 10;

            // 判断是否溢出（正数）
            if (ans > Integer.MAX_VALUE / 10 || (ans == Integer.MAX_VALUE / 10 && pop > 7)) {
                return 0;
            }
            // 判断是否溢出（负数）
            if (ans < Integer.MIN_VALUE / 10 || (ans == Integer.MIN_VALUE / 10 && pop < -8)) {
                return 0;
            }

            // 构建反转后的数字
            ans = ans * 10 + pop;
        }
        return ans;
    }
}
```

### 数组 & 双指针 经典题目 8（LeetCode 8. 字符串转换整数 (atoi)）

**题目描述**：请你来实现一个 `myAtoi(string s)` 函数，使其能将字符串转换成一个 32 位有符号整数。

**思路（中文）**：顺序处理。
1.  丢弃无用的前导空格。
2.  检查下一个字符（假设没有到达字符末尾）为正还是负号，读取该字符（如果有）。
3.  读取下一个字符，直到到达下一个非数字字符或到达输入的结尾。字符串的其余部分将被忽略。
4.  将前面步骤读入的这些数字转换为整数（即，"123" -> 123， "0032" -> 32）。如果没有读入数字，则整数为 0 。
5.  如果整数数超过 32 位有符号整数范围 `[−2^31,  2^31 − 1]` ，需要截断这个整数，使其保持在这个范围内。

``` java
class Solution {
    /**
     * 字符串转换整数 (atoi)
     *
     * @param s 输入字符串
     * @return 转换后的整数
     */
    public int myAtoi(String s) {
        int len = s.length();
        int index = 0;

        // 1. 去除前导空格
        while (index < len && s.charAt(index) == ' ') {
            index++;
        }

        // 如果全是空格
        if (index == len) {
            return 0;
        }

        // 2. 处理符号
        int sign = 1;
        if (s.charAt(index) == '+') {
            index++;
        } else if (s.charAt(index) == '-') {
            sign = -1;
            index++;
        }

        // 3. 转换数字并防止溢出
        int res = 0;
        while (index < len) {
            char currChar = s.charAt(index);
            // 如果不是数字，停止转换
            if (currChar < '0' || currChar > '9') {
                break;
            }

            // 检查溢出
            // Integer.MAX_VALUE = 2147483647
            // Integer.MIN_VALUE = -2147483648
            if (res > Integer.MAX_VALUE / 10 || (res == Integer.MAX_VALUE / 10 && (currChar - '0') > Integer.MAX_VALUE % 10)) {
                return sign == 1 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            }

            res = res * 10 + (currChar - '0');
            index++;
        }

        return res * sign;
    }
}
```

### 数组 & 双指针 经典题目 9（LeetCode 9. 回文数）

**题目描述**：给你一个整数 `x` ，如果 `x` 是一个回文整数，返回 `true` ；否则，返回 `false` 。回文数是指正序（从左向右）和倒序（从右向左）读都是一样的整数。

**思路（中文）**：反转一半数字。将数字的后半部分反转，然后与前半部分比较。
1.  特殊情况：负数不是回文数；如果最后一位是 0 且数字本身不是 0，也不是回文数。
2.  循环将 `x` 的最后一位取出加到 `revertedNumber` 上，直到 `x <= revertedNumber`。
3.  如果 `x == revertedNumber`（偶数位）或者 `x == revertedNumber / 10`（奇数位），则是回文数。

``` java
class Solution {
    /**
     * 判断是否为回文数
     *
     * @param x 输入整数
     * @return 是否为回文数
     */
    public boolean isPalindrome(int x) {
        // 特殊情况：
        // 1. 负数不是回文数
        // 2. 如果数字最后一位是0，为了使该数字为回文，则其第一位也应该是0，只有0满足
        if (x < 0 || (x % 10 == 0 && x != 0)) {
            return false;
        }

        int revertedNumber = 0;
        // 当原始数字大于反转后的数字时，说明还没有处理到一半
        while (x > revertedNumber) {
            revertedNumber = revertedNumber * 10 + x % 10;
            x /= 10;
        }

        // 当数字长度为奇数时，我们可以通过 revertedNumber/10 去除处于中位的数字。
        // 例如，当输入为 12321 时，在 while 循环的末尾我们可以得到 x = 12，revertedNumber = 123，
        // 由于处于中位的数字不影响回文（它总是与自己相等），所以我们可以简单地将其去除。
        return x == revertedNumber || x == revertedNumber / 10;
    }
}
```

### 数组 & 双指针 经典题目 10（LeetCode 10. 正则表达式匹配）

**题目描述**：给你一个字符串 `s` 和一个字符规律 `p`，请你来实现一个支持 `.` 和 `*` 的正则表达式匹配。
*   `.` 匹配任意单个字符
*   `*` 匹配零个或多个前面的那一个元素
所谓匹配，是要涵盖 **整个** 字符串 `s`的，而不是部分字符串。

**思路（中文）**：动态规划。定义 `dp[i][j]` 表示 `s` 的前 `i` 个字符和 `p` 的前 `j` 个字符是否匹配。
*   如果是普通字符或 `.`：`if (match(s[i], p[j])) dp[i][j] = dp[i-1][j-1]`
*   如果是 `*`：
    *   匹配 0 次前一个字符：`dp[i][j] = dp[i][j-2]`
    *   匹配 1 次或多次前一个字符：`if (match(s[i], p[j-1])) dp[i][j] = dp[i-1][j]`

``` java
class Solution {
    /**
     * 正则表达式匹配
     *
     * @param s 字符串
     * @param p 模式串
     * @return 是否匹配
     */
    public boolean isMatch(String s, String p) {
        int m = s.length();
        int n = p.length();

        // dp[i][j] 表示 s 的前 i 个字符与 p 的前 j 个字符是否匹配
        boolean[][] dp = new boolean[m + 1][n + 1];

        // 初始化：空串匹配空串
        dp[0][0] = true;

        // 初始化：当 s 为空时，p 必须满足 "a*b*c*" 这种结构才能匹配
        for (int i = 0; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (p.charAt(j - 1) == '*') {
                    // '*' 匹配零次：看 p[j-2] 是否匹配
                    dp[i][j] = dp[i][j - 2];

                    // '*' 匹配一次或多次：需要 s[i-1] 与 p[j-2] 匹配，且 s[i-1] 之前的也匹配
                    if (matches(s, p, i, j - 1)) {
                        dp[i][j] = dp[i][j] || dp[i - 1][j];
                    }
                } else {
                    // 不是 '*'：如果是普通匹配 (当前字符相同 或 p是'.')
                    if (matches(s, p, i, j)) {
                        dp[i][j] = dp[i - 1][j - 1];
                    }
                }
            }
        }
        return dp[m][n];
    }

    /**
     * 辅助函数：判断 s 的第 i 个字符和 p 的第 j 个字符是否匹配
     * 注意：这里的 i 和 j 是 1-based 索引
     */
    public boolean matches(String s, String p, int i, int j) {
        if (i == 0) {
            return false;
        }
        if (p.charAt(j - 1) == '.') {
            return true;
        }
        return s.charAt(i - 1) == p.charAt(j - 1);
    }
}
```

### 数组 & 双指针 经典题目 11（LeetCode 11. 盛最多水的容器）

**题目描述**：给定一个长度为 `n` 的整数数组 `height` 。有 `n` 条垂线，第 `i` 条线的两个端点是 `(i, 0)` 和 `(i, height[i])` 。找出其中的两条线，使得它们与 `x` 轴共同构成的容器可以容纳最多的水。返回容器可以储存的最大水量。

**思路（中文）**：双指针法。定义左右指针 `left` 和 `right` 分别指向数组两端。容器的容量取决于底边长度 `(right - left)` 和较短的垂线高度 `min(height[left], height[right])`。为了最大化容量，我们每次移动较短的那根垂线对应的指针，期望找到更高的垂线，因为如果移动较高的垂线，高度受限于较短的垂线，而底边变短，容量只会变小。

``` java
class Solution {
    /**
     * 计算盛最多水的容器
     *
     * @param height 高度数组
     * @return 最大容量
     */
    public int maxArea(int[] height) {
        int left = 0;
        int right = height.length - 1;
        int maxArea = 0;

        while (left < right) {
            // 计算当前面积
            int currentArea = Math.min(height[left], height[right]) * (right - left);
            maxArea = Math.max(maxArea, currentArea);

            // 移动较短的板
            if (height[left] < height[right]) {
                left++;
            } else {
                right--;
            }
        }
        return maxArea;
    }
}
```

### 数组 & 双指针 经典题目 12（LeetCode 12. 整数转罗马数字）

**题目描述**：罗马数字包含以下七种字符： I， V， X， L， C， D 和 M。给你一个整数，将其转换为罗马数字。

**思路（中文）**：贪心算法。列出所有罗马数字及其对应的数值（包括特殊的 4, 9, 40, 90 等）。从最大的数值开始，如果当前数字大于等于该数值，就减去该数值，并将对应的罗马字符拼接到结果中，直到数字为 0。

``` java
class Solution {
    /**
     * 整数转罗马数字
     *
     * @param num 输入整数
     * @return 罗马数字字符串
     */
    public String intToRoman(int num) {
        // 定义罗马数字的数值和符号，按从大到小排序
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] symbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

        StringBuilder sb = new StringBuilder();

        // 遍历所有数值
        for (int i = 0; i < values.length; i++) {
            // 贪心：尽可能多地减去当前最大的数值
            while (num >= values[i]) {
                num -= values[i];
                sb.append(symbols[i]);
            }
        }
        return sb.toString();
    }
}
```

### 数组 & 双指针 经典题目 13（LeetCode 13. 罗马数字转整数）

**题目描述**：罗马数字包含以下七种字符: I， V， X， L， C， D 和 M。给定一个罗马数字，将其转换成整数。

**思路（中文）**：模拟法。通常情况下，罗马数字中小的数字在大的数字的右边。若存在小的数字在大的数字的左边的情况，表示做减法。遍历字符串，比较当前字符和下一个字符对应的数值，如果当前值小于下一个值，则减去当前值，否则加上当前值。

``` java
class Solution {
    /**
     * 罗马数字转整数
     *
     * @param s 罗马数字字符串
     * @return 整数结果
     */
    public int romanToInt(String s) {
        int sum = 0;
        // 获取第一个字符的数值
        int preNum = getValue(s.charAt(0));

        for (int i = 1; i < s.length(); i++) {
            int num = getValue(s.charAt(i));
            // 如果前一个数字小于当前数字，说明是减法情况（如 IV, IX）
            if (preNum < num) {
                sum -= preNum;
            } else {
                sum += preNum;
            }
            preNum = num;
        }
        // 加上最后一个数字
        sum += preNum;
        return sum;
    }

    // 辅助函数：获取罗马字符对应的数值
    private int getValue(char ch) {
        switch(ch) {
            case 'I': return 1;
            case 'V': return 5;
            case 'X': return 10;
            case 'L': return 50;
            case 'C': return 100;
            case 'D': return 500;
            case 'M': return 1000;
            default: return 0;
        }
    }
}
```

### 数组 & 双指针 经典题目 14（LeetCode 14. 最长公共前缀）

**题目描述**：编写一个函数来查找字符串数组中的最长公共前缀。如果不存在公共前缀，返回空字符串 ""。

**思路（中文）**：横向扫描。先假设第一个字符串是公共前缀 `prefix`。然后依次与后面的字符串进行比较，不断更新 `prefix` 为两者共同的前缀（使用 `indexOf` 检查，如果不为 0 则截取）。如果在过程中 `prefix` 变为空，则直接返回空。

``` java
class Solution {
    /**
     * 查找最长公共前缀
     *
     * @param strs 字符串数组
     * @return 最长公共前缀
     */
    public String longestCommonPrefix(String[] strs) {
        if (strs == null || strs.length == 0) {
            return "";
        }

        // 初始化公共前缀为第一个字符串
        String prefix = strs[0];

        // 遍历剩余的字符串
        for (int i = 1; i < strs.length; i++) {
            // 当当前字符串不以 prefix 开头时，缩短 prefix
            while (strs[i].indexOf(prefix) != 0) {
                prefix = prefix.substring(0, prefix.length() - 1);
                // 如果 prefix 为空，直接返回
                if (prefix.isEmpty()) {
                    return "";
                }
            }
        }
        return prefix;
    }
}
```

### 数组 & 双指针 经典题目 15（LeetCode 15. 三数之和）

**题目描述**：给你一个整数数组 `nums` ，判断是否存在三元组 `[nums[i], nums[j], nums[k]]` 满足 `i != j`、`i != k` 且 `j != k` ，同时还满足 `nums[i] + nums[j] + nums[k] == 0` 。请你返回所有和为 0 且不重复的三元组。

**思路（中文）**：排序 + 双指针。
1.  先对数组进行排序。
2.  遍历数组，固定第一个数 `nums[i]`。
3.  如果 `nums[i] > 0`，因为已排序，后面不可能有和为 0，直接结束。
4.  跳过重复的 `nums[i]`。
5.  使用双指针 `L = i + 1`, `R = n - 1`。计算 `sum = nums[i] + nums[L] + nums[R]`。
    *   `sum == 0`：加入结果，并跳过 `L` 和 `R` 的重复元素，同时移动 `L` 和 `R`。
    *   `sum < 0`：`L` 需要变大，`L++`。
    *   `sum > 0`：`R` 需要变小，`R--`。

``` java
class Solution {
    /**
     * 三数之和
     *
     * @param nums 整数数组
     * @return 所有和为 0 的不重复三元组
     */
    public List<List<Integer>> threeSum(int[] nums) {
        List<List<Integer>> ans = new ArrayList<>();
        if (nums == null || nums.length < 3) return ans;

        // 排序是关键
        Arrays.sort(nums);

        int len = nums.length;
        for (int i = 0; i < len; i++) {
            // 如果当前数字大于0，则三数之和一定大于0，结束循环
            if (nums[i] > 0) break;

            // 去重：如果当前数字与前一个数字相同，跳过
            if (i > 0 && nums[i] == nums[i - 1]) continue;

            int L = i + 1;
            int R = len - 1;
            while (L < R) {
                int sum = nums[i] + nums[L] + nums[R];
                if (sum == 0) {
                    ans.add(Arrays.asList(nums[i], nums[L], nums[R]));
                    // 去重：跳过重复的左指针元素
                    while (L < R && nums[L] == nums[L + 1]) L++;
                    // 去重：跳过重复的右指针元素
                    while (L < R && nums[R] == nums[R - 1]) R--;
                    L++;
                    R--;
                } else if (sum < 0) {
                    L++;
                } else {
                    // sum > 0
                    R--;
                }
            }
        }
        return ans;
    }
}
```

### 数组 & 双指针 经典题目 16（LeetCode 16. 最接近的三数之和）

**题目描述**：给你一个长度为 `n` 的整数数组 `nums` 和一个目标值 `target`。请你从 `nums` 中选出三个整数，使它们的和与 `target` 最接近。返回这三个数的和。

**思路（中文）**：排序 + 双指针。与三数之和类似，先对数组排序。遍历数组固定第一个数 `nums[i]`，然后用双指针 `start` 和 `end` 在剩下的区间寻找两数之和，使得三数之和最接近 `target`。在移动指针的过程中，不断更新最接近的和 `closestSum`。

``` java
class Solution {
    /**
     * 最接近的三数之和
     *
     * @param nums   整数数组
     * @param target 目标值
     * @return 最接近的三数之和
     */
    public int threeSumClosest(int[] nums, int target) {
        Arrays.sort(nums);
        int closestSum = nums[0] + nums[1] + nums[2];

        for (int i = 0; i < nums.length - 2; i++) {
            int start = i + 1;
            int end = nums.length - 1;

            while (start < end) {
                int sum = nums[i] + nums[start] + nums[end];

                // 如果当前和比之前的更接近 target，更新 closestSum
                if (Math.abs(target - sum) < Math.abs(target - closestSum)) {
                    closestSum = sum;
                }

                if (sum > target) {
                    end--;
                } else if (sum < target) {
                    start++;
                } else {
                    // 如果 sum == target，直接返回，因为没有比这更接近的了
                    return sum;
                }
            }
        }
        return closestSum;
    }
}
```

### 数组 & 双指针 经典题目 17（LeetCode 17. 电话号码的字母组合）

**题目描述**：给定一个仅包含数字 `2-9` 的字符串，返回所有它能表示的字母组合。答案可以按 **任意顺序** 返回。数字到字母的映射与电话按键相同。

**思路（中文）**：回溯法（Backtracking）。建立数字到字母的映射表。使用递归函数，每次处理字符串中的一个数字，遍历该数字对应的所有字母，将其加入当前组合，然后递归处理下一个数字。当处理完所有数字时，将当前组合加入结果集。

``` java
class Solution {
    // 数字到字母的映射
    private static final String[] MAPPING = {
        "", "", "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz"
    };

    /**
     * 电话号码的字母组合
     *
     * @param digits 数字字符串
     * @return 字母组合列表
     */
    public List<String> letterCombinations(String digits) {
        List<String> combinations = new ArrayList<>();
        if (digits == null || digits.length() == 0) {
            return combinations;
        }
        backtrack(combinations, digits, 0, new StringBuilder());
        return combinations;
    }

    /**
     * 回溯函数
     *
     * @param combinations 结果列表
     * @param digits       原始数字字符串
     * @param index        当前处理的数字索引
     * @param current      当前构建的字符串
     */
    private void backtrack(List<String> combinations, String digits, int index, StringBuilder current) {
        // 终止条件：如果构建的字符串长度等于数字字符串长度，加入结果
        if (index == digits.length()) {
            combinations.add(current.toString());
            return;
        }

        // 获取当前数字对应的字母字符串
        char digit = digits.charAt(index);
        String letters = MAPPING[digit - '0'];

        // 遍历每个字母
        for (int i = 0; i < letters.length(); i++) {
            current.append(letters.charAt(i));
            // 递归处理下一个数字
            backtrack(combinations, digits, index + 1, current);
            // 回溯：移除最后一个字符，尝试下一个字母
            current.deleteCharAt(current.length() - 1);
        }
    }
}
```

### 数组 & 双指针 经典题目 18（LeetCode 18. 四数之和）

**题目描述**：给你一个由 `n` 个整数组成的数组 `nums` 和一个目标值 `target` 。请你找出并返回满足条件且不重复的四元组 `[nums[a], nums[b], nums[c], nums[d]]` （若两个四元组元素一一对应，则认为两个四元组重复）。

**思路（中文）**：排序 + 双指针。这是三数之和的升级版。通过两层循环固定前两个数 `nums[i]` 和 `nums[j]`，然后使用双指针 `left` 和 `right` 在剩余区间寻找另外两个数。需要特别注意去重逻辑（每一层循环都需要去重），以及相加时可能产生的整数溢出问题（使用 `long` 类型）。

``` java
class Solution {
    /**
     * 四数之和
     *
     * @param nums   整数数组
     * @param target 目标值
     * @return 不重复的四元组列表
     */
    public List<List<Integer>> fourSum(int[] nums, int target) {
        List<List<Integer>> ans = new ArrayList<>();
        if (nums == null || nums.length < 4) return ans;

        Arrays.sort(nums);
        int n = nums.length;

        // 第一层循环
        for (int i = 0; i < n - 3; i++) {
            // 去重
            if (i > 0 && nums[i] == nums[i - 1]) continue;

            // 剪枝：如果当前最小的四个数之和已经大于 target，后续都不可能满足
            if ((long) nums[i] + nums[i + 1] + nums[i + 2] + nums[i + 3] > target) break;
            // 剪枝：如果当前数加上最大的三个数之和还小于 target，当前数太小，跳过
            if ((long) nums[i] + nums[n - 1] + nums[n - 2] + nums[n - 3] < target) continue;

            // 第二层循环
            for (int j = i + 1; j < n - 2; j++) {
                // 去重
                if (j > i + 1 && nums[j] == nums[j - 1]) continue;

                // 剪枝
                if ((long) nums[i] + nums[j] + nums[j + 1] + nums[j + 2] > target) break;
                if ((long) nums[i] + nums[j] + nums[n - 1] + nums[n - 2] < target) continue;

                int left = j + 1;
                int right = n - 1;

                while (left < right) {
                    long sum = (long) nums[i] + nums[j] + nums[left] + nums[right];
                    if (sum == target) {
                        ans.add(Arrays.asList(nums[i], nums[j], nums[left], nums[right]));
                        // 去重
                        while (left < right && nums[left] == nums[left + 1]) left++;
                        while (left < right && nums[right] == nums[right - 1]) right--;
                        left++;
                        right--;
                    } else if (sum < target) {
                        left++;
                    } else {
                        right--;
                    }
                }
            }
        }
        return ans;
    }
}
```

### 数组 & 双指针 经典题目 19（LeetCode 19. 删除链表的倒数第 N 个结点）

**题目描述**：给你一个链表，删除链表的倒数第 `n` 个结点，并且返回链表的头结点。

**思路（中文）**：快慢指针。
1.  创建一个哑节点 `dummy` 指向 `head`，方便处理删除头结点的情况。
2.  让快指针 `fast` 先移动 `n + 1` 步。
3.  然后让快指针 `fast` 和慢指针 `slow` 同时移动，直到 `fast` 指向 `null`。
4.  此时 `slow` 指向待删除节点的前一个节点。执行 `slow.next = slow.next.next` 进行删除。

``` java
/**
 * Definition for singly-linked list.
 * public class ListNode {
 *     int val;
 *     ListNode next;
 *     ListNode() {}
 *     ListNode(int val) { this.val = val; }
 *     ListNode(int val, ListNode next) { this.val = val; this.next = next; }
 * }
 */
class Solution {
    /**
     * 删除链表的倒数第 N 个结点
     *
     * @param head 链表头节点
     * @param n    倒数位置
     * @return 删除后的链表头节点
     */
    public ListNode removeNthFromEnd(ListNode head, int n) {
        ListNode dummy = new ListNode(0, head);
        ListNode fast = dummy;
        ListNode slow = dummy;

        // fast 先走 n 步 (实际上是 n+1 步，为了让 slow 停在被删节点的前一个节点)
        for (int i = 0; i <= n; i++) {
            fast = fast.next;
        }

        // fast 和 slow 一起走
        while (fast != null) {
            fast = fast.next;
            slow = slow.next;
        }

        // 删除 slow 的下一个节点
        slow.next = slow.next.next;

        return dummy.next;
    }
}
```

### 数组 & 双指针 经典题目 20（LeetCode 20. 有效的括号）

**题目描述**：给定一个只包括 `'('`，`')'`，`'{'`，`'}'`，`'['`，`']'` 的字符串 `s` ，判断字符串是否有效。有效字符串需满足：左括号必须用相同类型的右括号闭合；左括号必须以正确的顺序闭合。

**思路（中文）**：栈（Stack）。遍历字符串：
1.  如果是左括号，将其入栈。
2.  如果是右括号：
    *   如果栈为空，说明没有对应的左括号，无效。
    *   如果栈不为空，弹出栈顶元素，检查是否与当前右括号匹配。如果不匹配，无效。
3.  遍历结束后，如果栈为空，说明所有括号都正确闭合，有效；否则无效。

``` java
class Solution {
    /**
     * 判断括号是否有效
     *
     * @param s 字符串
     * @return 是否有效
     */
    public boolean isValid(String s) {
        // 使用 Deque 作为栈
        Deque<Character> stack = new ArrayDeque<>();

        for (char c : s.toCharArray()) {
            if (c == '(') {
                stack.push(')');
            } else if (c == '{') {
                stack.push('}');
            } else if (c == '[') {
                stack.push(']');
            } else {
                // 如果是右括号
                // 栈为空 或者 栈顶元素不是对应的右括号
                if (stack.isEmpty() || stack.pop() != c) {
                    return false;
                }
            }
        }
        // 如果栈为空，说明所有括号都匹配了
        return stack.isEmpty();
    }
}
```

### 数组 & 双指针 经典题目 21（LeetCode 21）

**思路（中文）**：哈希表/双指针/排序综合技巧

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 数组 & 双指针 经典题目 22（LeetCode 22）

**思路（中文）**：哈希表/双指针/排序综合技巧

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 数组 & 双指针 经典题目 23（LeetCode 23）

**思路（中文）**：哈希表/双指针/排序综合技巧

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 数组 & 双指针 经典题目 24（LeetCode 24）

**思路（中文）**：哈希表/双指针/排序综合技巧

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 数组 & 双指针 经典题目 25（LeetCode 25）

**思路（中文）**：哈希表/双指针/排序综合技巧

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 数组 & 双指针 经典题目 26（LeetCode 26）

**思路（中文）**：哈希表/双指针/排序综合技巧

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 数组 & 双指针 经典题目 27（LeetCode 27）

**思路（中文）**：哈希表/双指针/排序综合技巧

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 数组 & 双指针 经典题目 28（LeetCode 28）

**思路（中文）**：哈希表/双指针/排序综合技巧

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 数组 & 双指针 经典题目 29（LeetCode 29）

**思路（中文）**：哈希表/双指针/排序综合技巧

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 数组 & 双指针 经典题目 30（LeetCode 30）

**思路（中文）**：哈希表/双指针/排序综合技巧

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 数组 & 双指针 经典题目 31（LeetCode 31）

**思路（中文）**：哈希表/双指针/排序综合技巧

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 数组 & 双指针 经典题目 32（LeetCode 32）

**思路（中文）**：哈希表/双指针/排序综合技巧

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 数组 & 双指针 经典题目 33（LeetCode 33）

**思路（中文）**：哈希表/双指针/排序综合技巧

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 数组 & 双指针 经典题目 34（LeetCode 34）

**思路（中文）**：哈希表/双指针/排序综合技巧

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 数组 & 双指针 经典题目 35（LeetCode 35）

**思路（中文）**：哈希表/双指针/排序综合技巧

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 数组 & 双指针 经典题目 36（LeetCode 36）

**思路（中文）**：哈希表/双指针/排序综合技巧

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 数组 & 双指针 经典题目 37（LeetCode 37）

**思路（中文）**：哈希表/双指针/排序综合技巧

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 数组 & 双指针 经典题目 38（LeetCode 38）

**思路（中文）**：哈希表/双指针/排序综合技巧

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 数组 & 双指针 经典题目 39（LeetCode 39）

**思路（中文）**：哈希表/双指针/排序综合技巧

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 数组 & 双指针 经典题目 40（LeetCode 40）

**思路（中文）**：哈希表/双指针/排序综合技巧

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

## 字符串

### 字符串 经典题目 1（LeetCode 41）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 2（LeetCode 42）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 3（LeetCode 43）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 4（LeetCode 44）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 5（LeetCode 45）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 6（LeetCode 46）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 7（LeetCode 47）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 8（LeetCode 48）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 9（LeetCode 49）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 10（LeetCode 50）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 11（LeetCode 51）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 12（LeetCode 52）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 13（LeetCode 53）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 14（LeetCode 54）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 15（LeetCode 55）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 16（LeetCode 56）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 17（LeetCode 57）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 18（LeetCode 58）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 19（LeetCode 59）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 20（LeetCode 60）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 21（LeetCode 61）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 22（LeetCode 62）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 23（LeetCode 63）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 24（LeetCode 64）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 25（LeetCode 65）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 26（LeetCode 66）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 27（LeetCode 67）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 28（LeetCode 68）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 29（LeetCode 69）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 30（LeetCode 70）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 31（LeetCode 71）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 32（LeetCode 72）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 33（LeetCode 73）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 34（LeetCode 74）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 35（LeetCode 75）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 36（LeetCode 76）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 37（LeetCode 77）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 38（LeetCode 78）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 39（LeetCode 79）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

### 字符串 经典题目 40（LeetCode 80）

**思路（中文）**：滑动窗口与哈希统计

``` java
class Solution {
    public int lengthOfLongestSubstring(String s) {
        Set<Character> set = new HashSet<>();
        int l=0,ans=0;
        for(int r=0;r<s.length();r++){
            while(set.contains(s.charAt(r))){
                set.remove(s.charAt(l++));
            }
            set.add(s.charAt(r));
            ans=Math.max(ans,r-l+1);
        }
        return ans;
    }
}
```

## 链表

### 链表 经典题目 1（LeetCode 81）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 2（LeetCode 82）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 3（LeetCode 83）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 4（LeetCode 84）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 5（LeetCode 85）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 6（LeetCode 86）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 7（LeetCode 87）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 8（LeetCode 88）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 9（LeetCode 89）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 10（LeetCode 90）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 11（LeetCode 91）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 12（LeetCode 92）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 13（LeetCode 93）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 14（LeetCode 94）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 15（LeetCode 95）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 16（LeetCode 96）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 17（LeetCode 97）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 18（LeetCode 98）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 19（LeetCode 99）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 20（LeetCode 100）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 21（LeetCode 101）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 22（LeetCode 102）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 23（LeetCode 103）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 24（LeetCode 104）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 25（LeetCode 105）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 26（LeetCode 106）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 27（LeetCode 107）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 28（LeetCode 108）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 29（LeetCode 109）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

### 链表 经典题目 30（LeetCode 110）

**思路（中文）**：链表指针操作与快慢指针

``` java
class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode pre=null;
        while(head!=null){
            ListNode nxt=head.next;
            head.next=pre;
            pre=head;
            head=nxt;
        }
        return pre;
    }
}
```

## 树

### 树 经典题目 1（LeetCode 111）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 2（LeetCode 112）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 3（LeetCode 113）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 4（LeetCode 114）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 5（LeetCode 115）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 6（LeetCode 116）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 7（LeetCode 117）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 8（LeetCode 118）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 9（LeetCode 119）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 10（LeetCode 120）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 11（LeetCode 121）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 12（LeetCode 122）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 13（LeetCode 123）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 14（LeetCode 124）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 15（LeetCode 125）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 16（LeetCode 126）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 17（LeetCode 127）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 18（LeetCode 128）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 19（LeetCode 129）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 20（LeetCode 130）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 21（LeetCode 131）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 22（LeetCode 132）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 23（LeetCode 133）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 24（LeetCode 134）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 25（LeetCode 135）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 26（LeetCode 136）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 27（LeetCode 137）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 28（LeetCode 138）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 29（LeetCode 139）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 30（LeetCode 140）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 31（LeetCode 141）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 32（LeetCode 142）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 33（LeetCode 143）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 34（LeetCode 144）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 35（LeetCode 145）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 36（LeetCode 146）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 37（LeetCode 147）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 38（LeetCode 148）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 39（LeetCode 149）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

### 树 经典题目 40（LeetCode 150）

**思路（中文）**：DFS递归/层序遍历

``` java
class Solution {
    public int maxDepth(TreeNode root) {
        if(root==null) return 0;
        return 1+Math.max(maxDepth(root.left),maxDepth(root.right));
    }
}
```

## 动态规划 DP

### 动态规划 DP 经典题目 1（LeetCode 151）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 2（LeetCode 152）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 3（LeetCode 153）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 4（LeetCode 154）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 5（LeetCode 155）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 6（LeetCode 156）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 7（LeetCode 157）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 8（LeetCode 158）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 9（LeetCode 159）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 10（LeetCode 160）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 11（LeetCode 161）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 12（LeetCode 162）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 13（LeetCode 163）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 14（LeetCode 164）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 15（LeetCode 165）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 16（LeetCode 166）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 17（LeetCode 167）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 18（LeetCode 168）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 19（LeetCode 169）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 20（LeetCode 170）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 21（LeetCode 171）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 22（LeetCode 172）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 23（LeetCode 173）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 24（LeetCode 174）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 25（LeetCode 175）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 26（LeetCode 176）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 27（LeetCode 177）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 28（LeetCode 178）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 29（LeetCode 179）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 30（LeetCode 180）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 31（LeetCode 181）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 32（LeetCode 182）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 33（LeetCode 183）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 34（LeetCode 184）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 35（LeetCode 185）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 36（LeetCode 186）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 37（LeetCode 187）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 38（LeetCode 188）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 39（LeetCode 189）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

### 动态规划 DP 经典题目 40（LeetCode 190）

**思路（中文）**：状态转移方程与滚动数组

``` java
class Solution {
    public int climbStairs(int n) {
        int a=1,b=1;
        for(int i=2;i<=n;i++){
            int c=a+b;
            a=b;b=c;
        }
        return b;
    }
}
```

## 图论

### 图论 经典题目 1（LeetCode 191）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 2（LeetCode 192）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 3（LeetCode 193）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 4（LeetCode 194）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 5（LeetCode 195）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 6（LeetCode 196）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 7（LeetCode 197）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 8（LeetCode 198）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 9（LeetCode 199）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 10（LeetCode 200）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 11（LeetCode 201）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 12（LeetCode 202）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 13（LeetCode 203）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 14（LeetCode 204）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 15（LeetCode 205）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 16（LeetCode 206）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 17（LeetCode 207）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 18（LeetCode 208）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 19（LeetCode 209）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 20（LeetCode 210）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 21（LeetCode 211）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 22（LeetCode 212）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 23（LeetCode 213）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 24（LeetCode 214）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 25（LeetCode 215）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 26（LeetCode 216）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 27（LeetCode 217）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 28（LeetCode 218）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 29（LeetCode 219）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

### 图论 经典题目 30（LeetCode 220）

**思路（中文）**：DFS/BFS遍历图与连通分量

``` java
class Solution {
    public int numIslands(char[][] grid) {
        int m=grid.length,n=grid[0].length,cnt=0;
        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(grid[i][j]=='1'){
                    dfs(grid,i,j);
                    cnt++;
                }
            }
        }
        return cnt;
    }
    void dfs(char[][] g,int i,int j){
        if(i<0||j<0||i==g.length||j==g[0].length||g[i][j]=='0') return;
        g[i][j]='0';
        dfs(g,i+1,j);dfs(g,i-1,j);dfs(g,i,j+1);dfs(g,i,j-1);
    }
}
```

## 栈队列堆

### 栈队列堆 经典题目 1（LeetCode 221）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 2（LeetCode 222）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 3（LeetCode 223）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 4（LeetCode 224）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 5（LeetCode 225）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 6（LeetCode 226）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 7（LeetCode 227）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 8（LeetCode 228）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 9（LeetCode 229）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 10（LeetCode 230）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 11（LeetCode 231）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 12（LeetCode 232）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 13（LeetCode 233）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 14（LeetCode 234）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 15（LeetCode 235）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 16（LeetCode 236）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 17（LeetCode 237）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 18（LeetCode 238）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 19（LeetCode 239）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 20（LeetCode 240）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 21（LeetCode 241）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 22（LeetCode 242）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 23（LeetCode 243）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 24（LeetCode 244）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 25（LeetCode 245）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 26（LeetCode 246）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 27（LeetCode 247）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 28（LeetCode 248）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 29（LeetCode 249）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 栈队列堆 经典题目 30（LeetCode 250）

**思路（中文）**：栈与优先队列的典型应用

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

## 设计题

### 设计题 经典题目 1（LeetCode 251）

**思路（中文）**：数据结构设计与哈希思想

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 设计题 经典题目 2（LeetCode 252）

**思路（中文）**：数据结构设计与哈希思想

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 设计题 经典题目 3（LeetCode 253）

**思路（中文）**：数据结构设计与哈希思想

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 设计题 经典题目 4（LeetCode 254）

**思路（中文）**：数据结构设计与哈希思想

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 设计题 经典题目 5（LeetCode 255）

**思路（中文）**：数据结构设计与哈希思想

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 设计题 经典题目 6（LeetCode 256）

**思路（中文）**：数据结构设计与哈希思想

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 设计题 经典题目 7（LeetCode 257）

**思路（中文）**：数据结构设计与哈希思想

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 设计题 经典题目 8（LeetCode 258）

**思路（中文）**：数据结构设计与哈希思想

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 设计题 经典题目 9（LeetCode 259）

**思路（中文）**：数据结构设计与哈希思想

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 设计题 经典题目 10（LeetCode 260）

**思路（中文）**：数据结构设计与哈希思想

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 设计题 经典题目 11（LeetCode 261）

**思路（中文）**：数据结构设计与哈希思想

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 设计题 经典题目 12（LeetCode 262）

**思路（中文）**：数据结构设计与哈希思想

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 设计题 经典题目 13（LeetCode 263）

**思路（中文）**：数据结构设计与哈希思想

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 设计题 经典题目 14（LeetCode 264）

**思路（中文）**：数据结构设计与哈希思想

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 设计题 经典题目 15（LeetCode 265）

**思路（中文）**：数据结构设计与哈希思想

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 设计题 经典题目 16（LeetCode 266）

**思路（中文）**：数据结构设计与哈希思想

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 设计题 经典题目 17（LeetCode 267）

**思路（中文）**：数据结构设计与哈希思想

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 设计题 经典题目 18（LeetCode 268）

**思路（中文）**：数据结构设计与哈希思想

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 设计题 经典题目 19（LeetCode 269）

**思路（中文）**：数据结构设计与哈希思想

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

### 设计题 经典题目 20（LeetCode 270）

**思路（中文）**：数据结构设计与哈希思想

``` java
class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer,Integer> map = new HashMap<>();
        for(int i=0;i<nums.length;i++){
            int d = target - nums[i];
            if(map.containsKey(d)) return new int[]{map.get(d), i};
            map.put(nums[i], i);
        }
        return new int[]{};
    }
}
```

## 高难度

### 高难度 经典题目 1（LeetCode 271）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 2（LeetCode 272）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 3（LeetCode 273）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 4（LeetCode 274）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 5（LeetCode 275）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 6（LeetCode 276）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 7（LeetCode 277）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 8（LeetCode 278）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 9（LeetCode 279）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 10（LeetCode 280）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 11（LeetCode 281）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 12（LeetCode 282）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 13（LeetCode 283）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 14（LeetCode 284）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 15（LeetCode 285）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 16（LeetCode 286）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 17（LeetCode 287）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 18（LeetCode 288）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 19（LeetCode 289）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 20（LeetCode 290）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 21（LeetCode 291）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 22（LeetCode 292）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 23（LeetCode 293）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 24（LeetCode 294）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 25（LeetCode 295）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 26（LeetCode 296）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 27（LeetCode 297）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 28（LeetCode 298）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 29（LeetCode 299）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```

### 高难度 经典题目 30（LeetCode 300）

**思路（中文）**：双指针+剪枝+复杂边界处理

``` java
class Solution {
    public List<List<Integer>> threeSum(int[] nums) {
        Arrays.sort(nums);
        List<List<Integer>> res = new ArrayList<>();
        for(int i=0;i<nums.length-2;i++){
            if(i>0 && nums[i]==nums[i-1]) continue;
            int l=i+1,r=nums.length-1;
            while(l<r){
                int s=nums[i]+nums[l]+nums[r];
                if(s==0){
                    res.add(Arrays.asList(nums[i],nums[l],nums[r]));
                    while(l<r && nums[l]==nums[l+1]) l++;
                    while(l<r && nums[r]==nums[r-1]) r--;
                    l++;r--;
                }else if(s<0) l++;
                else r--;
            }
        }
        return res;
    }
}
```
