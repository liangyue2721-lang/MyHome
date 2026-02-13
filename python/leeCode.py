import time
import re
import os
import sys
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.common.action_chains import ActionChains
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

# ================= 配置区域 =================
# 设置文件保存的文件夹路径 (脚本会自动创建此文件夹)
# 建议使用绝对路径，例如: r'C:\Users\84522\Desktop\LeetCode_Notes'
SAVE_DIR = r'C:\Users\84522\Desktop\LeetCode_Notes'

# 是否开启自动循环下一题
AUTO_NEXT = True


# ===========================================

def connect_chrome():
    print("🔌 正在连接 Chrome...")
    chrome_options = Options()
    chrome_options.add_experimental_option("debuggerAddress", "127.0.0.1:9222")
    try:
        driver = webdriver.Chrome(options=chrome_options)
        return driver
    except Exception as e:
        print(f"❌ 连接失败: {e}")
        print("请先在 CMD 运行: chrome.exe --remote-debugging-port=9222 --user-data-dir=\"C:\\sel_chrome\"")
        sys.exit()


def safe_find_element(driver, selectors, timeout=1):
    for by_type, selector in selectors:
        try:
            element = WebDriverWait(driver, timeout).until(
                EC.presence_of_element_located((by_type, selector))
            )
            if element.is_displayed():
                return element
        except:
            continue
    return None


def beautify_html_to_md(html):
    """ HTML 转 Markdown 深度清洗函数 """
    if not html: return ""

    # 1. 基础清洗
    html = re.sub(r'[ \t]+', ' ', html)  # 压缩空格

    # 2. 转换图片
    html = re.sub(r'<img[^>]*src="([^"]*)"[^>]*>', r'\n\n![image](\1)\n\n', html)

    # 3. 转换示例区域 (去除 <pre>)
    html = re.sub(r'<pre[^>]*>([\s\S]*?)</pre>', r'\n\1\n', html)

    # 4. 转换格式 (代码、加粗)
    html = re.sub(r'<code[^>]*>(.*?)</code>', r'`\1`', html)
    html = re.sub(r'<(?:strong|b)[^>]*>(.*?)</(?:strong|b)>', r'**\1**', html)

    # 5. 去除冗余的加粗符号 (**** 或 ** **)
    html = re.sub(r'\*\*\s*\*\*', '', html)
    html = re.sub(r'\*\*\s+(.*?)\s+\*\*', r' **\1** ', html)

    # 6. 转换列表
    html = re.sub(r'<li[^>]*>', r'\n- ', html)
    html = re.sub(r'</li>', '', html)
    html = re.sub(r'</?ul[^>]*>', r'\n', html)
    html = re.sub(r'</?ol[^>]*>', r'\n', html)

    # 7. 处理段落与换行
    html = re.sub(r'<p[^>]*>', r'\n\n', html)
    html = re.sub(r'</p>', '', html)
    html = re.sub(r'<br\s*/?>', r'\n', html)
    html = re.sub(r'<div>', r'\n', html)
    html = re.sub(r'</div>', r'', html)

    # 8. 清理剩余标签与还原实体
    html = re.sub(r'<[^>]+>', '', html)
    html = html.replace('&nbsp;', ' ').replace('&lt;', '<').replace('&gt;', '>').replace('&quot;', '"').replace('&amp;',
                                                                                                                '&')

    # 9. 格式整理
    lines = [line.strip() for line in html.split('\n')]
    html = '\n'.join(lines)
    html = re.sub(r'\n{3,}', '\n\n', html)  # 限制最大连续换行

    return html.strip()


def get_difficulty_color(difficulty):
    color = "gray"
    if "简单" in difficulty or "Easy" in difficulty:
        color = "green"
    elif "中等" in difficulty or "Medium" in difficulty:
        color = "orange"
    elif "困难" in difficulty or "Hard" in difficulty:
        color = "red"
    return f'<span style="color: {color}; font-weight: bold;">{difficulty}</span>'


def get_difficulty_by_location(driver):
    """
    通过坐标筛选页面顶部的难度标签
    【修复】：放宽了 Y 轴限制，并增加了通用匹配
    """
    candidates = ["简单", "中等", "困难", "Easy", "Medium", "Hard"]
    best_diff, min_y = "未知", 99999

    for text in candidates:
        # 使用 * 通配符匹配所有标签，不仅仅是 div 或 span
        xpath = f"//*[text()='{text}']"
        elements = driver.find_elements(By.XPATH, xpath)
        for elem in elements:
            try:
                if not elem.is_displayed(): continue
                y = elem.location['y']

                # 【修复】将高度限制从 400 放宽到 600，防止小窗口时标题被挤压
                # 同时排除 Y=0 的隐藏元素
                if 0 < y < 600 and y < min_y:
                    min_y = y
                    best_diff = text
            except:
                continue
    return best_diff


