import os

def process_markdown_file(input_file, output_file):
    """
    读取 input_file，为所有 ### 题目补充缺失的四级标签，
    并将结果写入 output_file。
    """

    # 1. 检查输入文件是否存在
    if not os.path.exists(input_file):
        print(f"❌ 错误：找不到文件 '{input_file}'，请检查文件名。")
        return

    print(f"📂 正在读取：{input_file} ...")

    # 2. 读取文件内容
    with open(input_file, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    processed_lines = []
    current_block = []

    # 定义要补充的标签常量
    TAG_DESC = "#### 📝 问题描述"
    TAG_SOL = "#### 💻 问题解答 (Java)"

    def flush_block(block):
        """处理当前缓存的题目块，补充缺失标签"""
        if not block:
            return []

        # 将列表拼接成字符串以进行检查
        block_text = "".join(block)

        # 如果块中没有“问题描述”，则添加
        if TAG_DESC not in block_text:
            # 确保前面有个空行（如果最后一行不是空行）
            if block and block[-1].strip() != "":
                block.append("\n")
            block.append(f"{TAG_DESC}\n\n")

        # 如果块中没有“问题解答”，则添加
        if TAG_SOL not in block_text:
            # 再次检查是否需要空行分隔
            if block and block[-1].strip() != "":
                block.append("\n")
            block.append(f"{TAG_SOL}\n\n")

        return block

    # 3. 逐行遍历
    for line in lines:
        stripped = line.strip()

        # 判断是否是三级标题 (### 开头，但不是 ####)
        is_h3 = stripped.startswith("### ") and not stripped.startswith("####")

        if is_h3:
            # 遇到新题目：先处理并保存上一个题目块
            if current_block:
                processed_lines.extend(flush_block(current_block))
                current_block = [] # 清空缓存

            # 开始记录新块
            current_block.append(line)
        else:
            # 不是新题目，继续添加到当前块
            current_block.append(line)

    # 4. 循环结束后，处理并保存最后一个块
    if current_block:
        processed_lines.extend(flush_block(current_block))

    # 5. 写入新文件
    try:
        with open(output_file, 'w', encoding='utf-8') as f:
            f.writelines(processed_lines)
        print(f"✅ 处理成功！新文件已生成：{output_file}")
    except Exception as e:
        print(f"❌ 写入文件时发生错误：{e}")

# ==========================================
# ⚙️ 配置区域：在这里修改你的文件名
# ==========================================

if __name__ == "__main__":
    # 输入文件名（需要放在和脚本同一目录下，或者写绝对路径）
    INPUT_FILENAME = "leecode（简单+中等）.md"

    # 输出文件名
    OUTPUT_FILENAME = "leetcode_new.md"

    # 执行处理
    process_markdown_file(INPUT_FILENAME, OUTPUT_FILENAME)