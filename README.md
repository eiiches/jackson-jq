jackson-jq
==========

[jq](http://stedolan.github.io/jq/) for Jackson JSON Processor

Installation
------------

Just add jackson-jq in your pom.xml.

```xml
<dependencies>
	<dependency>
		<groupId>net.thisptr</groupId>
		<artifactId>jackson-jq</artifactId>
		<version>0.0.7-SNAPSHOT</version>
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
   - Modules
   - Complex assignments
     - e.g) `jq '(.[]|select(. > 0)) += 10' <<< '[-1, 0, 1]'`
   - Destructuring
     - e.g) `[1, 2] as [$a, $b]`
   - `try` without `catch` clause
     - `catch` clause is required in jackson-jq.
     - e.g) `try .a` fails to compile. Try `try .a catch empty` instead.
   - `(expression)?` syntax
     - `?` can be only used after *accessors* in jackson-jq.
     - e.g) `(.a)?` does not compile in jackson-jq (while `.a?` does).
   - Streams

 - Missing functions in jackson-jq
   - Datetime functions: `fromdate/0`, `mktime/0`, `gmtime/0`
   - Path manipulation functions: `getpath/1`, `setpath/2` and `delpaths/1`
   - Others:
     - `env/0`
     - `bsearch/1`
     - `utf8bytelength`
     - and more

 - Known corner cases
   - When the function with the same name is defined more than once in the same-level scope, jackson-jq uses the last one. e.g) `def f: 1; def g: f; def f: 2; g` evaluates to 2 in jackson-jq, while jq evaluates it to 1.

Additionally, test cases used in jackson-jq (mostly from the jq unit tests) might be useful to learn exactly what queries are working and what are not working.

 - Test cases not working
   - [jackson-jq/src/test/resources/jq-test-all-ng.json](jackson-jq/src/test/resources/jq-test-all-ng.json)
   - [jackson-jq/src/test/resources/jq-test-official-ng.json](jackson-jq/src/test/resources/jq-test-official-ng.json)

 - Test cases working
   - [jackson-jq/src/test/resources/jq-test-all-ok.json](jackson-jq/src/test/resources/jq-test-all-ok.json)
   - [jackson-jq/src/test/resources/jq-test-official-ok.json](jackson-jq/src/test/resources/jq-test-official-ok.json)
   - [jackson-jq/src/test/resources/jq-test-extra-ok.json](jackson-jq/src/test/resources/jq-test-extra-ok.json)

Extra modules
-------------

### jackson-jq-extra

This module provides the following functions:

 - uuid4
 - random
 - strptime
 - strftime
 - uriparse
 - uridecode
 - hostname
 - timestamp

License
-------

This software is licensed under Apache Software License, Version 2.0, with some exceptions:

 - [jackson-jq/src/test/resources](jackson-jq/src/test/resources) contains test cases from [stedolan/jq](https://github.com/stedolan/jq).
 - [jackson-jq/src/main/resources/jq.json](jackson-jq/src/main/resources/jq.json) contains function definitions extracted from [stedolan/jq](https://github.com/stedolan/jq).

See [COPYING](COPYING) for details.
