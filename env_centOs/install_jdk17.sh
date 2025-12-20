#!/usr/bin/env bash
set -e

echo "==============================="
echo "Install JDK 17"
echo "==============================="

OS="$(uname -s)"
ARCH="$(uname -m)"

# ===== 平台判断 =====
if [[ "$OS" == "Linux" ]]; then
    BASE_DIR="/opt/java"
elif [[ "$OS" == "Darwin" ]]; then
    BASE_DIR="/usr/local/java"
else
    echo "Unsupported OS: $OS"
    exit 1
fi

mkdir -p "$BASE_DIR"
JDK_HOME="$BASE_DIR/jdk-17"
TMP_DIR="/tmp/jdk17_install"
ARCHIVE="$TMP_DIR/jdk17.tar.gz"

# ===== 架构判断 =====
if [[ "$ARCH" == "x86_64" ]]; then
    CPU="x64"
elif [[ "$ARCH" == "aarch64" || "$ARCH" == "arm64" ]]; then
    CPU="aarch64"
else
    echo "Unsupported architecture: $ARCH"
    exit 1
fi

# ===== 下载地址（Temurin 17 LTS）=====
JDK_URL="https://github.com/adoptium/temurin17-binaries/releases/latest/download/OpenJDK17U-jdk_${CPU}_${OS,,}_hotspot.tar.gz"

# ===== 已存在直接配置 =====
if [[ -x "$JDK_HOME/bin/java" ]]; then
    echo "JDK 17 already installed at $JDK_HOME"
    goto_config=true
else
    goto_config=false
fi

if [[ "$goto_config" == false ]]; then
    echo "Downloading JDK 17..."
    rm -rf "$TMP_DIR"
    mkdir -p "$TMP_DIR"

    if command -v curl >/dev/null 2>&1; then
        curl -L "$JDK_URL" -o "$ARCHIVE"
    elif command -v wget >/dev/null 2>&1; then
        wget "$JDK_URL" -O "$ARCHIVE"
    else
        echo "Neither curl nor wget found"
        exit 1
    fi

    echo "Extracting JDK..."
    tar -xzf "$ARCHIVE" -C "$TMP_DIR"

    JDK_EXTRACTED=$(find "$TMP_DIR" -maxdepth 1 -type d -name "jdk-17*" | head -n 1)

    if [[ ! -d "$JDK_EXTRACTED" ]]; then
        echo "Failed to locate extracted JDK"
        exit 1
    fi

    rm -rf "$JDK_HOME"
    mv "$JDK_EXTRACTED" "$JDK_HOME"
    rm -rf "$TMP_DIR"

    echo "JDK 17 installed at $JDK_HOME"
fi

# ===== 配置环境变量 =====
PROFILE_FILE=""

if [[ "$SHELL" == *zsh ]]; then
    PROFILE_FILE="$HOME/.zshrc"
else
    PROFILE_FILE="$HOME/.bashrc"
fi

if ! grep -q "JAVA_HOME=.*jdk-17" "$PROFILE_FILE" 2>/dev/null; then
    echo "" >> "$PROFILE_FILE"
    echo "# JDK 17" >> "$PROFILE_FILE"
    echo "export JAVA_HOME=$JDK_HOME" >> "$PROFILE_FILE"
    echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> "$PROFILE_FILE"
fi

echo "==============================="
echo "JDK 17 setup completed"
echo "Please run: source $PROFILE_FILE"
echo "==============================="

"$JDK_HOME/bin/java" -version
