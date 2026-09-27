# binary module

Maven artifact: `jackson-jq-ext-module-binary`

jq module: `jackson-jq/binary`

This module provides functions to inspect binary values and convert them to and from text, hex, Base64, Base64URL, and byte arrays:

```jq
import "jackson-jq/binary" as binary;
```

Binary data is represented as a binary node on a JSON provider that has a binary node type, such as the
Jackson 2 or Jackson 3 providers, and as a padded standard Base64 string on every other provider. This
is the same representation `fs::read_binary` and `http::get`'s `raw_body` use, so those results can be
piped directly into `binary::decode_text`.

| Function | Input | Output |
| --- | --- | --- |
| `decode_text/{0,1}` | a binary value, or a string containing standard Base64 | a string |
| `encode_text/{0,1}` | a string | a binary value |
| `size/0` | a binary value | its byte count |
| `to_hex/0` | a binary value | a lowercase hex string |
| `from_hex/0` | a hex string | a binary value |
| `to_base64/0` | a binary value | a padded standard Base64 string |
| `from_base64/0` | a standard Base64 string | a binary value |
| `to_base64url/0` | a binary value | an unpadded Base64URL string |
| `from_base64url/0` | a Base64URL string | a binary value |
| `to_bytes/0` | a binary value | an array of integers from 0 to 255 |
| `from_bytes/0` | an array of integers from 0 to 255 | a binary value |

Every function that accepts a binary value also accepts its standard Base64 string representation,
including on providers with native binary nodes. `from_hex` accepts uppercase or lowercase hex digits
and rejects whitespace, invalid digits, and an odd number of digits. `from_bytes` rejects values that
are not exact integers from 0 to 255.
`from_base64url` accepts padded and unpadded Base64URL; `to_base64url` always omits padding.

```jq
"hello" | binary::encode_text | binary::size             # 5
"hello" | binary::encode_text | binary::to_hex           # "68656c6c6f"
"68656c6c6f" | binary::from_hex | binary::decode_text  # "hello"
[0, 127, 255] | binary::from_bytes | binary::to_bytes   # [0, 127, 255]
```

```console
$ jackson-jq -n 'import "jackson-jq/binary" as binary; "hello" | binary::encode_text | binary::decode_text'
"hello"
```

`decode_text` and `encode_text` decode and encode as UTF-8 by default. Their optional argument is an
object whose `encoding` member names any charset available to `Charset.forName` on the running JVM:

```console
$ jackson-jq -n 'import "jackson-jq/binary" as binary; "café" | binary::encode_text({encoding: "ISO-8859-1"}) | binary::decode_text({encoding: "ISO-8859-1"})'
"café"
```

Malformed byte sequences or unmappable text raise an error instead of being silently replaced:

```console
$ jackson-jq -n 'import "jackson-jq/binary" as binary; "あ" | binary::encode_text({encoding: "ISO-8859-1"})'
jackson-jq: error: binary::encode_text failed to encode the input using ISO-8859-1: ...
```

`RuntimeOptions.Builder.setMaxBinaryLength(...)` bounds native binary results, while
`setMaxStringLength(...)` bounds Base64 fallback values and strings returned by conversions.
`setMaxArrayLength(...)` bounds arrays returned by `to_bytes`.
