import time
import re
import os
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.common.action_chains import ActionChains
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

# ================= 配置区域 =================
MD_FILE_PATH = r'C:\Users\84522\Desktop\leecode_py.md'
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
        print("请先在 CMD 运行 Chrome 调试启动命令。")
        exit()


def safe_find_element(driver, selectors):
    for by_type, selector in selectors:
        try:
            element = WebDriverWait(driver, 1).until(
                EC.presence_of_element_located((by_type, selector))
            )
            if element.is_displayed():
                return element
        except:
            continue
    return None


def beautify_html_to_md(html):
    """
    HTML 转 Markdown 清洗函数 (V6.0 深度净化版)
    核心修复：去除冗余的 ** 符号，修复空行
    """
    if not html: return ""

    # 1. 压缩空白字符 (保留换行)
    html = re.sub(r'[ \t]+', ' ', html)

    # 2. 图片转 MD
    html = re.sub(r'<img[^>]*src="([^"]*)"[^>]*>', r'\n\n![image](\1)\n\n', html)

    # 3. 示例区域处理 (去除 <pre>)
    html = re.sub(r'<pre[^>]*>([\s\S]*?)</pre>', r'\n\1\n', html)

    # 4. 行内代码处理 <code> -> `
    html = re.sub(r'<code[^>]*>(.*?)</code>', r'`\1`', html)

    # 5. 加粗处理 (strong/b -> **)
    html = re.sub(r'<(?:strong|b)[^>]*>(.*?)</(?:strong|b)>', r'**\1**', html)

    # --- 🚨 新增：深度清洗冗余的 ** 符号 ---
    # 5.1 去除空的加粗：**** 或 ** **
    html = re.sub(r'\*\*\s*\*\*', '', html)
    # 5.2 修复加粗内的空格：** text ** -> **text**
    html = re.sub(r'\*\*\s+(.*?)\s+\*\*', r' **\1** ', html)
    # 5.3 (可选) 如果不需要给示例标题加粗，可以在这里去掉
    # html = html.replace('**示例', '示例')

    # 6. 列表处理
    html = re.sub(r'<li[^>]*>', r'\n- ', html)
    html = re.sub(r'</li>', '', html)
    html = re.sub(r'</?ul[^>]*>', r'\n', html)
    html = re.sub(r'</?ol[^>]*>', r'\n', html)

    # 7. 段落和换行
    html = re.sub(r'<p[^>]*>', r'\n\n', html)
    html = re.sub(r'</p>', '', html)
    html = re.sub(r'<br\s*/?>', r'\n', html)
    html = re.sub(r'<div>', r'\n', html)
    html = re.sub(r'</div>', r'', html)
    html = re.sub(r'<[^>]+>', '', html)  # 清理剩余标签

    # 8. 实体字符还原
    html = html.replace('&nbsp;', ' ').replace('&lt;', '<').replace('&gt;', '>').replace('&quot;', '"').replace('&amp;',
                                                                                                                '&')

    # 9. 最终格式化
    lines = [line.strip() for line in html.split('\n')]
    html = '\n'.join(lines)
    html = re.sub(r'\n{3,}', '\n\n', html)  # 最多允许连续2个换行

    # 10. 最后的清理：去除孤立的 **
    html = html.replace('****', '')

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
        xpath = f"//div[text()='{text}'] | //span[text()='{text}']"
        elements = driver.find_elements(By.XPATH, xpath)
        for elem in elements:
            try:
                if not elem.is_displayed(): continue
                y = elem.location['y']
                if y < 400 and y < min_y: min_y, best_diff = y, text
            except:
                continue
    return best_diff


