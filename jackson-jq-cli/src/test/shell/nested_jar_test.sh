#!/bin/sh

set -eu

jar_file="$1"

# Locate java executable
if [ -n "${JAVA_RUNFILES:-}" ] && [ -x "${JAVA_RUNFILES}/bin/java" ]; then
    java_cmd="${JAVA_RUNFILES}/bin/java"
elif command -v java >/dev/null 2>&1; then
    java_cmd="java"
else
    echo "java executable not found" >&2
    exit 1
fi

# 1. Verify archive structure
jar_entries="$("$java_cmd" -jar -Ddummy=true -xf "$jar_file" 2>/dev/null || true)"
# Use unzip -l to list entries
jar_list="$(unzip -l "$jar_file")"

echo "$jar_list" | grep -q "BOOT-INF/classes/net/thisptr/jackson/jq/v2/cli/Main.class" || {
    echo "Missing BOOT-INF/classes/net/thisptr/jackson/jq/v2/cli/Main.class in $jar_file" >&2
    exit 1
}

echo "$jar_list" | grep -q "BOOT-INF/lib/" || {
    echo "Missing BOOT-INF/lib/ in $jar_file" >&2
    exit 1
}

echo "$jar_list" | grep -q "org/springframework/boot/loader/launch/JarLauncher.class" || {
    echo "Missing org/springframework/boot/loader/launch/JarLauncher.class in $jar_file" >&2
    exit 1
}

echo "$jar_list" | grep -q "META-INF/services/java.nio.file.spi.FileSystemProvider" || {
    echo "Missing FileSystemProvider in $jar_file" >&2
    exit 1
}

# 2. Verify manifest entries
manifest="$(unzip -p "$jar_file" META-INF/MANIFEST.MF)"

echo "$manifest" | grep -q "Main-Class: org.springframework.boot.loader.launch.JarLauncher" || {
    echo "Incorrect or missing Main-Class in manifest:" >&2
    echo "$manifest" >&2
    exit 1
}

echo "$manifest" | grep -q "Start-Class: net.thisptr.jackson.jq.v2.cli.Main" || {
    echo "Incorrect or missing Start-Class in manifest:" >&2
    echo "$manifest" >&2
    exit 1
}

# 3. Verify nested JARs are stored uncompressed (STORED / method 0)
# zipinfo -v outputs "compression method:        none (stored)" or similar
zipinfo -v "$jar_file" "BOOT-INF/lib/*.jar" | grep "compression method" | while read -r line; do
    if ! echo "$line" | grep -qi "none (stored)"; then
        echo "Found compressed nested jar: $line" >&2
        exit 1
    fi
done

# 4. Functional execution tests
assert_output() {
    expected="$1"
    shift
    actual="$("$java_cmd" -jar "$jar_file" "$@")"
    if [ "$actual" != "$expected" ]; then
        echo "expected: $expected" >&2
        echo "actual:   $actual" >&2
        exit 1
    fi
}

for provider in jackson2 jackson3 fastjson2 gson jakarta; do
    assert_output '{"value":2}' --json-provider "$provider" -cn '{value: 1 + 1}'
done

assert_output '"bcd"' -n '"abcde" | match("bcd").string'
assert_output 'true' -n \
    'import "jackson-jq/debug" as debug;
     import "jackson-jq/fs" as fs;
     import "jackson-jq/http" as http;
     true'
assert_output '"hello"' -n \
    'import "jackson-jq/binary" as binary;
     "hello" | binary::encode_text | binary::decode_text'
assert_output '"hello"' -n \
    'import "jackson-jq/gzip" as gzip;
     "hello" | gzip::compress_text | gzip::decompress_text'
assert_output '"hello"' -n \
    'import "jackson-jq/zstd" as zstd;
     "hello" | zstd::compress_text | zstd::decompress_text'
assert_output 'true' -n \
    'import "jackson-jq/re2" as re;
     "abc" | re::test("^a")'
assert_output '"2016-10-22 18:52:22.372Z"' -n \
    'import "jackson-jq/time" as time;
     1477162342372 | time::strftime("yyyy-MM-dd HH:mm:ss.SSSXXX"; "UTC")'
assert_output '"foo"' -n \
    'import "jackson-jq/uri" as uri;
     "%66%6f%6f" | uri::uridecode'
assert_output '"cfbff0d1-9375-5685-968c-48ce8b15ae17"' -n \
    'import "jackson-jq/uuid" as uuid;
     "example.com" | uuid::uuid5("6ba7b810-9dad-11d1-80b4-00c04fd430c8")'

echo "All nested jar tests passed!"
