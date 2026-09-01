# Using the jackson-jq-regex-impl-joni module

Regex functions (`test`, `match`, `capture`, `scan`, `sub`, `gsub`, `splits`, and `split/2`) are provided by a separate module backed by [Joni](https://github.com/jruby/joni). Add the following dependency to enable them:

```xml
<dependency>
	<groupId>net.thisptr.jackson.jq.v2</groupId>
	<artifactId>jackson-jq-regex-impl-joni</artifactId>
	<version>2.0.0-alpha1</version>
</dependency>
```

The functions are discovered automatically through `ServiceLoader`. The command-line application includes this module by default.
