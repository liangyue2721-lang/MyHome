#!/usr/bin/env bash
# -*- coding: utf‑8 -*-
# 脚本名称：check_hw_for_stream10.sh
# 说明：检查当前主机 CPU 架构和指令集支持情况，
#      决定是否建议升级到 CentOS Stream 10 或保留 Stream 9。

set -euo pipefail

echo "=== 检查 CPU 架构 ==="
arch=$(uname -m)
echo "CPU 架构: $arch"

if [ "$arch" != "x86_64" ]; then
  echo "WARNING: 当前 CPU 架构不是 x86_64，可能无法安装 CentOS Stream 10 (x86_64_v3 要求)。"
  echo "建议使用 CentOS Stream 9 或其它兼容系统。"
  exit 1
fi

echo "=== 检查 CPU Flags（指令集支持） ==="
flags=$(grep -m1 '^flags' /proc/cpuinfo | cut -d':' -f2-)
echo "Detected flags: $flags"

# 要求支持的关键指令集集合：avx, avx2, fma, bmi1, bmi2
need_flags=( avx avx2 fma bmi1 bmi2 )
missing_flags=()

for f in "${need_flags[@]}"; do
  if ! grep -qw "$f" <<< "$flags"; then
    missing_flags+=( "$f" )
  fi
done

if [ ${#missing_flags[@]} -eq 0 ]; then
  echo "✔ 检测到关键指令集: ${need_flags[*]}"
  echo "=> 你的 CPU 符合 CentOS Stream 10 要求（x86_64_v3 级别）。"
  echo "建议：可以安装 CentOS Stream 10。"
else
  echo "✘ 缺少以下指令集: ${missing_flags[*]}"
  echo "=> 你的 CPU **可能不满足** CentOS Stream 10 的最低指令集要求。"
  echo "建议：使用 CentOS Stream 9 以获得更广泛兼容性。"
fi

echo "=== 检查内存与磁盘空间建议 ==="
mem_kb=$(grep MemTotal /proc/meminfo | awk '{print $2}')
mem_gb=$(( mem_kb / 1024 / 1024 ))
disk_free_gb=$(df -BG / | tail -1 | awk '{print $4}' | tr -dc '0-9')
echo "可用内存约: ${mem_gb} GB"
echo "根分区可用空间约: ${disk_free_gb} GB"

if [ $mem_gb -lt 4 ] || [ $disk_free_gb -lt 20 ]; then
  echo "WARNING: 内存或磁盘空间偏低，建议至少 4 GB 内存 + 20 GB 可用磁盘用于安装及运行。"
fi

echo "=== 检查虚拟化平台指令透传 ==="
# 在虚拟机中也可能未透传 AVX2 等指令
if grep -qw "hypervisor" /proc/cpuinfo; then
  echo "检测到运行在虚拟化环境。请确认主机／虚拟化平台是否支持并透传 AVX2 等指令。"
fi

echo "=== 检查结束 ==="
