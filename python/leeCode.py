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
            return element
        except:
            continue
    return None


def beautify_html_to_md(html):
    """
    HTML 转 Markdown 清洗函数 (优化版)
    特点：保留示例换行，不使用代码块包裹示例
    """
    if not html: return ""

    # 1. 移除不必要的空白字符，但保留换行（关键！防止示例变成一行）
    # 仅将连续的空格/Tab压缩为一个空格，但不处理 \n
    html = re.sub(r'[ \t]+', ' ', html)

    # 2. 处理图片
    html = re.sub(r'<img[^>]*src="([^"]*)"[^>]*>', r'\n\n![image](\1)\n\n', html)

    # 3. 处理示例区域 <pre>
    # 【修改点】：不再用 ``` 包裹，直接保留内容，前后加换行
    html = re.sub(r'<pre[^>]*>([\s\S]*?)</pre>', r'\n\1\n', html)

    # 4. 处理行内代码 <code> -> `
    html = re.sub(r'<code[^>]*>(.*?)</code>', r'`\1`', html)

    # 5. 处理加粗 <strong>/<b> -> **
    html = re.sub(r'<(?:strong|b)[^>]*>(.*?)</(?:strong|b)>', r'**\1**', html)

    # 6. 处理列表
    html = re.sub(r'<li[^>]*>', r'\n- ', html)
    html = re.sub(r'</li>', '', html)
    html = re.sub(r'</?ul[^>]*>', r'\n', html)
    html = re.sub(r'</?ol[^>]*>', r'\n', html)

    # 7. 处理段落和换行
    html = re.sub(r'<p[^>]*>', r'\n\n', html)
    html = re.sub(r'</p>', '', html)
    html = re.sub(r'<br\s*/?>', r'\n', html)  # 将 <br> 转为显式换行
    html = re.sub(r'<div>', r'\n', html)
    html = re.sub(r'</div>', r'', html)

    # 8. 清理剩余标签
    html = re.sub(r'<[^>]+>', '', html)

    # 9. 实体还原
    html = html.replace('&nbsp;', ' ').replace('&lt;', '<').replace('&gt;', '>').replace('&quot;', '"').replace('&amp;',
                                                                                                                '&')

    # 10. 格式整理：确保每行开头不要有奇怪的缩进，且控制换行数量
    lines = [line.strip() for line in html.split('\n')]
    html = '\n'.join(lines)
    html = re.sub(r'\n{3,}', '\n\n', html)  # 最多允许两个连续换行

    return html.strip()


def get_difficulty_color(difficulty):
    """ 生成 HTML 颜色标签 """
    if "简单" in difficulty or "Easy" in difficulty:
        return f'<span style="color: green; font-weight: bold;">{difficulty}</span>'
    if "中等" in difficulty or "Medium" in difficulty:
        return f'<span style="color: orange; font-weight: bold;">{difficulty}</span>'
    if "困难" in difficulty or "Hard" in difficulty:
        return f'<span style="color: red; font-weight: bold;">{difficulty}</span>'
    return f'<span style="color: gray;">{difficulty}</span>'


def get_difficulty_by_location(driver):
    """
    【坐标筛选法】修复难度误判
    通过 Y 轴坐标筛选，取页面最顶部的难度标签
    """
    candidates = ["简单", "中等", "困难", "Easy", "Medium", "Hard"]
    best_diff = "未知"
    min_y = 99999

    for text in candidates:
        xpath = f"//div[text()='{text}'] | //span[text()='{text}']"
        elements = driver.find_elements(By.XPATH, xpath)
        for elem in elements:
            try:
                if not elem.is_displayed(): continue
                y = elem.location['y']
                # 筛选条件：必须在页面顶部区域 (y < 400)
                if y < 400 and y < min_y:
                    min_y = y
                    best_diff = text
            except:
                continue
    return best_diff


