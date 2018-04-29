jackson-jq
==========

[jq](http://stedolan.github.io/jq/) for Jackson JSON Processor

[![CircleCI](https://circleci.com/gh/eiiches/jackson-jq/tree/develop.svg?style=shield)](https://circleci.com/gh/eiiches/jackson-jq/tree/develop)

Installation
------------

Just add jackson-jq in your pom.xml.

```xml
<dependencies>
	<dependency>
		<groupId>net.thisptr</groupId>
		<artifactId>jackson-jq</artifactId>
		<version>0.0.8</version>
	</dependency>
</dependencies>
```

### Requirements

 - Java 8 or later

Usage
-----

### Basic Example

```java
ObjectMapper MAPPER = new ObjectMapper();

JsonQuery q = JsonQuery.compile("{ids:[.ids|split(\",\")[]|tonumber|.+100],name}");

JsonNode in = MAPPER.readTree("{\"ids\":\"12,15,23\",\"name\":\"jackson\",\"timestamp\":1418785331123}");
System.out.println(in);
// {"ids": "12,15,23", "name": "jackson", "timestamp": 1418785331123}

List<JsonNode> result = q.apply(in);
System.out.println(result);
// [{"ids": [112, 115, 123], "name": "jackson"}]
```

### Exposing Java variables

```java
Scope scope = new Scope();
scope.setValue("headers", MAPPER.readTree("{\"base\":10}"));
JsonQuery q = JsonQuery.compile("$headers.base + 3");
List<JsonNode> result = q.apply(scope, NullNode.getInstance());
System.out.println(result);
// [13]
```

### Defining custom functions

```java
Scope scope = new Scope();
scope.addFunction("repeat", 1, new Function() {
	@Override
	public List<JsonNode> apply(Scope scope, List<JsonQuery> args, JsonNode in) throws JsonQueryException {
		final List<JsonNode> out = new ArrayList<>();
		for (JsonNode arg : args.get(0).apply(in))
			out.add(new TextNode(Strings.repeat(in.asText(), arg.asInt())));
		return out;
	}
});
JsonQuery q = JsonQuery.compile(".name|repeat(3)");
List<JsonNode> result = q.apply(scope, MAPPER.readTree("{\"name\":\"a\"}"));
System.out.println(result);
// ["aaa"]
```

Using a jackson-jq command line tool
------------------------------------

We provide a CLI tool for testing a jackson-jq query. The tool has to be build with `mvn package`, but alternatively, Homebrew (or Linuxbrew) users can just `brew tap eiiches/jackson-jq && brew install jackson-jq` and `jackson-jq` will be available on $PATH.

```
$ bin/jackson-jq '.foo' <<< '{"foo":10}'
10
```

See `bin/jackson-jq --help` for more information.


Differences between jq and jackson-jq
-------------------------------------

Here is a *current* status of differences between jackson-jq and the jq. If you find something not in this list, please report an issue.

 - Missing language features in jackson-jq
   - [Breaking out of control structures](https://stedolan.github.io/jq/manual/#Breakingoutofcontrolstructures)
     - jq once had label-less `break` in `reduce` and `foreach` in the master branch, but the feature was removed without ever being shipped as jq-1.5. Actually, jackson-jq implemented it then and still has it. Proper `label $out` and `break $out` syntax will be implemented in the future version of jackson-jq. e.g)
        - `jackson-jq -n '(1,2,3) | label $out | if . == 2 then break $out else . end'` does not work.

   - [Complex assignments](https://stedolan.github.io/jq/manual/#Complexassignments)
     - Currently, complex assignments only work when the left-hand side is a simple field access. Won't work if `select/1` or any filters are used in left-hand side. e.g)
        - `jackson-jq '.a[]|.b += 10' <<< '{"a": [{"b": 1}, {"b": 2}]}` does work.
        - `jackson-jq '.a[]|select(.b>1) += 10' <<< '{"a": [{"b": 1}, {"b": 2}]}'` does not work.

   - [Modules](https://stedolan.github.io/jq/manual/#Modules)
   - [Streaming](https://stedolan.github.io/jq/manual/#Streaming)
   - [I/O](https://stedolan.github.io/jq/manual/#IO)
   - `{$foo}` syntax, a syntactic sugar for `{foo:$foo}` (#24)

 - Missing functions in jackson-jq
   - Datetime functions: `fromdate/0`, `mktime/0`, `gmtime/0`
   - Path manipulation functions: `getpath/1`, `setpath/2` and `delpaths/1`
   - Others:
     - `env/0`
     - `bsearch/1`
     - and more

 - Known corner cases
   - When the function with the same name is defined more than once in the same-level scope, jackson-jq uses the last one. e.g) `def f: 1; def g: f; def f: 2; g` evaluates to 2 in jackson-jq, while jq evaluates it to 1.

Additionally, test cases used in jackson-jq (mostly from the jq unit tests) might be useful to learn exactly what queries are working and what are not working.

 - Test cases not working
   - [jackson-jq/src/test/resources/jq-test-all-ng.json](jackson-jq/src/test/resources/jq-test-all-ng.json)
   - [jackson-jq/src/test/resources/jq-test-manual-ng.json](jackson-jq/src/test/resources/jq-test-manual-ng.json)
   - [jackson-jq/src/test/resources/jq-test-onig-ng.json](jackson-jq/src/test/resources/jq-test-onig-ng.json)

 - Test cases working
   - [jackson-jq/src/test/resources/jq-test-all-ok.json](jackson-jq/src/test/resources/jq-test-all-ok.json)
   - [jackson-jq/src/test/resources/jq-test-manual-ok.json](jackson-jq/src/test/resources/jq-test-manual-ok.json)
   - [jackson-jq/src/test/resources/jq-test-onig-ok.json](jackson-jq/src/test/resources/jq-test-onig-ok.json)
   - [jackson-jq/src/test/resources/jq-test-extra-ok.json](jackson-jq/src/test/resources/jq-test-extra-ok.json)

Using jackson-jq-extra module
-----------------------------

`jackson-jq-extra` module provides extra functions that you might find useful. These functions do not exist in jq.

### POM

```xml
<dependencies>
    <dependency>
        <groupId>net.thisptr</groupId>
        <artifactId>jackson-jq-extra</artifactId>
        <version>0.0.8</version>
    </dependency>
</dependencies>
```

### Examples

#### uuid4/0

 - `jackson-jq -n 'uuid4'` #=> `"a69cf146-f40e-42e1-ae88-12590bdae947"`

#### random/0

 - `jackson-jq -n 'random'` #=> `0.43292159535427466`

#### timestamp/0, strptime/{1, 2}, strftime/{1, 2}

 - `jackson-jq -n 'timestamp'` #=> `1477162056362`
 - `jackson-jq -n '1477162342372 | strftime("yyyy-MM-dd HH:mm:ss.SSSXXX")'` #=> `"2016-10-23 03:52:22.372+09:00"`
 - `jackson-jq -n '1477162342372 | strftime("yyyy-MM-dd HH:mm:ss.SSSXXX"; "UTC")'` #=> `"2016-10-22 18:52:22.372Z"`
 - `jackson-jq -n '"2016-10-23 03:52:22.372+09:00" | strptime("yyyy-MM-dd HH:mm:ss.SSSXXX")'` #=> `1477162342372`
 - `jackson-jq -n '"2016-10-22 18:52:22.372" | strptime("yyyy-MM-dd HH:mm:ss.SSS"; "UTC")'` #=> `1477162342372`

#### uriparse/0

 - `jackson-jq -n '"http://user@www.example.com:8080/index.html?foo=1&bar=%20#hash" | uriparse'` #=>
 
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

 - `jackson-jq -n '"%66%6f%6f" | uridecode'` #=> `"foo"`

#### hostname/0

 - `jackson-jq -n 'hostname'` #=> `"jenkins-slave01"`

License
-------

This software is licensed under Apache Software License, Version 2.0, with some exceptions:

 - [jackson-jq/src/test/resources](jackson-jq/src/test/resources) contains test cases from [stedolan/jq](https://github.com/stedolan/jq).
 - [jackson-jq/src/main/resources/net/thisptr/jackson/jq/jq.json](jackson-jq/src/main/resources/net/thisptr/jackson/jq/jq.json) contains function definitions extracted from [stedolan/jq](https://github.com/stedolan/jq).

See [COPYING](COPYING) for details.
