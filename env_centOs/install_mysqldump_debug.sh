#!/usr/bin/env bash

set -Eeuo pipefail

log()  { echo -e "\033[1;32m[INFO]\033[0m $1"; }
warn() { echo -e "\033[1;33m[WARN]\033[0m $1"; }
err()  { echo -e "\033[1;31m[ERROR]\033[0m $1"; }

MYSQL_REPO_RPM="mysql80-community-release-el9-1.noarch.rpm"
MYSQL_REPO_URL="https://dev.mysql.com/get/${MYSQL_REPO_RPM}"

log "开始安装 mysqldump（MySQL 8 客户端）..."

echo "[DEBUG] 当前目录: $(pwd)"
echo "[DEBUG] 当前用户: $(whoami)"
echo "[DEBUG] 检查 curl 命令..."
command -v curl || err "curl 未安装"

echo "[DEBUG] 测试 MySQL 官网连通性..."
curl -I --max-time 5 https://dev.mysql.com/ || err "无法访问 dev.mysql.com"

log "下载 MySQL 官方仓库 RPM..."
curl -fSL "$MYSQL_REPO_URL" -o "$MYSQL_REPO_RPM"

log "安装 MySQL 官方仓库 RPM..."
dnf -y install "$MYSQL_REPO_RPM"

log "安装 MySQL 客户端 mysql-community-client..."
dnf --enablerepo=mysql80-community --disablerepo=appstream install -y mysql-community-client

log "检查 mysqldump 是否存在..."
command -v mysqldump || err "mysqldump 未成功安装"

log "mysqldump 版本："
mysqldump --version

log "安装完成！"
