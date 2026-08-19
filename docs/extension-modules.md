# Using extension modules

Functions that do not exist in jq are provided by separate extension modules. Add only the dependencies your application needs and configure the environment with `ClassPathModuleLoader` (see [jackson-jq-core/src/test/java/examples/Usage.java](../jackson-jq-core/src/test/java/examples/Usage.java)).

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

Then import it in jq:

```jq
import "jackson-jq/uuid" as uuid;

uuid::uuid4
```

Extension functions are available only through their imported module namespace.

<details>
<summary>List of Functions</summary>

#### uuid4/0

 - `jackson-jq -n 'import "jackson-jq/uuid" as uuid; uuid::uuid4'` #=> `"a69cf146-f40e-42e1-ae88-12590bdae947"`

#### random/0

 - `jackson-jq -n 'import "jackson-jq/random" as random; random::random'` #=> `0.43292159535427466`

#### timestamp/0, strptime/{1, 2}, strftime/{1, 2}

 - `jackson-jq -n 'import "jackson-jq/time" as time; time::timestamp'` #=> `1477162056362`
 - `jackson-jq -n 'import "jackson-jq/time" as time; 1477162342372 | time::strftime("yyyy-MM-dd HH:mm:ss.SSSXXX"; "UTC")'` #=> `"2016-10-22 18:52:22.372Z"`
 - `jackson-jq -n 'import "jackson-jq/time" as time; "2016-10-22 18:52:22.372" | time::strptime("yyyy-MM-dd HH:mm:ss.SSS"; "UTC")'` #=> `1477162342372`

#### uriparse/0

 - `jackson-jq -n 'import "jackson-jq/uri" as uri; "http://user@www.example.com:8080/index.html?foo=1&bar=%20#hash" | uri::uriparse'` #=>
 
   ```json
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

#### uridecode/0

 - `jackson-jq -n 'import "jackson-jq/uri" as uri; "%66%6f%6f" | uri::uridecode'` #=> `"foo"`

</details>
