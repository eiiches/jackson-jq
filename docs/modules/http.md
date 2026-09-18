# HTTP module

Maven artifact: `jackson-jq-ext-module-http`

jq module: `jackson-jq/http`

This module performs HTTP GET requests. Adding it allows jq programs to access the network, so only
make it available to programs that are trusted to choose their request URLs.

```jq
import "jackson-jq/http" as http;

http::get("https://example.com/data.json")
http::get("https://example.com/data.json"; {timeout: 10})
http::get("https://example.com/data.json"; {expected_status: [200, 204]})
```

The first argument is an HTTP or HTTPS URL string. The optional second argument is an object containing
`timeout` and/or `expected_status`. `timeout` is a positive number of seconds. The default is 30 seconds,
and the configured value is applied separately to connecting and reading, not as a deadline for the
whole request. `expected_status` is either one HTTP status number or a nonempty array of status numbers,
each from 100 through 599. Unknown object members are rejected.

The result has this shape:

```jq
{
  status: 200,
  headers: [{name: "Content-Type", value: "application/json"}],
  body: {example: true},
  raw_body: "<binary value>"
}
```

`status` is the final HTTP status. When `expected_status` is omitted, every response status is returned
normally, including 3xx, 4xx, and 5xx. When it is present, a final status outside the expected set
raises an error before the response body is read. `headers` contains one entry per header value, so
repeated fields remain separate. The module follows redirects only while the protocol remains the
same.

The module decodes `gzip` and `deflate` content encodings before producing either body field. For
`application/json` and `application/*+json`, `body` is the parsed JSON value; an empty JSON entity is
`null`. For `text/*`, it is a string decoded with the response's valid `charset` parameter or UTF-8
when no valid charset is present. For other media types, it is `null`.

`raw_body` contains the decompressed entity bytes. Providers with binary nodes, including the Jackson
2 and Jackson 3 providers, receive a binary value. Other providers receive padded Base64. Runtime
binary, string, array, and object limits apply to the response. Invalid inputs, transport failures,
timeouts, unsupported content encodings, and malformed JSON responses raise jq errors.