def get_page_content(driver):
    print("⏳ 正在读取页面数据...")

    # 【关键优化】强制滚动到顶部，确保难度标签可见
    driver.execute_script("window.scrollTo(0, 0);")
    time.sleep(0.5)

    # 1. 获取标题
    title_element = safe_find_element(driver, [
        (By.CSS_SELECTOR, 'div[data-cy="question-title"]'),
        (By.CSS_SELECTOR, '.text-title-large')
    ])

    if not title_element:
        print("❌ 未找到标题，跳过...")
        return None, None, None, None

    title = title_element.text.strip()
    print(f"👉 发现题目: {title}")

    # 2. 获取难度 (增加重试机制)
    difficulty = "未知"
    retry_count = 0
    while difficulty == "未知" and retry_count < 3:
        difficulty = get_difficulty_by_location(driver)
        if difficulty == "未知":
            retry_count += 1
            print(f"   -> 难度未找到，正在重试 ({retry_count}/3)...")
            time.sleep(1)  # 等待 1 秒让元素加载
            driver.execute_script("window.scrollTo(0, 0);")  # 再次滚顶

    print(f"📊 题目难度: {difficulty}")

    # 3. 获取描述
    desc_element = safe_find_element(driver, [
        (By.CSS_SELECTOR, 'div[data-track-load="description_content"]'),
        (By.CLASS_NAME, 'content__u3I1')
    ])
    description = beautify_html_to_md(desc_element.get_attribute('innerHTML')) if desc_element else "暂无描述"

    # 4. 获取代码 (增加显式等待)
    try:
        # 等待代码行出现，最多等 2 秒
        WebDriverWait(driver, 2).until(
            EC.presence_of_element_located((By.CSS_SELECTOR, '.view-lines .view-line'))
        )
        code_lines = driver.find_elements(By.CSS_SELECTOR, '.view-lines .view-line')
        code_text = "\n".join([line.text.replace('\u00a0', ' ') for line in code_lines])
    except:
        code_text = "// 未检测到代码，请确认编辑器已加载"

    return title, difficulty, description, code_text


def get_target_file_path(title):
    """ 根据 ID 计算存储文件路径 """
    # 确保目录存在
    if not os.path.exists(SAVE_DIR):
        os.makedirs(SAVE_DIR)
        print(f"📁 已创建目录: {SAVE_DIR}")

    match = re.match(r'^(\d+)\.', title)
    filename = "leetcode_others.md"

    if match:
        prob_id = int(match.group(1))
        # 计算区间: 1-500, 501-1000...
        start = ((prob_id - 1) // 500) * 500 + 1
        end = start + 499
        filename = f"leetcode_{start}-{end}.md"

    return os.path.join(SAVE_DIR, filename)


def ensure_file_structure(file_path):
    """ 确保 MD 文件存在且有难度标题 """
    default_content = "# LeetCode 题库\n\n## 难度等级：简单\n\n## 难度等级：中等\n\n## 难度等级：困难\n"

    if not os.path.exists(file_path):
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(default_content)
    else:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        needs_write = False
        for level in ["## 难度等级：简单", "## 难度等级：中等", "## 难度等级：困难"]:
            if level not in content:
                content += f"\n\n{level}\n"
                needs_write = True
        if needs_write:
            with open(file_path, 'w', encoding='utf-8') as f: f.write(content)


def update_markdown(title, difficulty, description, code):
    # 1. 准备文件
    target_path = get_target_file_path(title)
    ensure_file_structure(target_path)

    with open(target_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # 2. 确定归属分类 Scope
    target_header = "## 难度等级：未知"
    if "简单" in difficulty:
        target_header = "## 难度等级：简单"
    elif "中等" in difficulty:
        target_header = "## 难度等级：中等"
    elif "困难" in difficulty:
        target_header = "## 难度等级：困难"

    if target_header not in content: content += f"\n\n{target_header}\n"

    # 锁定该难度的区间
    start_scope = content.find(target_header)
    next_header_match = re.search(r'\n## ', content[start_scope + len(target_header):])
    end_scope = (start_scope + len(target_header) + next_header_match.start()) if next_header_match else len(content)

    category_content = content[start_scope:end_scope]

    # 3. 构造内容块
    diff_colored = get_difficulty_color(difficulty)
    styled_desc = f"> {diff_colored}\n\n{description}"
    new_prob_block = f"\n\n### {title}\n\n#### 📝 问题描述\n\n{styled_desc}\n\n#### 💻 问题解答 (Java)\n\n```Java\n{code}\n```\n"

    # 4. 更新或新增
    prob_id = title.split('.', 1)[0].strip()
    match_prob = re.search(f"### {re.escape(prob_id)}\\.", category_content)

    file_name = os.path.basename(target_path)

    if match_prob:
        print(f"✅ 更新题目 [{file_name}]: {title}")
        prob_start = match_prob.start()
        next_prob = re.search(r'\n### ', category_content[match_prob.end():])
        prob_end = (match_prob.end() + next_prob.start()) if next_prob else len(category_content)
        updated_category = category_content[:prob_start] + new_prob_block.strip() + category_content[prob_end:]
    else:
        print(f"🆕 新增题目 [{file_name}]: {title}")
        updated_category = category_content + new_prob_block

    final_content = content[:start_scope] + updated_category + content[end_scope:]
    with open(target_path, 'w', encoding='utf-8') as f:
        f.write(final_content)
    return True


def trigger_next_shortcut(driver):
    print("⌨️ 切换下一题...")
    try:
        ActionChains(driver).key_down(Keys.CONTROL).send_keys(Keys.ARROW_RIGHT).key_up(Keys.CONTROL).perform()
    except:
        driver.find_element(By.TAG_NAME, 'body').send_keys(Keys.CONTROL, Keys.ARROW_RIGHT)


def main():
    driver = connect_chrome()
    try:
        while True:
            time.sleep(3)  # 等待页面加载

            result = get_page_content(driver)
            if result[0] is not None:
                update_markdown(*result)

            if not AUTO_NEXT: break

            trigger_next_shortcut(driver)
            time.sleep(2)  # 翻页缓冲

    except KeyboardInterrupt:
        print("\n👋 脚本停止")


if __name__ == "__main__":
    main()
