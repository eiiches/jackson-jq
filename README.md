jackson-jq
==========

A pure-Java, embeddable [jq](http://stedolan.github.io/jq/) implementation with pluggable JSON providers.

[![GitHub Actions](https://github.com/eiiches/jackson-jq/workflows/test/badge.svg)](https://github.com/eiiches/jackson-jq/actions)


Getting started
---------------

Java 8 or later is required.
If you use Maven, add `jackson-jq-core` and the appropriate JSON provider to the `<dependencies>` section of your POM. Add the regex implementation only if your application uses regex functions.

```xml
<dependency>
	<groupId>net.thisptr.jackson.jq.v2</groupId>
	<artifactId>jackson-jq-core</artifactId>
	<version>2.0.0-alpha1</version>
</dependency>

<!-- Optional: add this dependency if your application uses regex functions -->
<dependency>
	<groupId>net.thisptr.jackson.jq.v2</groupId>
	<artifactId>jackson-jq-regex-impl-joni</artifactId>
	<version>2.0.0-alpha1</version>
</dependency>

<!-- Choose one JSON provider that matches the JSON library your application uses -->
<dependency>
	<groupId>net.thisptr.jackson.jq.v2</groupId>
	<artifactId>jackson-jq-json-provider-impl-jackson2</artifactId>
	<version>2.0.0-alpha1</version>
</dependency>
<dependency>
	<groupId>net.thisptr.jackson.jq.v2</groupId>
	<artifactId>jackson-jq-json-provider-impl-jackson3</artifactId>
	<version>2.0.0-alpha1</version>
</dependency>
<dependency>
	<groupId>net.thisptr.jackson.jq.v2</groupId>
	<artifactId>jackson-jq-json-provider-impl-gson</artifactId>
	<version>2.0.0-alpha1</version>
</dependency>
```

See [jackson-jq/src/test/java/examples/Usage.java](jackson-jq/src/test/java/examples/Usage.java) for an example of using the API.

Command-line interface
----------------------

Use the jackson-jq CLI to test queries quickly.

*jackson-jq is primarily a Java library. The CLI is intended only for debugging and testing, not for production use. Its command-line options may change without notice.*

```sh
$ curl -LO https://repo1.maven.org/maven2/net/thisptr/jackson/jq/v2/jackson-jq-cli/2.0.0-alpha1/jackson-jq-cli-2.0.0-alpha1.jar

$ java -jar jackson-jq-cli-2.0.0-alpha1.jar --help
usage: jackson-jq [OPTIONS...] QUERY
 -c,--compact      compact instead of pretty-printed output
 -h,--help         print this message
    --jq <arg>     specify jq version
 -n,--null-input   use `null` as the single input value
 -r,--raw          output raw strings, not JSON texts

$ java -jar jackson-jq-cli-2.0.0-alpha1.jar '.foo'
{"foo": 42}
42
```

To test a query against a specific jq version, use the `--jq` option:

```sh
$ java -jar jackson-jq-cli-2.0.0-alpha1.jar --jq 1.5 'join("-")'
["1", 2]
jq: error: string ("-") and number (2) cannot be added

$ java -jar jackson-jq-cli-2.0.0-alpha1.jar --jq 1.6 'join("-")' # jq-1.6 can join any values, not only strings
["1", 2]
"1-2"
```

Documentation
-------------

* [Compatibility with jq](docs/compatibility.md)
* [Regex](docs/regex.md)
* [Extension modules](docs/extension-modules.md)
* [Git branching and versioning](docs/development.md)

Contributing
------------

* If jackson-jq produces different results from jq, please [file an issue](https://github.com/eiiches/jackson-jq/issues). Such differences are treated as bugs in jackson-jq.
* Before submitting a nontrivial pull request, please open an issue to discuss the proposed change with the maintainers.
* Documentation improvements, including small grammar and wording corrections, are also welcome.

License
-------

This software is licensed under the Apache License, Version 2.0, with the following exceptions:

* [jackson-jq/src/test/resources](jackson-jq/src/test/resources) contains test cases from [stedolan/jq](https://github.com/stedolan/jq).
* [CoreJqLibrary.java](jackson-jq-core/src/main/java/net/thisptr/jackson/jq/v2/core/internal/CoreJqLibrary.java) and [RegexJqLibrary.java](jackson-jq-regex-impl-joni/src/main/java/net/thisptr/jackson/jq/v2/regex/impl/joni/RegexJqLibrary.java) contain function definitions extracted from [jqlang/jq](https://github.com/jqlang/jq).

See [LICENSE](LICENSE) for details.
