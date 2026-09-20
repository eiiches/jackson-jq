#!/bin/sh

set -eu

jackson_jq="$1"

assert_output() {
    expected="$1"
    shift
    actual="$("$jackson_jq" "$@")"
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
assert_output '""' -n \
    'import "jackson-jq/fs" as fs;
     fs::read_text("/dev/null")'
assert_output '"number"' -n \
    'import "jackson-jq/random" as random;
     random::random | type'
