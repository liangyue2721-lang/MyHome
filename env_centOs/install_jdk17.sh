#!/usr/bin/env bash
# ==========================================================
# Install / Upgrade JDK 17 (Linux / macOS)
# - Stable source (Adoptium API)
# - Versioned install
# - Symlink based upgrade
# - Idempotent
# ==========================================================

set -euo pipefail

echo "==============================="
echo "Install / Upgrade JDK 17"
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
        PROFILE_FILE="/etc/profile.d/java.sh"
        ;;
    Darwin)
        OS="mac"
        BASE_DIR="/usr/local/java"
        PROFILE_FILE="$HOME/.zshrc"
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
JDK_ROOT="$BASE_DIR/jdk-17"
VERSION_DIR="$BASE_DIR/jdk-17-versions"
TMP_DIR="$(mktemp -d /tmp/jdk17_XXXX)"
ARCHIVE="$TMP_DIR/jdk17.tar.gz"

mkdir -p "$VERSION_DIR"

# ----------------------------------------------------------
# 3. Download URL (Adoptium official API)
# ----------------------------------------------------------
JDK_URL="https://api.adoptium.net/v3/binary/latest/17/ga/${OS}/${CPU}/jdk/hotspot/normal/eclipse"

echo "Download URL:"
echo "  $JDK_URL"

# ----------------------------------------------------------
# 4. Download
# ----------------------------------------------------------
echo "Downloading JDK 17..."

if command -v curl >/dev/null 2>&1; then
    curl -fL -C - \
         --retry 5 \
         --retry-delay 5 \
         --connect-timeout 30 \
         -o "$ARCHIVE" \
         "$JDK_URL"
elif command -v wget >/dev/null 2>&1; then
    wget -c --tries=5 --timeout=30 "$JDK_URL" -O "$ARCHIVE"
else
    echo "ERROR: curl or wget is required"
    exit 1
fi

if ! gzip -t "$ARCHIVE" >/dev/null 2>&1; then
    echo "ERROR: Invalid tar.gz archive"
    exit 1
fi

# ----------------------------------------------------------
# 5. Extract
# ----------------------------------------------------------
echo "Extracting..."
tar -xzf "$ARCHIVE" -C "$TMP_DIR"

JDK_EXTRACTED="$(find "$TMP_DIR" -maxdepth 1 -type d -name 'jdk-17*' | head -n 1)"

if [ -z "$JDK_EXTRACTED" ]; then
    echo "ERROR: Failed to locate JDK directory"
    exit 1
fi

VERSION_NAME="$(basename "$JDK_EXTRACTED")"
TARGET_DIR="$VERSION_DIR/$VERSION_NAME"

if [ -d "$TARGET_DIR" ]; then
    echo "Version already exists: $VERSION_NAME"
else
    mv "$JDK_EXTRACTED" "$TARGET_DIR"
    echo "Installed version: $VERSION_NAME"
fi

# ----------------------------------------------------------
# 6. Switch symlink (atomic upgrade)
# ----------------------------------------------------------
ln -sfn "$TARGET_DIR" "$JDK_ROOT"

rm -rf "$TMP_DIR"

# ----------------------------------------------------------
# 7. Environment variables (write once)
# ----------------------------------------------------------
if [ ! -f "$PROFILE_FILE" ] || ! grep -q "JAVA_HOME=.*jdk-17" "$PROFILE_FILE"; then
    echo "Configuring JAVA_HOME..."

    cat > "$PROFILE_FILE" <<EOF
# JDK 17
export JAVA_HOME=$JDK_ROOT
export PATH=\$JAVA_HOME/bin:\$PATH
EOF
fi

# ----------------------------------------------------------
# 8. Result
# ----------------------------------------------------------
echo "==============================="
echo "JDK 17 installed / upgraded"
echo "JAVA_HOME = $JDK_ROOT"
echo "==============================="

"$JDK_ROOT/bin/java" -version
