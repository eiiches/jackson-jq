# binary module

Maven artifact: `jackson-jq-ext-module-binary`

jq module: `jackson-jq/binary`

This module provides functions to decode and encode text from and to binary values:

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

`RuntimeOptions.Builder.setMaxBinaryLength(...)` bounds binary results produced by `encode_text`, while
`setMaxStringLength(...)` bounds Base64 results and the text returned by `decode_text`.
