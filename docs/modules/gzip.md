# gzip module

Maven artifact: `jackson-jq-ext-module-gzip`

jq module: `jackson-jq/gzip`

This module provides gzip compression and decompression:

```jq
import "jackson-jq/gzip" as gzip;
```

The filters are named after the *uncompressed* side of the conversion. Compressed data is always a
binary value: a binary node on a JSON provider that has a binary node type, such as the Jackson 2 or
Jackson 3 providers, and a padded standard Base64 string on every other provider. This is the same
representation `fs::read_binary` and `http::get`'s `raw_body` use, so those results can be piped
straight in.

| Function | Input | Output |
| --- | --- | --- |
| `compress_binary/0` | a binary value, or a string containing standard Base64 | a binary value |
| `compress_text/{0,1}` | a string | a binary value |
| `decompress_binary/0` | a binary value, or a string containing standard Base64 | a binary value |
| `decompress_text/{0,1}` | a binary value, or a string containing standard Base64 | a string |

```console
$ jackson-jq -n 'import "jackson-jq/gzip" as gzip; "hello" | gzip::compress_text | gzip::decompress_text'
"hello"
```

`compress_text` and `decompress_text` encode and decode as UTF-8 by default. Their optional argument is
an object whose `encoding` member names any charset available to `Charset.forName` on the running JVM:

```console
$ jackson-jq -n 'import "jackson-jq/gzip" as gzip; "café" | gzip::compress_text({encoding: "ISO-8859-1"}) | gzip::decompress_text({encoding: "ISO-8859-1"})'
"café"
```

Malformed or unmappable text raises an error instead of being replaced, as in `fs::read_text`.

`RuntimeOptions.Builder.setMaxBinaryLength(...)` bounds binary results, while `setMaxStringLength(...)`
bounds Base64 results and the text `decompress_text` returns; decompression stops as soon as the
relevant limit would be exceeded.

The module uses Java's built-in gzip implementation.
