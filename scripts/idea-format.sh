#!/usr/bin/env bash
set -euo pipefail

readonly FORMATTER_VERSION="0.0.3"
readonly FORMATTER_SHA256="32eebd2924499f4956648ff457e7033321f583ff541ad075328c9a2c1df6e0ef"
readonly FORMATTER_URL="https://github.com/ICIJ/intellij-code-formatter/releases/download/${FORMATTER_VERSION}/intellij-code-formatter-${FORMATTER_VERSION}.jar"

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_dir="$(cd "${script_dir}/.." && pwd)"
cache_root="${INTELLIJ_CODE_FORMATTER_CACHE_DIR:-${XDG_CACHE_HOME:-${HOME}/.cache}/jackson-jq/intellij-code-formatter}"
formatter_dir="${cache_root}/${FORMATTER_VERSION}"
formatter_jar="${formatter_dir}/intellij-code-formatter-${FORMATTER_VERSION}.jar"
launcher_dir="${formatter_dir}/launcher"

usage() {
	cat <<EOF
Usage: $0 [--check]

Without arguments, formats every Java source file in the repository.
With --check, verifies formatting without changing files.
EOF
}

case "${1:-}" in
	"")
		formatter_mode=()
		;;
	--check)
		formatter_mode=(--check)
		;;
	-h|--help)
		usage
		exit 0
		;;
	*)
		usage >&2
		exit 2
		;;
esac

if [ "$#" -gt 1 ]; then
	usage >&2
	exit 2
fi

if ! command -v java >/dev/null 2>&1; then
	echo "Error: JDK 21 or later is required." >&2
	exit 2
fi

if ! command -v javac >/dev/null 2>&1; then
	echo "Error: JDK 21 or later is required (javac was not found)." >&2
	exit 2
fi

java_version="$(java -version 2>&1 | sed -n '1s/.*version "\([0-9][0-9]*\).*/\1/p')"
if [ -z "$java_version" ] || [ "$java_version" -lt 21 ]; then
	echo "Error: JDK 21 or later is required." >&2
	exit 2
fi

checksum() {
	if command -v sha256sum >/dev/null 2>&1; then
		sha256sum "$1" | awk '{ print $1 }'
	elif command -v shasum >/dev/null 2>&1; then
		shasum -a 256 "$1" | awk '{ print $1 }'
	else
		echo "Error: sha256sum or shasum is required." >&2
		return 2
	fi
}

download() {
	local destination="$1"
	if command -v curl >/dev/null 2>&1; then
		curl -fsSL --retry 3 --output "$destination" "$FORMATTER_URL"
	elif command -v wget >/dev/null 2>&1; then
		wget -q --tries=3 --output-document="$destination" "$FORMATTER_URL"
	else
		echo "Error: curl or wget is required." >&2
		return 2
	fi
}

if [ ! -f "$formatter_jar" ] || [ "$(checksum "$formatter_jar")" != "$FORMATTER_SHA256" ]; then
	mkdir -p "$formatter_dir"
	temporary_jar="$(mktemp "${formatter_dir}/.intellij-code-formatter.XXXXXX")"
	trap 'rm -f "$temporary_jar"' EXIT

	echo "Downloading IntelliJ code formatter ${FORMATTER_VERSION}..." >&2
	download "$temporary_jar"

	actual_sha256="$(checksum "$temporary_jar")"
	if [ "$actual_sha256" != "$FORMATTER_SHA256" ]; then
		echo "Error: formatter checksum mismatch: expected ${FORMATTER_SHA256}, got ${actual_sha256}." >&2
		exit 2
	fi

	mv -f "$temporary_jar" "$formatter_jar"
	trap - EXIT
fi

mkdir -p "$launcher_dir"
javac -d "$launcher_dir" "${script_dir}/IdeaFormatLauncher.java"

exec java \
	--add-opens java.base/java.lang=ALL-UNNAMED \
	--add-opens java.base/java.lang.reflect=ALL-UNNAMED \
	--add-opens java.base/java.io=ALL-UNNAMED \
	--add-opens java.base/java.util=ALL-UNNAMED \
	--add-opens java.base/java.util.concurrent=ALL-UNNAMED \
	--add-opens java.desktop/sun.awt=ALL-UNNAMED \
	--add-opens java.desktop/java.awt=ALL-UNNAMED \
	--add-opens java.desktop/javax.swing=ALL-UNNAMED \
	-Djava.awt.headless=true \
	--class-path "${launcher_dir}:${formatter_jar}" \
	IdeaFormatLauncher \
	"${project_dir}/docs/idea-code-style.xml" \
	"${formatter_mode[@]}" \
	"$project_dir"
