#!/usr/bin/env python3

# Implements the credential helper protocol specified at:
# https://github.com/EngFlow/credential-helper-spec/

import json
import sys
from pathlib import Path
from urllib.parse import urlparse


BUILDBUDDY_HOST = "remote.buildbuddy.io"


def fail(message: str) -> int:
    print(message, file=sys.stderr)
    return 1


def main() -> int:
    if sys.argv[1:] != ["get"]:
        return fail("usage: credential-helper get")

    try:
        request = json.load(sys.stdin)
    except (json.JSONDecodeError, UnicodeDecodeError):
        return fail("credential request is not valid JSON")

    if not isinstance(request, dict) or not isinstance(request.get("uri"), str):
        return fail("credential request must contain a URI")

    if urlparse(request["uri"]).hostname != BUILDBUDDY_HOST:
        return fail("credential request is not for BuildBuddy")

    try:
        api_key = Path(__file__).with_name("api-key").read_text(encoding="utf-8")
    except OSError:
        return fail("BuildBuddy API key is unavailable")

    if not api_key:
        return fail("BuildBuddy API key is empty")

    json.dump(
        {"headers": {"x-buildbuddy-api-key": [api_key]}},
        sys.stdout,
        separators=(",", ":"),
    )
    sys.stdout.write("\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
