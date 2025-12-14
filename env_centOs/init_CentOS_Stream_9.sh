#!/usr/bin/env bash
# -*- coding: utf-8 -*-
#
# CentOS Stream 9 系统初始化脚本
# 作者: ChatGPT
# 日期: 2025-11-07
#
# 功能：
# 1. 自动修复无IP问题，启用NetworkManager
# 2. 设置固定IP与主机名
# 3. 自动开放端口 22, 8085, 6969, 80, 6000-7000
# 4. 设置笔记本合盖子不休眠/不关机
# 5. 显示最终网络状态
#
# 可根据需要修改以下变量：
#   HOST_NAME="myserver"
#   STATIC_IP="192.168.1.100"
#   GATEWAY="192.168.1.1"
#   DNS1="8.8.8.8"
#   DNS2="1.1.1.1"

set -e

##############################################
#            🧩 参数配置区
##############################################
HOST_NAME="myserver"          # 主机名，可自定义
STATIC_IP="192.168.1.100"     # 固定IP
GATEWAY="192.168.1.1"         # 网关
DNS1="8.8.8.8"                # DNS1
DNS2="1.1.1.1"                # DNS2
##############################################

echo "==============================="
echo " 🧩 开始系统初始化任务 "
echo "==============================="

#-----------------------------------
# 1. NetworkManager 检查
#-----------------------------------
if ! systemctl is-active --quiet NetworkManager; then
  echo "🔧 NetworkManager 未运行，正在启动..."
  systemctl enable --now NetworkManager
else
  echo "✅ NetworkManager 已在运行"
fi

#-----------------------------------
# 2. 检查网卡设备
#-----------------------------------
echo "🔍 检查网络接口..."
ip link show | grep -E "^[0-9]+: " || echo "⚠️ 未发现网络接口！请检查网络适配器。"

# 自动识别主网卡（忽略 lo/docker）
NET_IF=$(nmcli device status | awk '/ethernet/ && $3=="connected" {print $1; exit}')
if [ -z "$NET_IF" ]; then
  NET_IF=$(nmcli device status | awk '/ethernet/ {print $1; exit}')
fi

if [ -z "$NET_IF" ]; then
  echo "❌ 未找到可用网卡，请检查虚拟机或物理机配置。"
  exit 1
else
  echo "✅ 检测到网卡: $NET_IF"
fi

#-----------------------------------
# 3. 设置主机名
#-----------------------------------
echo "🧩 设置主机名为: $HOST_NAME"
hostnamectl set-hostname "$HOST_NAME"
echo "✅ 当前主机名: $(hostname)"

#-----------------------------------
# 4. 设置静态IP（修正版）
#-----------------------------------
echo "⚙️ 配置网卡 $NET_IF 为静态IP: $STATIC_IP"

# 检查是否存在连接配置
CON_NAME=$(nmcli -t -f NAME,DEVICE connection show | grep "$NET_IF" | cut -d: -f1 | head -n1)

if [ -z "$CON_NAME" ]; then
  echo "🔧 未找到连接配置，创建新连接 $NET_IF ..."
  nmcli connection add type ethernet con-name "$NET_IF" ifname "$NET_IF" autoconnect yes
  CON_NAME="$NET_IF"
fi

# 先清除旧配置，避免冲突
nmcli connection modify "$CON_NAME" ipv4.addresses ""
nmcli connection modify "$CON_NAME" ipv4.gateway ""
nmcli connection modify "$CON_NAME" ipv4.dns ""
nmcli connection modify "$CON_NAME" ipv4.method auto

# 依次设置静态IP参数
nmcli connection modify "$CON_NAME" ipv4.addresses "${STATIC_IP}/24"
nmcli connection modify "$CON_NAME" ipv4.gateway "$GATEWAY"
nmcli connection modify "$CON_NAME" ipv4.dns "$DNS1,$DNS2"
nmcli connection modify "$CON_NAME" ipv4.method manual
nmcli connection modify "$CON_NAME" connection.autoconnect yes

echo "🔁 应用新的网络配置..."
nmcli connection down "$CON_NAME" || true
sleep 1
nmcli connection up "$CON_NAME"

sleep 3
IP_ADDR=$(ip addr show "$NET_IF" | awk '/inet /{print $2}' | head -n1)
if [ -n "$IP_ADDR" ]; then
  echo "✅ 已分配静态IP: $IP_ADDR"
else
  echo "⚠️ 未成功获取IP，请手动检查配置。"
fi
