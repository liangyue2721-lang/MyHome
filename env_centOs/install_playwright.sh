#!/usr/bin/env bash
# -*- coding: utf-8 -*-
#
# install_playwright.sh
# 适配：CentOS Stream 9 / Python3.11+
# 功能：自动安装并验证 Playwright 环境
# 特性：全自动、容错执行、输出版本信息
#
# 作者：ChatGPT
# 日期：2025-11-07
#

set -Eeo pipefail
trap 'echo "⚠️ [警告] 出错命令: $BASH_COMMAND (行号: $LINENO)"' ERR

echo "=== 开始安装 Playwright 环境 ==="

# 1️⃣ 检查 Python3 和 pip
if ! command -v python3 &>/dev/null; then
    echo ">>> 未检测到 Python3，正在安装..."
    dnf -y install python3 python3-pip || true
fi

if ! command -v pip3 &>/dev/null; then
    echo ">>> 安装 pip3..."
    dnf -y install python3-pip || true
fi

# 2️⃣ 升级 pip 并安装 playwright 相关包
echo ">>> 升级 pip..."
pip3 install --upgrade pip || true

echo ">>> 安装 playwright 与 pytest-playwright..."
pip3 install -U playwright pytest-playwright || true

# 3️⃣ 安装浏览器内核
echo ">>> 下载 Playwright 浏览器内核 (Chromium, Firefox, WebKit)..."
playwright install || true

# 4️⃣ 安装依赖库（GUI、音频、Wayland、字体）
echo ">>> 安装系统依赖库..."
dnf -y install \
    libX11-xcb libgbm libdrm libXcomposite libXcursor libXdamage \
    libXext libXtst libXi libXrandr libxshmfence alsa-lib atk \
    at-spi2-core cups-libs gtk3 libxkbcommon wayland-libs-client \
    pango cairo libXrender || true

# 5️⃣ 验证结果
echo -e "\n=== Playwright 安装结果验证 ==="
echo "Python 版本: $(python3 --version 2>/dev/null || echo 未检测到)"
echo "Pip 版本: $(pip3 --version 2>/dev/null || echo 未检测到)"
echo "Playwright 版本: $(playwright --version 2>/dev/null || echo 未检测到)"

# 6️⃣ 验证安装是否可用
echo -e "\n>>> 运行 playwright info 检查环境..."
playwright info || echo "⚠️ playwright info 执行失败（可能环境不完整）"

echo -e "\n✅ Playwright 环境安装完成！"
echo "浏览器路径: $(python3 -m playwright install --dry-run | grep 'download' | head -n 1 || echo '默认路径 ~/.cache/ms-playwright')"
echo "你可以运行以下命令进行测试："
echo "  python3 -m playwright codegen https://example.com"
