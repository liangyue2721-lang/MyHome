#!/usr/bin/env bash
# -*- coding: utf-8 -*-
#
# install_jdk8.sh
# 适配：CentOS Stream 9
# 功能：安装并切换默认 Java 版本为 OpenJDK 1.8
# 作者：ChatGPT
# 日期：2025-11-07
#

set -Eeuo pipefail
trap 'echo "❌ 出错命令: $BASH_COMMAND (行号: $LINENO)"' ERR

echo "=== 安装 OpenJDK 1.8 ==="

# 1️⃣ 检查当前 Java 版本
if command -v java &>/dev/null; then
    CURRENT_VER=$(java -version 2>&1 | head -n1 | grep -oP '\d+\.\d+')
    echo ">>> 当前 Java 版本: $CURRENT_VER"
else
    echo ">>> 当前未检测到 Java 环境"
    CURRENT_VER="none"
fi

# 2️⃣ 安装 JDK8
echo ">>> 安装 java-1.8.0-openjdk-devel..."
dnf -y install java-1.8.0-openjdk-devel || {
    echo "⚠️ 主源下载失败，尝试阿里备用源..."
    dnf -y install https://mirrors.aliyun.com/centos-stream/9-stream/AppStream/x86_64/os/Packages/java-1.8.0-openjdk-devel-1.8.0.462.b08-4.el9.x86_64.rpm || true
}

# 3️⃣ 查找 JDK8 安装路径
echo ">>> 检测 JDK8 安装路径..."
JDK8_PATH=$(dirname "$(dirname "$(readlink -f "$(command -v java-1.8.0-openjdk 2>/dev/null || echo '')")")" 2>/dev/null || true)

# 若未检测到，则从 /usr/lib/jvm 搜索
if [[ -z "$JDK8_PATH" || "$JDK8_PATH" == "." || ! -d "$JDK8_PATH" ]]; then
    JDK8_PATH=$(ls -d /usr/lib/jvm/java-1.8.0-openjdk* 2>/dev/null | head -n1)
fi

# 再次确认路径有效性
if [[ -z "$JDK8_PATH" || ! -d "$JDK8_PATH" ]]; then
    echo "❌ 未找到 JDK8 安装路径，请检查安装。"
    exit 1
fi

echo ">>> 检测到 JDK8 安装路径: $JDK8_PATH"

# 4️⃣ 切换默认 Java 版本到 JDK8
echo ">>> 使用 alternatives 切换默认 Java 为 JDK8..."
alternatives --install /usr/bin/java java "$JDK8_PATH/bin/java" 1080 --family java-1.8.0-openjdk || true
alternatives --install /usr/bin/javac javac "$JDK8_PATH/bin/javac" 1080 --family java-1.8.0-openjdk || true
alternatives --set java "$JDK8_PATH/bin/java" || true
alternatives --set javac "$JDK8_PATH/bin/javac" || true

# 5️⃣ 设置环境变量
echo ">>> 写入 JAVA_HOME 环境变量..."
cat >/etc/profile.d/java8.sh <<EOF
export JAVA_HOME=${JDK8_PATH}
export PATH=\$JAVA_HOME/bin:\$PATH
EOF
chmod +x /etc/profile.d/java8.sh
source /etc/profile.d/java8.sh

# 6️⃣ 验证结果
echo -e "\n=== 验证 JDK8 安装结果 ==="
echo "Java 路径: $(which java)"
java -version || echo "⚠️ Java 执行失败"
echo "JAVA_HOME: $JAVA_HOME"

echo -e "\n✅ JDK8 安装并配置完成！"
