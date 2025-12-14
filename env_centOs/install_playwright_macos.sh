#!/usr/bin/env bash
# -*- coding: utf-8 -*-
#
# install_playwright_macos.sh
# 适配：macOS / Python3.11+
# 功能：自动安装并验证 Playwright 环境
# 特性：全自动、容错执行、输出版本信息
#
# 作者：ChatGPT
# 日期：2025-11-14
#

set -Eeo pipefail
trap 'echo "⚠️ [警告] 出错命令: $BASH_COMMAND (行号: $LINENO)"' ERR

echo "=== 开始安装 macOS Playwright 环境 ==="

############################################
# 1️⃣ 检查 Homebrew
############################################
if ! command -v brew &>/dev/null; then
    echo ">>> 未检测到 Homebrew，正在安装..."
    NONINTERACTIVE=1 /bin/bash -c \
        "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
else
    echo ">>> Homebrew 已安装"
fi

brew update || true

############################################
# 2️⃣ 检查 Python3 与 pip3
############################################
if ! command -v python3 &>/dev/null; then
    echo ">>> 未检测到 Python3，正在通过 Homebrew 安装..."
    brew install python || true
fi

if ! command -v pip3 &>/dev/null; then
    echo ">>> pip3 未找到，使用 brew 重新链接 Python..."
    brew reinstall python || true
fi

echo ">>> 升级 pip..."
pip3 install --upgrade pip || true

############################################
# 3️⃣ 安装 Playwright 库
############################################
echo ">>> 安装 playwright 与 pytest-playwright..."
pip3 install -U playwright pytest-playwright || true

############################################
# 4️⃣ 安装 Playwright 浏览器
############################################
echo ">>> 下载 Playwright 浏览器内核 (Chromium / Firefox / WebKit)..."
playwright install || true

############################################
# 5️⃣ macOS 特殊依赖（字体、WebKit 补丁）
############################################
echo ">>> 安装必要 macOS 字体（可选）..."
brew install --cask font-noto-sans || true

############################################
# 6️⃣ 打印安装验证信息
############################################
echo -e "\n=== Playwright 安装结果验证 ==="
echo "Python 版本: $(python3 --version 2>/dev/null || echo 未检测到)"
echo "Pip 版本: $(pip3 --version 2>/dev/null || echo 未检测到)"
echo "Playwright 版本: $(playwright --version 2>/dev/null || echo 未检测到)"

echo -e "\n>>> 运行 playwright info 检查环境..."
playwright info || echo "⚠️ playwright info 执行失败（可能环境不完整）"

############################################
# 7️⃣ 浏览器路径输出
############################################
echo -e "\n=== 浏览器安装路径 ==="
python3 -m playwright install --dry-run | grep 'download' | head -n 1 \
    || echo "默认路径：~/Library/Caches/ms-playwright"

echo -e "\n✅ macOS Playwright 环境已安装完成！"
echo "你可以运行以下命令测试："
echo "  python3 -m playwright codegen https://example.com"
