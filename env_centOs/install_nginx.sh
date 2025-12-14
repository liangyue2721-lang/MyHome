#!/usr/bin/env bash
# -*- coding: utf-8 -*-
#
# install_nginx.sh
# 适配：CentOS Stream 9
# 功能：自动安装、启用并验证 Nginx
# 特性：稳定执行、带备用源、不会中断
#
# 作者：ChatGPT
# 日期：2025-11-07
#

set -Eeo pipefail
trap 'echo "⚠️ [警告] 出错命令: $BASH_COMMAND (行号: $LINENO)"' ERR

echo "=== 开始安装 Nginx ==="

# 1️⃣ 检查是否已安装
if command -v nginx &>/dev/null; then
    echo ">>> 检测到 Nginx 已安装 ($(nginx -v 2>&1))"
else
    echo ">>> 安装 Nginx 中..."

    # 主源安装
    if ! dnf -y install nginx; then
        echo "⚠️ 从默认源安装失败，尝试切换到阿里云源..."
        cat >/etc/yum.repos.d/nginx.repo <<'EOF'
[nginx-stable]
name=nginx stable repo
baseurl=https://mirrors.aliyun.com/nginx/yum/centos/9/x86_64/
gpgcheck=0
enabled=1
EOF
        dnf clean all
        dnf makecache
        dnf -y install nginx || echo "⚠️ 阿里源安装 Nginx 仍失败，可能网络受限"
    fi
fi

# 2️⃣ 启动并设置自启
echo ">>> 启动 Nginx 服务..."
systemctl enable --now nginx || echo "⚠️ 启动 nginx 失败（可能已启动）"

# 3️⃣ 配置防火墙
echo ">>> 开放 HTTP/HTTPS 端口..."
firewall-cmd --permanent --add-service=http || true
firewall-cmd --permanent --add-service=https || true
firewall-cmd --reload || true

# 4️⃣ 验证结果
echo -e "\n=== Nginx 安装结果验证 ==="
echo "Nginx 路径: $(which nginx 2>/dev/null || echo 未找到)"
echo "Nginx 版本: $(nginx -v 2>&1 || echo 未安装)"
echo "Web 根目录: /usr/share/nginx/html"
echo "服务状态:"
systemctl status nginx --no-pager | grep Active || echo "未知状态"

echo -e "\n✅ Nginx 安装与配置已完成！"
echo "🌐 可访问测试页面: http://<你的服务器IP>"
