#!/usr/bin/env bash
# -*- coding: utf-8 -*-
#
# 一键安装 FRP 0.65.0 服务端/客户端（适用于 Linux AMD64）
# 作者: ChatGPT
# 日期: 2025-11-07
#
# 功能：
# 1. 自动检测并安装 wget、tar
# 2. 自动解压 frp_0.65.0_linux_amd64.tar.gz
# 3. 可选择安装为 frps（服务端）或 frpc（客户端）
# 4. 自动注册 systemd 服务
# 5. 启动并设置开机自启

set -e

FRP_VERSION="0.65.0"
FRP_DIR="/usr/local/frp"
FRP_TAR="frp_${FRP_VERSION}_linux_amd64.tar.gz"
FRP_EXTRACT_DIR="frp_${FRP_VERSION}_linux_amd64"

echo "=== 🚀 一键安装 FRP ${FRP_VERSION} 开始 ==="

# 检查是否有 tar 包
if [ ! -f "$FRP_TAR" ]; then
  echo "❌ 未找到 ${FRP_TAR}，请先将文件上传到当前目录！"
  exit 1
fi

# 安装必要依赖
echo "=== 📦 安装依赖 ==="
if command -v dnf >/dev/null 2>&1; then
  sudo dnf install -y tar wget
elif command -v yum >/dev/null 2>&1; then
  sudo yum install -y tar wget
elif command -v apt >/dev/null 2>&1; then
  sudo apt update -y && sudo apt install -y tar wget
else
  echo "❌ 未检测到支持的包管理器（dnf/yum/apt）"
  exit 1
fi

# 解压 FRP
echo "=== 📂 解压 FRP 文件 ==="
sudo rm -rf "$FRP_DIR"
sudo tar -zxf "$FRP_TAR"
sudo mv "$FRP_EXTRACT_DIR" "$FRP_DIR"

# 选择安装模式
echo "请选择安装模式:"
echo "1) frps（服务端）"
echo "2) frpc（客户端）"
read -p "输入选项 (1/2): " MODE

if [ "$MODE" == "1" ]; then
  BIN="frps"
  CONF="frps.ini"
  SERVICE="frps"
elif [ "$MODE" == "2" ]; then
  BIN="frpc"
  CONF="frpc.ini"
  SERVICE="frpc"
else
  echo "❌ 无效选择"
  exit 1
fi

# 创建配置文件（如果不存在）
if [ ! -f "$FRP_DIR/$CONF" ]; then
  echo "=== ✏️ 生成默认配置文件: $CONF ==="
  if [ "$MODE" == "1" ]; then
    cat <<EOF | sudo tee "$FRP_DIR/$CONF" >/dev/null
[common]
bind_port = 7000
dashboard_port = 7500
dashboard_user = admin
dashboard_pwd = admin123
token = 123456
EOF
  else
    cat <<EOF | sudo tee "$FRP_DIR/$CONF" >/dev/null
[common]
server_addr = x.x.x.x
server_port = 7000
token = 123456

[web]
type = tcp
local_ip = 127.0.0.1
local_port = 80
remote_port = 8080
EOF
  fi
fi

# 注册 systemd 服务
echo "=== ⚙️ 创建 systemd 服务文件 ==="
SERVICE_FILE="/etc/systemd/system/${SERVICE}.service"
sudo bash -c "cat > ${SERVICE_FILE}" <<EOF
[Unit]
Description=FRP ${SERVICE}
After=network.target

[Service]
ExecStart=${FRP_DIR}/${BIN} -c ${FRP_DIR}/${CONF}
Restart=always
User=root
LimitNOFILE=65535

[Install]
WantedBy=multi-user.target
EOF

# 启动服务并设置开机自启
echo "=== 🔄 启动 ${SERVICE} 服务 ==="
sudo systemctl daemon-reload
sudo systemctl enable ${SERVICE}
sudo systemctl restart ${SERVICE}

sleep 1
sudo systemctl status ${SERVICE} --no-pager -l

echo "=== ✅ FRP ${FRP_VERSION} 安装完成！ ==="
echo "安装目录：$FRP_DIR"
echo "配置文件：$FRP_DIR/$CONF"
echo "服务名：$SERVICE"
echo ""
echo "👉 查看日志：journalctl -u ${SERVICE} -f"
echo "👉 停止服务：systemctl stop ${SERVICE}"
echo "👉 修改配置后重启：systemctl restart ${SERVICE}"
