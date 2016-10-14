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
		<version>0.0.6</version>
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

Commandline Tool
----------------

`jackson-jq` provides a command line tool useful for testing and debugging purpose.

```
$ bin/jackson-jq '.foo' <<< '{"foo":10}'
10
```

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
