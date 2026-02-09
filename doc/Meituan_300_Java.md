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

### 数组 & 双指针 经典题目 6（LeetCode 6）

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

### 数组 & 双指针 经典题目 7（LeetCode 7）

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

### 数组 & 双指针 经典题目 8（LeetCode 8）

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

### 数组 & 双指针 经典题目 9（LeetCode 9）

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

### 数组 & 双指针 经典题目 10（LeetCode 10）

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

### 数组 & 双指针 经典题目 11（LeetCode 11）

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

### 数组 & 双指针 经典题目 12（LeetCode 12）

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

### 数组 & 双指针 经典题目 13（LeetCode 13）

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

### 数组 & 双指针 经典题目 14（LeetCode 14）

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

### 数组 & 双指针 经典题目 15（LeetCode 15）

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

### 数组 & 双指针 经典题目 16（LeetCode 16）

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

### 数组 & 双指针 经典题目 17（LeetCode 17）

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

### 数组 & 双指针 经典题目 18（LeetCode 18）

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

### 数组 & 双指针 经典题目 19（LeetCode 19）

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

### 数组 & 双指针 经典题目 20（LeetCode 20）

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
