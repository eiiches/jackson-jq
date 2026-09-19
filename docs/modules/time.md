# Time module

Maven artifact: `jackson-jq-ext-module-time`

jq module: `jackson-jq/time`

```jq
import "jackson-jq/time" as time;
```

Date and time patterns use Java's `SimpleDateFormat` syntax.

## `timestamp/0`

Returns the current Unix timestamp in milliseconds.

```console
$ jackson-jq -n 'import "jackson-jq/time" as time; time::timestamp'
1477162056362
```

## `strptime/{1,2}`

Parses the input string using the format supplied as the first argument and returns the corresponding Unix timestamp in milliseconds. The optional second argument is a time zone ID. When it is omitted, the system default time zone is used.

```console
$ jackson-jq -n 'import "jackson-jq/time" as time; "2016-10-22 18:52:22.372" | time::strptime("yyyy-MM-dd HH:mm:ss.SSS"; "UTC")'
1477162342372
```

## `strftime/{1,2}`

Formats the input Unix timestamp, expressed in milliseconds, using the format supplied as the first argument. The optional second argument is a time zone ID. When it is omitted, the system default time zone is used.

```console
$ jackson-jq -n 'import "jackson-jq/time" as time; 1477162342372 | time::strftime("yyyy-MM-dd HH:mm:ss.SSSXXX"; "UTC")'
"2016-10-22 18:52:22.372Z"
```
