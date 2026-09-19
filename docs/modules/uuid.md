# UUID module

Maven artifact: `jackson-jq-ext-module-uuid`

jq module: `jackson-jq/uuid`

```jq
import "jackson-jq/uuid" as uuid;
```

## `uuid3/1`

Generates a deterministic version 3 UUID from the input and a namespace UUID. The input must be a string or binary value, and the namespace argument must be a valid UUID string. String input is encoded as UTF-8, and the UUID is generated using MD5.

```console
$ jackson-jq -n 'import "jackson-jq/uuid" as uuid; "example.com" | uuid::uuid3("6ba7b810-9dad-11d1-80b4-00c04fd430c8")'
"9073926b-929f-31c2-abc9-fad77ae3e8eb"
```

## `uuid4/0`

Generates a random version 4 UUID and returns it as a string.

```console
$ jackson-jq -n 'import "jackson-jq/uuid" as uuid; uuid::uuid4'
"a69cf146-f40e-42e1-ae88-12590bdae947"
```

## `uuid5/1`

Generates a deterministic version 5 UUID from the input and a namespace UUID. The input must be a string or binary value, and the namespace argument must be a valid UUID string. String input is encoded as UTF-8, and the UUID is generated using SHA-1.

```console
$ jackson-jq -n 'import "jackson-jq/uuid" as uuid; "example.com" | uuid::uuid5("6ba7b810-9dad-11d1-80b4-00c04fd430c8")'
"cfbff0d1-9375-5685-968c-48ce8b15ae17"
```
