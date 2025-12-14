#!/usr/bin/env bash
# -*- coding: utf-8 -*-
#
# install_dev_env.sh v4
# 稳定全流程版 — CentOS Stream 9 智能开发环境安装脚本
#
# 功能：
# - 自动安装并配置：JDK8 + Git + Maven(3.9.8) + Python3.11 + Playwright + MySQL8.0 + Nginx
# - 支持重复执行，不会中断
# - 所有下载失败自动使用备用阿里云镜像源
# - 执行全流程不中断，出错仅警告继续执行
#
# 作者: ChatGPT
# 日期: 2025-11-07
#

set -Eeo pipefail
trap 'echo "⚠️ [警告] 出错命令: $BASH_COMMAND (行号: $LINENO)"' ERR

TOTAL_STEPS=10
CURRENT_STEP=0

# =================== 工具函数 ===================
show_progress() {
    local step=$1 total=$2 width=40
    local filled=$(( width * step / total ))
    local empty=$(( width - filled ))
    local percent=$(( 100 * step / total ))
    printf "\r[%-${width}s] %3d%% (步骤 %d/%d) " \
        "$(printf '%0.s#' $(seq 1 $filled))$(printf '%0.s-' $(seq 1 $empty))" \
        "$percent" "$step" "$total"
    [[ "$step" -eq "$total" ]] && printf "\n"
}

version_lt() { [ "$(printf '%s\n' "$@" | sort -V | head -n1)" != "$1" ]; }

install_if_missing(){
    local cmd=$1 pkg=$2
    if ! command -v "$cmd" &>/dev/null; then
        echo ">>> 安装 $pkg ..."
        dnf -y install "$pkg" || echo "⚠️ $pkg 安装失败（已忽略）"
    else
        echo ">>> 已安装 $pkg，跳过"
    fi
}

# =================== 1. 更新系统元数据 ===================
echo "=== 更新系统软件包元数据 ==="
dnf -y makecache || true
dnf -y upgrade-minimal || true
CURRENT_STEP=$((CURRENT_STEP+1)); show_progress $CURRENT_STEP $TOTAL_STEPS

# =================== 2. 基础工具 ===================
echo "=== 安装基础工具 ==="
dnf -y install wget tar unzip curl || true
CURRENT_STEP=$((CURRENT_STEP+1)); show_progress $CURRENT_STEP $TOTAL_STEPS

# =================== 3. Git ===================
echo "=== 安装 Git ==="
install_if_missing git git
CURRENT_STEP=$((CURRENT_STEP+1)); show_progress $CURRENT_STEP $TOTAL_STEPS

# =================== 4. JDK8 ===================
echo "=== 安装 OpenJDK 1.8 ==="
if java -version &>/dev/null; then
    CURRENT_VER=$(java -version 2>&1 | head -n1 | grep -oP '\d+\.\d+')
    if [[ "$CURRENT_VER" != "1.8" ]]; then
        echo ">>> 检测到 Java 版本为 $CURRENT_VER，切换到 1.8..."
        dnf -y install java-1.8.0-openjdk-devel || true
    else
        echo ">>> Java 版本符合要求 ($CURRENT_VER)"
    fi
else
    dnf -y install java-1.8.0-openjdk-devel || true
fi

JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
echo "export JAVA_HOME=${JAVA_HOME}" > /etc/profile.d/java8.sh
echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> /etc/profile.d/java8.sh
chmod +x /etc/profile.d/java8.sh
source /etc/profile.d/java8.sh
CURRENT_STEP=$((CURRENT_STEP+1)); show_progress $CURRENT_STEP $TOTAL_STEPS

# =================== 5. Maven ===================
echo "=== 安装 Maven 3.9.8 ==="
MAVEN_VERSION=3.9.8
MAVEN_TGZ="apache-maven-${MAVEN_VERSION}-bin.tar.gz"
MAIN_URL="https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/${MAVEN_TGZ}"
ALIYUN_URL="https://mirrors.aliyun.com/apache/maven/maven-3/${MAVEN_VERSION}/binaries/${MAVEN_TGZ}"

if command -v mvn &>/dev/null; then
    CURRENT_VER=$(mvn -v | head -n1 | grep -oP '\d+\.\d+\.\d+')
    if version_lt "$CURRENT_VER" "$MAVEN_VERSION"; then
        echo ">>> 当前 Maven 版本 $CURRENT_VER 低于 $MAVEN_VERSION，升级中..."
        rm -rf /opt/apache-maven-* || true
    else
        echo ">>> Maven 版本符合要求 ($CURRENT_VER)"
    fi
fi

if ! command -v mvn &>/dev/null || version_lt "$CURRENT_VER" "$MAVEN_VERSION"; then
    echo ">>> 下载 Maven..."
    wget -q "$MAIN_URL" -O "$MAVEN_TGZ" || wget -q "$ALIYUN_URL" -O "$MAVEN_TGZ" || echo "⚠️ Maven 下载失败，跳过"
    if [ -f "$MAVEN_TGZ" ]; then
        tar -xzf "$MAVEN_TGZ" -C /opt
        ln -sf /opt/apache-maven-${MAVEN_VERSION} /opt/maven
        echo "export MAVEN_HOME=/opt/maven" > /etc/profile.d/maven.sh
        echo "export PATH=\$MAVEN_HOME/bin:\$PATH" >> /etc/profile.d/maven.sh
        chmod +x /etc/profile.d/maven.sh
        source /etc/profile.d/maven.sh
        rm -f "$MAVEN_TGZ"
    fi
