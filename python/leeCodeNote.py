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
# 设置【文件夹】路径
# 脚本会自动在此文件夹下生成 LeetCode_Simple.md, LeetCode_Medium.md 等文件
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
    """
    HTML 转 Markdown (V18.0 视觉优化版)
    保留灰色代码块背景，去除杂乱标签，安全转义 #
    """
    if not html: return ""

    html = html.replace('\n', ' ')
    html = re.sub(r'[ \t]+', ' ', html)

    # 1. 媒体处理
    html = re.sub(r'<img[^>]*src="([^"]*)"[^>]*>', r'\n\n![image](\1)\n\n', html)
    html = re.sub(r'<video[^>]*src="([^"]*)"[^>]*>.*?</video>',
                  r'\n\n<video controls src="\1" style="width: 100%; max-width: 800px;"></video>\n\n', html)

    # 2. 示例 <pre> 处理 (转为 ```text 灰色背景框)
    def handle_pre(match):
        content = match.group(1)
        content = re.sub(r'<br\s*/?>', '\n', content)
        content = re.sub(r'<[^>]+>', '', content)  # 暴力去除内部标签
        content = content.replace('&nbsp;', ' ').replace('&lt;', '<').replace('&gt;', '>').replace('&quot;',
                                                                                                   '"').replace('&amp;',
                                                                                                                '&')
        return f"\n\n```text\n{content.strip()}\n```\n\n"

    html = re.sub(r'<pre[^>]*>([\s\S]*?)</pre>', handle_pre, html)

    # 3. 正文格式
    html = re.sub(r'<code[^>]*>(.*?)</code>', r'`\1`', html)
    html = re.sub(r'<(?:strong|b)[^>]*>(.*?)</(?:strong|b)>', r'**\1**', html)

    # 列表与段落
    html = re.sub(r'<li[^>]*>', r'\n- ', html)
    html = re.sub(r'</li>', '', html)
    html = re.sub(r'</?ul[^>]*>', r'\n', html)
    html = re.sub(r'</?ol[^>]*>', r'\n', html)
    html = re.sub(r'<p[^>]*>', r'\n\n', html)
    html = re.sub(r'<br\s*/?>', r'\n', html)

    # 清理
    html = re.sub(r'<[^>]+>', '', html)
    html = html.replace('&nbsp;', ' ').replace('&lt;', '<').replace('&gt;', '>').replace('&quot;', '"').replace('&amp;',
                                                                                                                '&')

    # 4. 正文 # 转义
    html = html.replace('#', '&#35;')

    lines = [line.strip() for line in html.split('\n')]
    html = '\n'.join(lines)
    html = re.sub(r'\n{3,}', '\n\n', html)
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
    candidates = ["简单", "中等", "困难", "Easy", "Medium", "Hard"]
    best_diff, min_y = "未知", 99999
    for text in candidates:
        xpath = f"//*[text()='{text}']"
        elements = driver.find_elements(By.XPATH, xpath)
        for elem in elements:
            try:
                if not elem.is_displayed(): continue
                y = elem.location['y']
                if 0 < y < 600 and y < min_y:
                    min_y = y
                    best_diff = text
            except:
                continue
    return best_diff


def get_page_content(driver):
    print("⏳ 正在读取页面数据...")
    driver.execute_script("window.scrollTo(0, 0);")
    time.sleep(0.5)

    title_element = safe_find_element(driver, [(By.CSS_SELECTOR, 'div[data-cy="question-title"]'),
                                               (By.CSS_SELECTOR, '.text-title-large')])
    if not title_element: return None, None, None, None
    title = title_element.text.strip()
    print(f"👉 发现题目: {title}")

    difficulty = "未知"
    retry = 0
    while difficulty == "未知" and retry < 3:
        difficulty = get_difficulty_by_location(driver)
        if difficulty == "未知":
            retry += 1
            print(f"   -> 难度未找到，正在重试 ({retry}/3)...")
            time.sleep(1)
            driver.execute_script("window.scrollTo(0, 0);")
    print(f"📊 题目难度: {difficulty}")

    desc_element = safe_find_element(driver, [(By.CSS_SELECTOR, 'div[data-track-load="description_content"]'),
                                              (By.CLASS_NAME, 'content__u3I1')])
    description = beautify_html_to_md(desc_element.get_attribute('innerHTML')) if desc_element else "暂无描述"

    try:
        WebDriverWait(driver, 2).until(EC.presence_of_element_located((By.CSS_SELECTOR, '.view-lines .view-line')))
        code_lines = driver.find_elements(By.CSS_SELECTOR, '.view-lines .view-line')
        code_text = "\n".join([line.text.replace('\u00a0', ' ') for line in code_lines])
    except:
        code_text = "// 未检测到代码"

    return title, difficulty, description, code_text


