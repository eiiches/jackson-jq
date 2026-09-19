# URI module

Maven artifact: `jackson-jq-ext-module-uri`

jq module: `jackson-jq/uri`

```jq
import "jackson-jq/uri" as uri;
```

## `uriparse/0`

Parses the input URI string and returns an object containing its decoded and raw components. The `query_obj` field contains decoded query parameters. A parameter with one value is represented as a string, while a repeated parameter is represented as an array.

```console
$ jackson-jq -n 'import "jackson-jq/uri" as uri; "http://user@www.example.com:8080/index.html?foo=1&bar=%20#hash" | uri::uriparse'
{
  "scheme" : "http",
  "user_info" : "user",
  "raw_user_info" : "user",
  "host" : "www.example.com",
  "port" : 8080,
  "authority" : "user@www.example.com:8080",
  "raw_authority" : "user@www.example.com:8080",
  "path" : "/index.html",
  "raw_path" : "/index.html",
  "query" : "foo=1&bar= ",
  "raw_query" : "foo=1&bar=%20",
  "query_obj" : {
    "bar" : " ",
    "foo" : "1"
  },
  "fragment" : "hash",
  "raw_fragment" : "hash"
}
```

## `uridecode/0`

Decodes the input string as UTF-8 URL-encoded data. Percent-encoded bytes are decoded, and plus signs are converted to spaces.

```console
$ jackson-jq -n 'import "jackson-jq/uri" as uri; "%66%6f%6f" | uri::uridecode'
"foo"
```
