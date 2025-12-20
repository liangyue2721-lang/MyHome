#!/usr/bin/env bash
# ==========================================================
# Install JDK 17 (Linux / macOS)
# - Auto download (resume + retry)
# - Safe extract
# - Idempotent
# - No latest redirect
# ==========================================================

set -euo pipefail

echo "==============================="
echo "Install JDK 17"
echo "==============================="

# ----------------------------------------------------------
# 1. OS / ARCH detection
# ----------------------------------------------------------
OS_NAME="$(uname -s)"
ARCH="$(uname -m)"

case "$OS_NAME" in
    Linux)
        OS="linux"
        BASE_DIR="/opt/java"
        ;;
    Darwin)
        OS="mac"
        BASE_DIR="/usr/local/java"
        ;;
    *)
        echo "Unsupported OS: $OS_NAME"
        exit 1
        ;;
esac

case "$ARCH" in
    x86_64)
        CPU="x64"
        ;;
    aarch64|arm64)
        CPU="aarch64"
        ;;
    *)
        echo "Unsupported architecture: $ARCH"
        exit 1
        ;;
esac

# ----------------------------------------------------------
# 2. Paths
# ----------------------------------------------------------
JDK_HOME="$BASE_DIR/jdk-17"
TMP_DIR="/tmp/jdk17_install"
ARCHIVE="$TMP_DIR/jdk17.tar.gz"

mkdir -p "$BASE_DIR"

# ----------------------------------------------------------
# 3. JDK source (Temurin 17 LTS - fixed version)
# ----------------------------------------------------------
JDK_VERSION="17.0.10_7"
JDK_URL="https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.10%2B7/OpenJDK17U-jdk_${CPU}_${OS}_hotspot_${JDK_VERSION}.tar.gz"

# ----------------------------------------------------------
# 4. Install if not exists
# ----------------------------------------------------------
if [ -x "$JDK_HOME/bin/java" ]; then
    echo "JDK 17 already installed: $JDK_HOME"
else
    echo "Downloading JDK 17..."
    rm -rf "$TMP_DIR"
    mkdir -p "$TMP_DIR"

    if command -v curl >/dev/null 2>&1; then
        curl -fL -C - \
             --retry 5 \
             --retry-delay 10 \
             --connect-timeout 30 \
             --max-time 0 \
             -o "$ARCHIVE" \
             "$JDK_URL"
    elif command -v wget >/dev/null 2>&1; then
        wget -c --tries=5 --timeout=30 "$JDK_URL" -O "$ARCHIVE"
    else
        echo "ERROR: curl or wget is required"
        exit 1
    fi

    # ------------------------------------------------------
    # 5. Validate archive
    # ------------------------------------------------------
    if ! gzip -t "$ARCHIVE" >/dev/null 2>&1; then
        echo "ERROR: Downloaded file is not a valid tar.gz"
        exit 1
    fi

    echo "Extracting JDK..."
    tar -xzf "$ARCHIVE" -C "$TMP_DIR"

    JDK_EXTRACTED="$(find "$TMP_DIR" -maxdepth 1 -type d -name 'jdk-17*' | head -n 1)"

    if [ -z "$JDK_EXTRACTED" ]; then
        echo "ERROR: Failed to locate extracted JDK directory"
        exit 1
    fi

    rm -rf "$JDK_HOME"
    mv "$JDK_EXTRACTED" "$JDK_HOME"
    rm -rf "$TMP_DIR"

    echo "JDK 17 installed at $JDK_HOME"
fi

# ----------------------------------------------------------
# 6. Environment variables
# ----------------------------------------------------------
if [ -n "${ZSH_VERSION:-}" ]; then
    PROFILE="$HOME/.zshrc"
else
    PROFILE="$HOME/.bashrc"
fi

if ! grep -q "JAVA_HOME=.*jdk-17" "$PROFILE" 2>/dev/null; then
    {
        echo ""
        echo "# JDK 17"
        echo "export JAVA_HOME=$JDK_HOME"
        echo "export PATH=\$JAVA_HOME/bin:\$PATH"
    } >> "$PROFILE"
fi

# ----------------------------------------------------------
# 7. Result
# ----------------------------------------------------------
echo "==============================="
echo "JDK 17 setup completed"
echo "Run: source $PROFILE"
echo "==============================="

"$JDK_HOME/bin/java" -version
