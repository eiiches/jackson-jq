#!/usr/bin/env bash

set -euo pipefail

readonly notice='Oracle designates this particular file as subject to the "Classpath" exception as provided by Oracle in the LICENSE file that accompanied this code.'

if (( $# == 0 )); then
	echo "No JEP 540 source files were provided" >&2
	exit 1
fi

status=0
for source in "$@"; do
	if ! awk -v notice="$notice" '
		{
			line = $0
			sub(/^[[:space:]]*\*[[:space:]]*/, "", line)
			text = text " " line
		}
		END {
			gsub(/[[:space:]]+/, " ", text)
			exit index(text, notice) == 0
		}
	' "$source"; then
		echo "Missing Classpath exception notice: $source" >&2
		status=1
	fi
done

exit "$status"