def get_page_content(driver):
    print("⏳ 正在读取页面数据...")

    # 1. 获取标题
    title_selectors = [
        (By.CSS_SELECTOR, 'div[data-cy="question-title"]'),
        (By.CSS_SELECTOR, '.text-title-large'),
        (By.XPATH, '//div[contains(@class, "text-title-large")]'),
        (By.ID, 'question-title'),
    ]
    title_element = safe_find_element(driver, title_selectors)
    if not title_element:
        print("❌ 未找到标题，跳过")
        return None, None, None, None
    title = title_element.text.strip()
    print(f"👉 发现题目: {title}")

    # 2. 获取难度 (坐标法)
    difficulty = get_difficulty_by_location(driver)
    print(f"📊 题目难度: {difficulty}")

    # 3. 获取描述
    desc_selectors = [
        (By.CSS_SELECTOR, 'div[data-track-load="description_content"]'),
        (By.CLASS_NAME, 'content__u3I1'),
    ]
    desc_element = safe_find_element(driver, desc_selectors)
    if desc_element:
        raw_html = desc_element.get_attribute('innerHTML')
        description = beautify_html_to_md(raw_html)
    else:
        description = "未获取到描述"

    # 4. 获取代码
    try:
        code_lines = driver.find_elements(By.CSS_SELECTOR, '.view-lines .view-line')
        code_text = "\n".join([line.text.replace('\u00a0', ' ') for line in code_lines]) if code_lines else "// 未检测到代码"
    except:
        code_text = "// 代码获取出错"

    return title, difficulty, description, code_text


def get_category_header(difficulty):
    if "简单" in difficulty: return "## 难度等级：简单"
    if "中等" in difficulty: return "## 难度等级：中等"
    if "困难" in difficulty: return "## 难度等级：困难"
    return "## 难度等级：未知"


def update_markdown(title, difficulty, description, code):
    if not os.path.exists(MD_FILE_PATH):
        with open(MD_FILE_PATH, 'w', encoding='utf-8') as f:
            f.write("# LeetCode 题库\n")

    with open(MD_FILE_PATH, 'r', encoding='utf-8') as f:
        content = f.read()

    # 确保分类标题存在
    target_header = get_category_header(difficulty)
    if target_header not in content:
        order = ["## 难度等级：简单", "## 难度等级：中等", "## 难度等级：困难", "## 难度等级：未知"]
        try:
            idx = order.index(target_header)
        except:
            idx = 3
        insert_pos = len(content)
        for i in range(idx - 1, -1, -1):
            if order[i] in content:
                match = re.search(re.escape(order[i]), content)
                start_search = match.end()
                next_sect = re.search(r'\n## 难度等级：', content[start_search:])
                insert_pos = start_search + next_sect.start() if next_sect else len(content)
                break
        content = content[:insert_pos] + f"\n\n{target_header}\n" + content[insert_pos:]

    # 生成描述块 (带颜色难度，但示例无代码块)
    diff_colored = get_difficulty_color(difficulty)
    styled_description = f"> {diff_colored}\n\n{description}"

    # 匹配与更新
    title_parts = title.split('.', 1)
    prob_id = title_parts[0].strip() if len(title_parts) > 1 else title
    match_header = re.search(f"### {re.escape(prob_id)}\\..*", content)

    if match_header:
        print(f"✅ 更新: {title}")
        start = match_header.end()
        next_h = re.search(r'\n### ', content[start:])
        end = (start + next_h.start()) if next_h else len(content)
        section = content[start:end]

        if "#### 📝 问题描述" in section:
            section = re.sub(r'(#### 📝 问题描述\s*)([\s\S]*?)(?=\s*#### 💻)', f'\\1\n{styled_description}\n\n', section)
        if "// TODO: 待补充代码" in section:
            section = section.replace("// TODO: 待补充代码", code)

        new_content = content[:start] + section + content[end:]
    else:
        print(f"🆕 新增: {title} -> {difficulty}")
        new_section = f"\n### {title}\n\n" \
                      f"#### 📝 问题描述\n\n" \
                      f"{styled_description}\n\n" \
                      f"#### 💻 问题解答 (Java)\n\n" \
                      f"```Java\n{code}\n```\n"

        h_pos = content.find(target_header)
        start_search = h_pos + len(target_header)
        next_sect = re.search(r'\n## 难度等级：', content[start_search:])
        insert_pos = start_search + next_sect.start() if next_sect else len(content)
        new_content = content[:insert_pos] + new_section + content[insert_pos:]

    with open(MD_FILE_PATH, 'w', encoding='utf-8') as f:
        f.write(new_content)
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
            if result[0]:
                update_markdown(*result)
            if not AUTO_NEXT: break
            trigger_next_shortcut(driver)
            time.sleep(2)
    except KeyboardInterrupt:
        print("\n👋 停止")


if __name__ == "__main__":
    main()
