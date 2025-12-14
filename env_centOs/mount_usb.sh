#!/usr/bin/env bash
# 一键识别并挂载U盘
set -e

USB_DEV=$(lsblk -o NAME,RM,SIZE,TYPE,MOUNTPOINT | awk '$2==1 && $4=="part" && $5=="" {print "/dev/"$1; exit}')
MNT_DIR="/mnt/usb"

if [ -z "$USB_DEV" ]; then
  echo "❌ 未检测到未挂载的U盘分区。"
  exit 1
fi

echo "✅ 检测到U盘设备: $USB_DEV"
mkdir -p $MNT_DIR
mount $USB_DEV $MNT_DIR
echo "U盘已挂载到: $MNT_DIR"
ls -l $MNT_DIR
