#!/usr/bin/env bash
# --------------------------------------------------------------
# install_mysqldump.sh
# 适配：CentOS Stream 8/9、RHEL8/9、Rocky/AlmaLinux
# 功能：自动安装 MySQL 8 客户端（含 mysqldump）
# 特性：自动重试、进度条、版本信息、自检、PATH 强制软链
# 作者：ChatGPT（2025）
# --------------------------------------------------------------

set -Eeuo pipefail

# 全局变量
MAX_RETRY=3
MYSQL_REPO_RPM="mysql80-community-release-el9-1.noarch.rpm"
MYSQL_REPO_URL="https://dev.mysql.com/get/${MYSQL_REPO_RPM}"

log() { printf "\033[1;32m[INFO]\033[0m %s\n" "$1"; }
warn() { printf "\033[1;33m[WARN]\033[0m %s\n" "$1"; }
err() { printf "\033[1;31m[ERROR]\033[0m %s\n" "$1"; }

# ------------------------------
# 进度条
# ------------------------------
progress_bar() {
    local current=$1 total=$2
    local width=40
    local filled=$(( current * width / total ))
    local empty=$(( width - filled ))
    printf "\r["
    printf "%0.s#" $(seq 1 $filled)
    printf "%0.s-" $(seq 1 $empty)
    printf "] %d%%" $(( current * 100 / total ))
}

# ------------------------------
# 重试执行命令
# ------------------------------
retry() {
    local n=1
    while true; do
        if "$@"; then
            return 0
        else
            if (( n >= MAX_RETRY )); then
                err "命令执行失败超过 ${MAX_RETRY} 次：$*"
                return 1
            fi
            warn "命令执行失败，正在重试（${n}/${MAX_RETRY}）：$*"
            n=$(( n + 1 ))
            sleep 2
        fi
    done
}

# ------------------------------
# 下载 MySQL 官方仓库 RPM
# ------------------------------
install_repo() {
    log "下载 MySQL 官方仓库 RPM..."
    retry curl -fSL "$MYSQL_REPO_URL" -o "$MYSQL_REPO_RPM"
    retry sudo dnf -y install "$MYSQL_REPO_RPM"
}

# ------------------------------
# 安装 MySQL 客户端（含 mysqldump）
# ------------------------------
install_mysql_client() {
    log "安装 MySQL 8 客户端（mysql-client / mysqldump）..."

    retry sudo dnf --enablerepo=mysql80-community --disablerepo=appstream install -y mysql-community-client
}

# ------------------------------
# 软链 mysqldump（确保可执行）
# ------------------------------
ensure_symlink() {
    if [ -x /usr/bin/mysqldump ]; then
        log "mysqldump 已存在：/usr/bin/mysqldump"
        return
    fi

    local dump_path
    dump_path=$(command -v mysqldump 2>/dev/null || true)
    if [ "$dump_path" != "" ]; then
        log "mysqldump 在 PATH 中可用：$dump_path"
        return
    fi

    # 尝试 MySQL 标准安装路径
    for path in /usr/bin/mysql /usr/bin/mysqladmin /usr/bin/mysqldump /usr/libexec/mysql/mysqld; do
        if [ -x "$path" ]; then
            sudo ln -s "$path" /usr/bin/mysqldump
            log "创建软链：/usr/bin/mysqldump -> $path"
            return
        fi
    done

    err "mysqldump 未成功安装或未找到可执行文件"
    exit 1
}

# ------------------------------
# 显示最终版本
# ------------------------------
show_version() {
    log "验证 mysqldump 版本..."
    if ! command -v mysqldump >/dev/null 2>&1; then
        err "mysqldump 未安装成功！"
        exit 1
    fi

    mysqldump --version
}

# ------------------------------
# 主流程
# ------------------------------
main() {
    clear
    log "开始安装 mysqldump（MySQL 8 客户端）..."

    step=0
    total=4

    ((step++)); progress_bar $step $total
    install_repo

    ((step++)); progress_bar $step $total
    install_mysql_client

    ((step++)); progress_bar $step $total
    ensure_symlink

    ((step++)); progress_bar $step $total
    show_version

    printf "\n"
    log "mysqldump 安装完成！"
}

main
