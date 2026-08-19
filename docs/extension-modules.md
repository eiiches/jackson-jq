# Using extension modules

Additional functions that are not part of jq are available through separate extension modules. Add only the dependencies your application needs and configure its `Environment` with a `ClassPathModuleLoader` (see [jackson-jq-core/src/test/java/examples/Usage.java](../jackson-jq-core/src/test/java/examples/Usage.java)).

| Maven artifact | jq module | Functions |
| --- | --- | --- |
| `jackson-jq-ext-module-uuid` | `jackson-jq/uuid` | `uuid3/1`, `uuid4/0`, `uuid5/1` |
| `jackson-jq-ext-module-time` | `jackson-jq/time` | `timestamp/0`, `strptime/{1,2}`, `strftime/{1,2}` |
| `jackson-jq-ext-module-uri` | `jackson-jq/uri` | `uriparse/0`, `uridecode/0` |
| `jackson-jq-ext-module-random` | `jackson-jq/random` | `random/0` |

For example, add the UUID extension:

```xml
<dependency>
	<groupId>net.thisptr.jackson.jq.v2</groupId>
	<artifactId>jackson-jq-ext-module-uuid</artifactId>
	<version>2.0.0-SNAPSHOT</version>
</dependency>
```

Import the module in the jq program:

```jq
import "jackson-jq/uuid" as uuid;

uuid::uuid4
```

Extension functions are available only through their imported module namespace.

## `jackson-jq-ext-module-uuid`

jq module: `jackson-jq/uuid`

```jq
import "jackson-jq/uuid" as uuid;
```

### `uuid3/1`

Generates a deterministic version 3 UUID from the input and a namespace UUID. The input must be a string or binary value, and the namespace argument must be a valid UUID string. String input is encoded as UTF-8, and the UUID is generated using MD5.

```console
$ jackson-jq -n 'import "jackson-jq/uuid" as uuid; "example.com" | uuid::uuid3("6ba7b810-9dad-11d1-80b4-00c04fd430c8")'
"9073926b-929f-31c2-abc9-fad77ae3e8eb"
```

### `uuid4/0`

Generates a random version 4 UUID and returns it as a string.

```console
$ jackson-jq -n 'import "jackson-jq/uuid" as uuid; uuid::uuid4'
"a69cf146-f40e-42e1-ae88-12590bdae947"
```

### `uuid5/1`

Generates a deterministic version 5 UUID from the input and a namespace UUID. The input must be a string or binary value, and the namespace argument must be a valid UUID string. String input is encoded as UTF-8, and the UUID is generated using SHA-1.

```console
$ jackson-jq -n 'import "jackson-jq/uuid" as uuid; "example.com" | uuid::uuid5("6ba7b810-9dad-11d1-80b4-00c04fd430c8")'
"cfbff0d1-9375-5685-968c-48ce8b15ae17"
```

## `jackson-jq-ext-module-time`

jq module: `jackson-jq/time`

```jq
import "jackson-jq/time" as time;
```

Date and time patterns use Java's `SimpleDateFormat` syntax.

### `timestamp/0`

Returns the current Unix timestamp in milliseconds.

```console
$ jackson-jq -n 'import "jackson-jq/time" as time; time::timestamp'
1477162056362
```

### `strptime/{1,2}`

Parses the input string using the format supplied as the first argument and returns the corresponding Unix timestamp in milliseconds. The optional second argument is a time zone ID. When it is omitted, the system default time zone is used.

```console
$ jackson-jq -n 'import "jackson-jq/time" as time; "2016-10-22 18:52:22.372" | time::strptime("yyyy-MM-dd HH:mm:ss.SSS"; "UTC")'
1477162342372
```

### `strftime/{1,2}`

Formats the input Unix timestamp, expressed in milliseconds, using the format supplied as the first argument. The optional second argument is a time zone ID. When it is omitted, the system default time zone is used.

```console
$ jackson-jq -n 'import "jackson-jq/time" as time; 1477162342372 | time::strftime("yyyy-MM-dd HH:mm:ss.SSSXXX"; "UTC")'
"2016-10-22 18:52:22.372Z"
```

## `jackson-jq-ext-module-uri`

jq module: `jackson-jq/uri`

```jq
import "jackson-jq/uri" as uri;
```

### `uriparse/0`

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

### `uridecode/0`

Decodes the input string as UTF-8 URL-encoded data. Percent-encoded bytes are decoded, and plus signs are converted to spaces.

```console
$ jackson-jq -n 'import "jackson-jq/uri" as uri; "%66%6f%6f" | uri::uridecode'
"foo"
```

## `jackson-jq-ext-module-random`

jq module: `jackson-jq/random`

```jq
import "jackson-jq/random" as random;
```

### `random/0`

Returns a pseudorandom number greater than or equal to `0.0` and less than `1.0`.

```console
$ jackson-jq -n 'import "jackson-jq/random" as random; random::random'
0.43292159535427466
```