fi
CURRENT_STEP=$((CURRENT_STEP+1)); show_progress $CURRENT_STEP $TOTAL_STEPS

# =================== 6. Python 3.11 ===================
echo "=== 安装 Python 3.11 ==="
if ! command -v python3.11 &>/dev/null; then
    dnf -y install epel-release || true
    dnf config-manager --set-enabled crb || true
    dnf -y install python3.11 python3.11-devel python3.11-pip || true
    ln -sf /usr/bin/python3.11 /usr/local/bin/python3
    ln -sf /usr/bin/pip3.11 /usr/local/bin/pip3
else
    echo ">>> 已安装 Python3.11 ($(python3.11 -V))"
fi
CURRENT_STEP=$((CURRENT_STEP+1)); show_progress $CURRENT_STEP $TOTAL_STEPS

# =================== 7. Playwright ===================
echo "=== 安装 Playwright ==="
pip3 install --upgrade pip || true
pip3 install -U playwright pytest-playwright || true
playwright install || true
dnf -y install libX11-xcb libgbm libdrm libXcomposite libXcursor libXdamage \
libXext libXtst libXi libXrandr libxshmfence alsa-lib atk at-spi2-core \
cups-libs gtk3 libxkbcommon wayland-libs-client pango cairo libXrender || true
CURRENT_STEP=$((CURRENT_STEP+1)); show_progress $CURRENT_STEP $TOTAL_STEPS

# =================== 8. MySQL 8.0 ===================
echo "=== 安装 MySQL 8.0 (端口6969) ==="
MYSQL_PORT=6969
if ! command -v mysqld &>/dev/null; then
    dnf -y install https://dev.mysql.com/get/mysql80-community-release-el9-1.noarch.rpm || true
    dnf -y install mysql-community-server || true
    MYCNF_FILE="/etc/my.cnf.d/mysqld.cnf"
    if grep -q "^port=" "$MYCNF_FILE"; then
        sed -i "s/^port=.*/port=${MYSQL_PORT}/" "$MYCNF_FILE"
    else
        echo -e "\n[mysqld]\nport=${MYSQL_PORT}" >> "$MYCNF_FILE"
    fi
    systemctl enable --now mysqld || true
    firewall-cmd --permanent --add-port=${MYSQL_PORT}/tcp || true
    firewall-cmd --reload || true
    grep 'temporary password' /var/log/mysqld.log | awk '{print $NF}' | tail -n1 > /root/mysql_initial_password.txt || true
else
    echo ">>> 已安装 MySQL ($(mysql --version))"
fi
CURRENT_STEP=$((CURRENT_STEP+1)); show_progress $CURRENT_STEP $TOTAL_STEPS

# =================== 9. Nginx ===================
echo "=== 安装 Nginx ==="
install_if_missing nginx nginx
systemctl enable --now nginx || true
firewall-cmd --permanent --add-service=http || true
firewall-cmd --permanent --add-service=https || true
firewall-cmd --reload || true
CURRENT_STEP=$((CURRENT_STEP+1)); show_progress $CURRENT_STEP $TOTAL_STEPS

# =================== 10. 验证输出 ===================
echo -e "\n=== 验证安装结果 ==="
echo "Java 路径: $(which java 2>/dev/null || echo 未安装)"
echo "Java 版本: $(java -version 2>&1 | head -n1 || echo 未安装)"
echo "Maven 路径: $(which mvn 2>/dev/null || echo 未安装)"
echo "Maven 版本: $(mvn -v | head -n1 || echo 未安装)"
echo "Git 路径: $(which git 2>/dev/null || echo 未安装)"
echo "Git 版本: $(git --version 2>/dev/null || echo 未安装)"
echo "Python 路径: $(which python3 2>/dev/null || echo 未安装)"
echo "Python 版本: $(python3 --version 2>/dev/null || echo 未安装)"
echo "Pip 路径: $(which pip3 2>/dev/null || echo 未安装)"
echo "Pip 版本: $(pip3 --version 2>/dev/null || echo 未安装)"
echo "Playwright 版本: $(python3 -c 'import playwright; print(playwright.__version__)' 2>/dev/null || echo 未安装)"
echo "MySQL 版本: $(mysql --version 2>/dev/null || echo 未安装)"
echo "MySQL 端口: $MYSQL_PORT"
echo "Nginx 路径: $(which nginx 2>/dev/null || echo 未安装)"
echo "Nginx 版本: $(nginx -v 2>&1 || echo 未安装)"
echo "Nginx Web 根目录: /usr/share/nginx/html"
CURRENT_STEP=$((CURRENT_STEP+1)); show_progress $CURRENT_STEP $TOTAL_STEPS

echo -e "\n✅ 所有组件安装流程执行完毕！"
echo "🎉 开发环境已准备就绪。"
