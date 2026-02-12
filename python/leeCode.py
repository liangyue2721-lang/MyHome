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
# 请确认你的路径是否正确
MD_FILE_PATH = r'C:\Users\84522\Desktop\leecode_py.md'

# 开启自动循环
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
        exit()


def safe_find_element(driver, selectors, name="元素"):
    """尝试多个选择器来寻找同一个元素"""
    for by_type, selector in selectors:
        try:
            element = WebDriverWait(driver, 2).until(
                EC.presence_of_element_located((by_type, selector))
            )
            return element
        except:
            continue
    return None


def get_page_content(driver):
    print("⏳ 正在读取页面数据...")

    # 1. 获取标题
    title_selectors = [
        (By.CSS_SELECTOR, 'div[data-cy="question-title"]'),
        (By.CSS_SELECTOR, '.text-title-large'),
        (By.XPATH, '//div[contains(@class, "text-title-large")]'),
        (By.ID, 'question-title'),
    ]
    title_element = safe_find_element(driver, title_selectors, "题目标题")

    if not title_element:
        print("❌ 未找到标题，跳过此页")
        return None, None, None

    title = title_element.text.strip()
    print(f"👉 发现题目: {title}")

    # 2. 获取描述
    desc_selectors = [
        (By.CSS_SELECTOR, 'div[data-track-load="description_content"]'),
        (By.CLASS_NAME, 'content__u3I1'),
        (By.CSS_SELECTOR, 'div.elfjS'),
    ]
    desc_element = safe_find_element(driver, desc_selectors, "题目描述")
    description = desc_element.text.strip() if desc_element else "未获取到描述"

    # 3. 获取代码
    try:
        code_lines = driver.find_elements(By.CSS_SELECTOR, '.view-lines .view-line')
        if not code_lines:
            code_text = "// 未检测到代码，请确认编辑器已加载"
        else:
            code_text = "\n".join([line.text.replace('\u00a0', ' ') for line in code_lines])
    except:
        code_text = "// 代码获取出错"

    return title, description, code_text


def update_markdown(title, description, code):
    if not os.path.exists(MD_FILE_PATH):
        print(f"❌ 文件不存在: {MD_FILE_PATH}")
        return False

    with open(MD_FILE_PATH, 'r', encoding='utf-8') as f:
        content = f.read()

    # 处理标题 ID (例如 "1" 或 "LCR 164")
    title_parts = title.split('.', 1)
    if len(title_parts) < 2:
        prob_id = title
    else:
        prob_id = title_parts[0].strip()

    # 正则匹配 ### ID.
    pattern_str = f"### {re.escape(prob_id)}\\..*"
    match_header = re.search(pattern_str, content)

    if not match_header:
        print(f"⚠️ 文件中未找到题目 '{prob_id}'，跳过写入。")
        return False

    print(f"✅ 定位到章节: {match_header.group()}")

    start_pos = match_header.end()
    next_header = re.search(r'\n### ', content[start_pos:])
    end_pos = (start_pos + next_header.start()) if next_header else len(content)
    section_content = content[start_pos:end_pos]

    # 替换描述
    if "#### 📝 问题描述" in section_content:
        # 使用非贪婪匹配填充描述
        section_content = re.sub(
            r'(#### 📝 问题描述\s*)([\s\S]*?)(?=\s*#### 💻)',
            f'\\1\n{description}\n\n',
            section_content
        )

    # 替换代码
    todo_marker = "// TODO: 待补充代码"
    if todo_marker in section_content:
        section_content = section_content.replace(todo_marker, code)
        print("✅ 代码已填入")
    else:
        print("ℹ️ 代码位置似乎已被修改，未执行覆盖")

    new_full_content = content[:start_pos] + section_content + content[end_pos:]
    with open(MD_FILE_PATH, 'w', encoding='utf-8') as f:
        f.write(new_full_content)

    return True


def trigger_next_shortcut(driver):
    """使用快捷键 Ctrl + -> 切换下一题"""
    print("⌨️ 发送快捷键: Ctrl + → ...")
    try:
        # 方法1: 使用 ActionChains 全局发送按键
        actions = ActionChains(driver)
        actions.key_down(Keys.CONTROL).send_keys(Keys.ARROW_RIGHT).key_up(Keys.CONTROL).perform()
        return True
    except Exception as e:
        print(f"⚠️ 快捷键发送失败: {e}")
        # 方法2: 尝试对 body 发送
        try:
            driver.find_element(By.TAG_NAME, 'body').send_keys(Keys.CONTROL, Keys.ARROW_RIGHT)
            return True
        except:
            return False


def main():
    driver = connect_chrome()

    try:
        while True:
            # 1. 等待页面加载
            time.sleep(3)

            # 2. 获取并更新
            title, desc, code = get_page_content(driver)
            if title:
                update_markdown(title, desc, code)

            if not AUTO_NEXT:
                break

            # 3. 触发下一题
            trigger_next_shortcut(driver)

            # 4. 翻页缓冲 (防止太快)
            time.sleep(2)

    except KeyboardInterrupt:
        print("\n👋 脚本停止")


if __name__ == "__main__":
    main()
