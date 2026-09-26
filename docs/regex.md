# Regular expressions

jq's regex functions -- `test`, `match`, `capture`, `scan`, `sub`, `gsub`, `splits` and `split/2` --
are not builtins here. They come from an extension module, and an application chooses which engine
provides them. Nothing installs them implicitly: a query that calls `test` without an engine in scope
fails to compile.

Two engines are available.

| | [`jackson-jq/joni`](modules/joni.md) | [`jackson-jq/re2`](modules/re2.md) |
| --- | --- | --- |
| Artifact | `jackson-jq-ext-module-joni` | `jackson-jq-ext-module-re2` |
| Engine | [Joni](https://github.com/jruby/joni), the Oniguruma port jq itself uses | [RE2/J](https://github.com/google/re2j) |
| Backreferences, look-around | yes | no |
| Worst-case matching time | exponential in the input | linear in the input |
| `\d` and friends | Unicode-aware | ASCII-only |
| Mode flags | `g i m n p s l x` | `g i m p s l` |

Pick joni to match jq's own behaviour, and re2 when patterns come from somewhere you do not control
and a pathological one must not be able to stall the query.

## Enabling one

Add the artifact:

```xml
<dependency>
	<groupId>net.thisptr.jackson.jq.v2</groupId>
	<artifactId>jackson-jq-ext-module-joni</artifactId>
	<version>2.0.0-alpha2</version>
</dependency>
```

Then make it visible in one of three ways. An application that wants the functions available to every
query it compiles registers the module on the `Environment`, which is the same thing an `include`
directive does, without needing one in the query text:

```java
Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(jsonProvider, version)
        .includeModule(JoniRegexModule.getInstance())
        .build();
```

A query can instead reach the functions under a namespace, which cannot collide with anything else
the environment provides:

```jq
import "jackson-jq/joni" as re;

"abc" | re::test("^a")
```

or ask for them unqualified for that one compilation:

```jq
include "jackson-jq/joni";

"abc" | test("^a")
```

`Re2RegexModule.getInstance()` and `"jackson-jq/re2"` are the equivalents for the re2 engine. Both
engines export the same signatures, so a program written against one compiles against the other; only
pattern syntax and the flag set differ. Registering both and leaving both unqualified is possible but
pointless -- the later registration answers the call. See
[extension modules](modules/README.md) for how registration ranks against a query's own `def`s and
directives, and [type annotations](type-annotations.md) for how each module states its signatures so
that type checking answers precisely for them.

The command-line application includes the joni engine by default, so `jackson-jq 'test("a")'` works
with no directive.