def get_page_content(driver):
    print("⏳ 正在读取页面数据...")
    title_element = safe_find_element(driver, [(By.CSS_SELECTOR, 'div[data-cy="question-title"]'),
                                               (By.CSS_SELECTOR, '.text-title-large')])

    if not title_element:
        print("❌ 未找到标题，跳过...")
        return None, None, None, None

    title = title_element.text.strip()
    print(f"👉 发现题目: {title}")

    difficulty = get_difficulty_by_location(driver)
    print(f"📊 题目难度: {difficulty}")

    desc_element = safe_find_element(driver, [(By.CSS_SELECTOR, 'div[data-track-load="description_content"]'),
                                              (By.CLASS_NAME, 'content__u3I1')])

    if desc_element:
        description = beautify_html_to_md(desc_element.get_attribute('innerHTML'))
    else:
        description = "暂无描述"

    try:
        code_lines = driver.find_elements(By.CSS_SELECTOR, '.view-lines .view-line')
        code_text = "\n".join([line.text.replace('\u00a0', ' ') for line in code_lines]) if code_lines else "// 未检测到代码"
    except:
        code_text = "// 代码获取出错"

    if not title or not description or len(description) < 5:
        print("❌ 数据为空，跳过")
        return None, None, None, None

    return title, difficulty, description, code_text


def ensure_file_structure():
    default_content = "# LeetCode 题库\n\n## 难度等级：简单\n\n## 难度等级：中等\n\n## 难度等级：困难\n"
    if not os.path.exists(MD_FILE_PATH):
        with open(MD_FILE_PATH, 'w', encoding='utf-8') as f:
            f.write(default_content)
    else:
        with open(MD_FILE_PATH, 'r', encoding='utf-8') as f:
            content = f.read()
        needs_write = False
        for level in ["## 难度等级：简单", "## 难度等级：中等", "## 难度等级：困难"]:
            if level not in content:
                content += f"\n\n{level}\n"
                needs_write = True
        if needs_write:
            with open(MD_FILE_PATH, 'w', encoding='utf-8') as f: f.write(content)


def update_markdown(title, difficulty, description, code):
    ensure_file_structure()
    with open(MD_FILE_PATH, 'r', encoding='utf-8') as f:
        content = f.read()

    # 1. 确定归属分类
    target_header = "## 难度等级：未知"
    if "简单" in difficulty:
        target_header = "## 难度等级：简单"
    elif "中等" in difficulty:
        target_header = "## 难度等级：中等"
    elif "困难" in difficulty:
        target_header = "## 难度等级：困难"

    if target_header not in content: content += f"\n\n{target_header}\n"

    # 2. 锁定操作范围 (Scope)
    start_scope = content.find(target_header)
    next_header_match = re.search(r'\n## ', content[start_scope + len(target_header):])
    end_scope = (start_scope + len(target_header) + next_header_match.start()) if next_header_match else len(content)
    category_content = content[start_scope:end_scope]

    # 3. 构造内容
    diff_colored = get_difficulty_color(difficulty)
    styled_desc = f"> {diff_colored}\n\n{description}"
    new_prob_block = f"\n\n### {title}\n\n#### 📝 问题描述\n\n{styled_desc}\n\n#### 💻 问题解答 (Java)\n\n```Java\n{code}\n```\n"

    # 4. 更新或新增
    prob_id = title.split('.', 1)[0].strip()
    match_prob = re.search(f"### {re.escape(prob_id)}\\.", category_content)

    if match_prob:
        print(f"✅ 更新题目: {title}")
        prob_start = match_prob.start()
        next_prob = re.search(r'\n### ', category_content[match_prob.end():])
        prob_end = (match_prob.end() + next_prob.start()) if next_prob else len(category_content)
        updated_category = category_content[:prob_start] + new_prob_block.strip() + category_content[prob_end:]
    else:
        print(f"🆕 新增题目: {title}")
        updated_category = category_content + new_prob_block

    final_content = content[:start_scope] + updated_category + content[end_scope:]
    with open(MD_FILE_PATH, 'w', encoding='utf-8') as f:
        f.write(final_content)
    return True


def trigger_next_shortcut(driver):
    print("⌨️ 下一题...")
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


if __name__ == "__main__":
    main()
