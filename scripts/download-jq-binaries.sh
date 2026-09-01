#!/usr/bin/env bash
set -euo pipefail

DEST_DIR="${1:-${INSTALL_DIR:-$HOME/.local/bin}}"
mkdir -p "$DEST_DIR"

# Determine download tool
DOWNLOADER=""
if command -v curl >/dev/null 2>&1; then
    DOWNLOADER="curl"
elif command -v wget >/dev/null 2>&1; then
    DOWNLOADER="wget"
else
    echo "Error: either curl or wget is required." >&2
    exit 1
fi

download_file() {
    local url="$1"
    local output="$2"
    if [ "$DOWNLOADER" = "curl" ]; then
        curl -fsSL -o "$output" "$url"
    elif [ "$DOWNLOADER" = "wget" ]; then
        wget -q -O "$output" "$url"
    fi
}

OS="$(uname -s)"
ARCH="$(uname -m)"

# Normalise OS & Architecture
case "$OS" in
    Linux)
        OS_TYPE="linux"
        ;;
    Darwin)
        OS_TYPE="darwin"
        ;;
    *)
        echo "Error: Unsupported operating system: $OS" >&2
        exit 1
        ;;
esac

case "$ARCH" in
    x86_64|amd64)
        ARCH_TYPE="amd64"
        ;;
    aarch64|arm64)
        ARCH_TYPE="arm64"
        ;;
    *)
        echo "Error: Unsupported architecture: $ARCH" >&2
        exit 1
        ;;
esac

# Function to get release asset URL for a given jq version
get_asset_url() {
    local version="$1"
    local base_url="https://github.com/jqlang/jq/releases/download"

    case "$version" in
        1.5|1.6)
            if [ "$OS_TYPE" = "linux" ]; then
                if [ "$ARCH_TYPE" = "amd64" ]; then
                    echo "${base_url}/jq-${version}/jq-linux64"
                else
                    echo ""
                fi
            elif [ "$OS_TYPE" = "darwin" ]; then
                echo "${base_url}/jq-${version}/jq-osx-x86_64"
            fi
            ;;
        1.7|1.7.1|1.8.0|1.8.1|1.8.2)
            if [ "$OS_TYPE" = "linux" ]; then
                if [ "$ARCH_TYPE" = "amd64" ]; then
                    echo "${base_url}/jq-${version}/jq-linux-amd64"
                elif [ "$ARCH_TYPE" = "arm64" ]; then
                    echo "${base_url}/jq-${version}/jq-linux-arm64"
                fi
            elif [ "$OS_TYPE" = "darwin" ]; then
                if [ "$ARCH_TYPE" = "amd64" ]; then
                    echo "${base_url}/jq-${version}/jq-macos-amd64"
                elif [ "$ARCH_TYPE" = "arm64" ]; then
                    echo "${base_url}/jq-${version}/jq-macos-arm64"
                fi
            fi
            ;;
        *)
            echo ""
            ;;
    esac
}

VERSIONS=(
    "1.5"
    "1.6"
    "1.7"
    "1.7.1"
    "1.8.0"
    "1.8.1"
    "1.8.2"
)

echo "Installing jq binaries into: $DEST_DIR"
echo "System: $OS ($ARCH)"
echo ""

TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT

installed_count=0
skipped_count=0
failed_count=0

for ver in "${VERSIONS[@]}"; do
    target_bin="jq-${ver}"
    target_path="$DEST_DIR/$target_bin"

    if [ -f "$target_path" ]; then
        echo "[SKIP] $target_bin already exists at $target_path"
        ((skipped_count++)) || true
        continue
    fi

    url="$(get_asset_url "$ver")"
    if [ -z "$url" ]; then
        echo "[WARN] No prebuilt asset available for jq-$ver on $OS_TYPE-$ARCH_TYPE" >&2
        ((failed_count++)) || true
        continue
    fi

    echo "[DOWNLOADING] jq-$ver from $url ..."
    temp_file="$TEMP_DIR/$target_bin"

    if download_file "$url" "$temp_file"; then
        chmod +x "$temp_file"
        
        # Verify binary runs
        if ! "$temp_file" --version >/dev/null 2>&1; then
            echo "[ERROR] Downloaded binary for jq-$ver failed execution check" >&2
            rm -f "$temp_file"
            ((failed_count++)) || true
            continue
        fi

        # Atomic move (non-overwriting check again)
        if [ ! -f "$target_path" ]; then
            mv "$temp_file" "$target_path"
            reported_ver="$("$target_path" --version 2>&1 || true)"
            echo "[INSTALLED] $target_bin -> $target_path ($reported_ver)"
            ((installed_count++)) || true
        else
            echo "[SKIP] $target_bin was created concurrently at $target_path"
            rm -f "$temp_file"
            ((skipped_count++)) || true
        fi
    else
        echo "[ERROR] Failed to download jq-$ver from $url" >&2
        rm -f "$temp_file"
        ((failed_count++)) || true
    fi
done

echo ""
echo "Summary: $installed_count installed, $skipped_count skipped, $failed_count failed."

if [[ ":$PATH:" != *":$DEST_DIR:"* ]]; then
    echo ""
    echo "Note: '$DEST_DIR' is not currently in your PATH."
    echo "Consider adding it to your PATH:"
    echo "  export PATH=\"$DEST_DIR:\$PATH\""
fi

if [ "$failed_count" -gt 0 ]; then
    exit 1
fi