def get_target_file_path(difficulty):
    """
    根据【难度】决定存储文件名
    """
    if not os.path.exists(SAVE_DIR):
        os.makedirs(SAVE_DIR)
        print(f"📁 已创建目录: {SAVE_DIR}")

    filename = "LeetCode_Unknown.md"

    if "简单" in difficulty or "Easy" in difficulty:
        filename = "LeetCode_Simple.md"
    elif "中等" in difficulty or "Medium" in difficulty:
        filename = "LeetCode_Medium.md"
    elif "困难" in difficulty or "Hard" in difficulty:
        filename = "LeetCode_Hard.md"

    return os.path.join(SAVE_DIR, filename)


def ensure_file_structure(file_path, difficulty):
    """
    确保文件存在，且包含对应难度的标题
    """
    # 确定该文件应该包含的标题
    target_header = "## 难度等级：未知"
    if "简单" in difficulty:
        target_header = "## 难度等级：简单"
    elif "中等" in difficulty:
        target_header = "## 难度等级：中等"
    elif "困难" in difficulty:
        target_header = "## 难度等级：困难"

    default_content = f"# LeetCode 题库 ({difficulty})\n\n{target_header}\n"

    if not os.path.exists(file_path):
        print(f"📄 创建新卷: {os.path.basename(file_path)}")
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(default_content)
    else:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        if target_header not in content:
            # 如果文件存在但没有这个标题（理论上不会发生，除非手动删了），追加进去
            with open(file_path, 'w', encoding='utf-8') as f: f.write(content + "\n\n" + target_header + "\n")


def update_markdown(title, difficulty, description, code):
    # 1. 根据难度获取路径
    target_path = get_target_file_path(difficulty)
    ensure_file_structure(target_path, difficulty)

    with open(target_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # 2. 确定该文件的核心标题
    target_header = "## 难度等级：未知"
    if "简单" in difficulty:
        target_header = "## 难度等级：简单"
    elif "中等" in difficulty:
        target_header = "## 难度等级：中等"
    elif "困难" in difficulty:
        target_header = "## 难度等级：困难"

    # 3. 确定写入范围 (从标题开始，到文件结束)
    # 因为是按难度分文件的，所以 LeetCode_Simple.md 里理论上只有一个 "## 难度等级：简单"
    # 我们只需要找到它，然后把内容加在后面即可
    start_scope = content.find(target_header)
    if start_scope == -1:
        # 双重保险
        content += f"\n\n{target_header}\n"
        start_scope = content.find(target_header)

    # 在按难度分文件的情况下，scope 就是从标题到文件末尾
    category_content = content[start_scope:]

    # 4. 构造内容
    diff_colored = get_difficulty_color(difficulty)
    styled_desc = f"> {diff_colored}\n\n{description}"
    new_prob_block = f"\n\n### {title}\n\n#### 📝 问题描述\n\n{styled_desc}\n\n#### 💻 问题解答 (Java)\n\n```Java\n{code}\n```\n"

    # 5. 写入
    prob_id = title.split('.', 1)[0].strip()
    match_prob = re.search(f"### {re.escape(prob_id)}\\.", category_content)

    file_name = os.path.basename(target_path)

    if match_prob:
        print(f"✅ 更新题目 [{file_name}]: {title}")
        prob_start = match_prob.start()
        next_prob = re.search(r'\n### \d+\.', category_content[match_prob.end():])
        # 如果是最后一题，next_prob 为 None
        prob_end = (match_prob.end() + next_prob.start()) if next_prob else len(category_content)

        updated_category = category_content[:prob_start] + new_prob_block.strip() + category_content[prob_end:]
    else:
        print(f"🆕 新增题目 [{file_name}]: {title}")
        updated_category = category_content + new_prob_block

    final_content = content[:start_scope] + updated_category
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
            time.sleep(3)
            result = get_page_content(driver)
            if result[0] is not None: update_markdown(*result)
            if not AUTO_NEXT: break
            trigger_next_shortcut(driver)
            time.sleep(2)
    except KeyboardInterrupt:
        print("\n👋 停止")


if __name__ == "__main__": main()
