#!/usr/bin/env bash
# --------------------------------------------------------------
# install_mysqldump_centos9.sh
# 适配：CentOS Stream 9 / RHEL9 / Rocky9 / AlmaLinux9
# 功能：安装 mysqldump（使用 MariaDB 客户端，100% 兼容 MySQL 8）
# --------------------------------------------------------------

set -Eeuo pipefail

log()  { echo -e "\033[1;32m[INFO]\033[0m $1"; }
err()  { echo -e "\033[1;31m[ERROR]\033[0m $1"; }

MAX_RETRY=3

retry() {
    local n=1
    while true; do
        if "$@"; then
            return 0
        fi
        if (( n >= MAX_RETRY )); then
            err "命令执行失败：$*"
            exit 1
        fi
        echo "[WARN] 重试第 ${n}/${MAX_RETRY} 次: $*"
        n=$((n + 1))
        sleep 1
    done
}

log "开始安装 mysqldump（MariaDB 兼容版）..."

log "安装 mariadb 客户端..."
retry dnf install -y mariadb

log "检查 mariadb-dump..."
if ! command -v mariadb-dump >/dev/null 2>&1; then
    err "mariadb-dump 安装失败"
    exit 1
fi

log "创建 mysqldump 软链..."
ln -sf "$(command -v mariadb-dump)" /usr/bin/mysqldump

log "验证 mysqldump..."
mysqldump --version

log "mysqldump 安装完成！"
