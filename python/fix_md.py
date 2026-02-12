import os
import re


def beautify_markdown(input_file, output_file):
    """
    读取 Markdown 文件，补全标签，并在解答区插入 Java 代码块，最后美化排版。
    """
    if not os.path.exists(input_file):
        print(f"❌ 错误：找不到文件 '{input_file}'")
        return

    print(f"📂 正在处理：{input_file} ...")

    with open(input_file, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    formatted_lines = []
    current_block = []

    # ==========================================
    # 核心处理函数：处理单个题目块
    # ==========================================
    def process_block(block):
        if not block:
            return []

        # 1. 提取标题
        title_line = block[0].strip()
        body = block[1:]

        # 2. 扫描现有的标签位置
        desc_idx = -1
        sol_idx = -1

        for i, line in enumerate(body):
            if "#### 📝 问题描述" in line:
                desc_idx = i
            elif "#### 💻 问题解答 (Java)" in line:
                sol_idx = i

        # 3. 开始构建新的块内容
        new_block = []
        new_block.append(title_line)  # 添加题目 H3 标题
        new_block.append("")  # 标题下空一行

        # --- A. 处理“问题描述” ---
        new_block.append("#### 📝 问题描述")

        # 提取原有描述内容
        desc_content = []
        start = desc_idx + 1 if desc_idx != -1 else 0
        end = sol_idx if sol_idx != -1 else len(body)

        if desc_idx != -1:
            # 如果原文本里有描述标签，提取中间的内容
            for line in body[start:end]:
                if line.strip():  # 只保留非空行，后面统一控制格式
                    desc_content.append(line.rstrip())

        if desc_content:
            new_block.append("")
            new_block.extend(desc_content)
        else:
            new_block.append("")  # 如果没内容，留一个空行占位

        # --- B. 处理“问题解答” ---
        new_block.append("")
        new_block.append("#### 💻 问题解答 (Java)")

        sol_content = []
        if sol_idx != -1:
            # 提取原有的解答内容
            for line in body[sol_idx + 1:]:
                sol_content.append(line.rstrip())

        # 检测是否已经存在代码块 (```)
        has_code_block = any("```" in line for line in sol_content)

        # 添加原有内容（去除原有内容开头过多的空行）
        if sol_content:
            # 过滤掉内容前的纯空行
            while sol_content and not sol_content[0].strip():
                sol_content.pop(0)
            if sol_content:
                new_block.append("")
                new_block.extend(sol_content)

        # --- C. 自动补充代码块 ---
        # 如果解答区没有代码块，插入模板
        if not has_code_block:
            new_block.append("")
            new_block.append("```Java")
            new_block.append("// TODO: 待补充代码")
            new_block.append("class Solution {")
            new_block.append("    ")
            new_block.append("}")
            new_block.append("```")

        new_block.append("")  # 题目块结束，空一行
        return new_block

    # ==========================================
    # 主循环：按行读取并分块
    # ==========================================
    for line in lines:
        stripped = line.strip()

        # 判断是否是题目标题 (### 开头，且不是 ####)
        is_h3_problem = stripped.startswith("### ") and not stripped.startswith("####")
        # 判断是否是大的章节标题 (如 # leetcode 题库, ## 难度等级)
        is_major_header = stripped.startswith("# ") or stripped.startswith("## ")

        if is_h3_problem:
            # 遇到新题目：处理并保存上一个题目块
            if current_block:
                formatted_lines.extend(process_block(current_block))
                current_block = []
            # 开始新块
            current_block.append(line)

        elif is_major_header:
            # 遇到大标题：先结束当前题目块
            if current_block:
                formatted_lines.extend(process_block(current_block))
                current_block = []
            # 直接写入大标题，并加空行
            formatted_lines.append("")
            formatted_lines.append(line.strip())
            formatted_lines.append("")

        else:
            # 普通行
            if current_block:
                # 如果在题目块内，加入块缓存
                current_block.append(line)
            else:
                # 如果是文件头部的介绍文字，直接保留
                if line.strip():
                    formatted_lines.append(line.rstrip())

    # 循环结束，处理最后一个块
    if current_block:
        formatted_lines.extend(process_block(current_block))

    # ==========================================
    # 最终写入：清理多余空行
    # ==========================================
    content_str = "\n".join(formatted_lines)
    # 正则替换：将连续3个以上的换行符替换为2个（保证段落间最多1个空行）
    content_str = re.sub(r'\n{3,}', '\n\n', content_str)
    # 去除文件开头可能的空行
    content_str = content_str.strip()

    try:
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(content_str)
        print(f"✅ 处理成功！新文件已生成：{output_file}")
    except Exception as e:
        print(f"❌ 写入错误：{e}")


# ==========================================
# ⚙️ 配置区域
# ==========================================
if __name__ == "__main__":
    # 输入文件（请确保文件名正确）
    INPUT_FILE = "leecode（简单+中等）.md"

    # 输出文件
    OUTPUT_FILE = "leetcode_formatted.md"

    beautify_markdown(INPUT_FILE, OUTPUT_FILE)
