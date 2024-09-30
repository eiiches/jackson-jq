jackson-jq
==========

Pure Java [jq](http://stedolan.github.io/jq/) Implementation for Jackson JSON Processor

[![GitHub Actions](https://github.com/eiiches/jackson-jq/workflows/test/badge.svg)](https://github.com/eiiches/jackson-jq/actions)



Usage
-----

First, you need Java 8 or later.

If you use Maven, add the following snippet to the `<dependencies>` section of your POM. For instructions for other build tools (Gradle, etc.), visit [jackson-jq](https://search.maven.org/artifact/net.thisptr/jackson-jq/1.0.0/jar) on search.maven.org.

```xml
<dependency>
	<groupId>net.thisptr</groupId>
	<artifactId>jackson-jq</artifactId>
	<version>1.0.0</version>
</dependency>
```

See [jackson-jq/src/test/java/examples/Usage.java](jackson-jq/src/test/java/examples/Usage.java) for the API usage.

Using a jackson-jq command line tool
------------------------------------

To test a query quickly, we provide jackson-jq CLI.

*Please note that jackson-jq is a Java library and the CLI is provided solely for debugging/testing purpose (and not for production). The command-line options might change without notice.*

```sh
$ curl -LO https://repo1.maven.org/maven2/net/thisptr/jackson-jq-cli/1.0.0/jackson-jq-cli-1.0.0.jar

$ java -jar jackson-jq-cli-1.0.0.jar --help
usage: jackson-jq [OPTIONS...] QUERY
 -c,--compact      compact instead of pretty-printed output
 -h,--help         print this message
    --jq <arg>     specify jq version
 -n,--null-input   use `null` as the single input value
 -r,--raw          output raw strings, not JSON texts

$ java -jar jackson-jq-cli-1.0.0.jar '.foo'
{"foo": 42}
42
```

To test a query with a specific jq version,

```sh
$ java -jar jackson-jq-cli-1.0.0.jar --jq 1.5 'join("-")'
["1", 2]
jq: error: string ("-") and number (2) cannot be added

$ java -jar jackson-jq-cli-1.0.0.jar --jq 1.6 'join("-")' # jq-1.6 can join any values, not only strings
["1", 2]
"1-2"
```

Homebrew (or Linuxbrew) users can alternatively run `brew tap eiiches/jackson-jq && brew install jackson-jq` to install the CLI. `jackson-jq` will be available on your $PATH.

Branches and versioning
-----------------------

There are currently two development branches.

* `develop/1.x`: This branch (you are viewing), which is currently under development for the future 1.0 release. You can find preview releases at [Releases](https://github.com/eiiches/jackson-jq/releases) page (tags: `1.0.0-preview.yyyyMMdd`). Although the API is not stable yet, I recommend new users to use these releases insetad of 0.x versions, because these releases have more features, better compatibility, and better performance.
* `develop/0.x`: The development branch for 0.x versions. Features that need breaking API changes will no longer be added. Go to [Releases](https://github.com/eiiches/jackson-jq/releases) and find the latest 0.x.y version.

PRs can be sent to any of the develop/\* branches. The patch will be ported to the other branch(es) if necessary.

We use [Semantic Versioning 2.0.0](https://semver.org/) for Java API versioning, 1.0.0 onwards. A jq behavior fix (even if it may possibly affect users) will not be considered a major change if the fix is to make the bahavior compatible with ./jq; these kind of incompatible changes are documented in the release note.

If you get different results between ./jq and jackson-jq, please [file an issue](https://github.com/eiiches/jackson-jq/issues). That is a bug on jackson-jq side.

Implementation Status
---------------------

jackson-jq aims to be a compatible jq implementation. However, not every feature is available; some are intentionally omitted because thay are not relevant as a Java library; some may be incomplete, have bugs or are yet to be implemented.

### List of Features

<details>
<summary>Click to see the list</summary>
<br />

This table illustrates which features (picked from jq-1.5 manual) are supported and which are not in jackson-jq. We try to keep this list accurate and up to date. If you find something is missing or wrong, please file an issue.
  
| Language Features / Functions                                                                                                                                                                                                                                                                                                      | jackson-jq |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|
| [Basic filters](https://stedolan.github.io/jq/manual/v1.5/#Basicfilters)                                                                                                                                                                                                                                                           | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`.`](https://stedolan.github.io/jq/manual/v1.5/#&#46;)                                                                                                                                                                                                                                             | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`.foo`, `.foo.bar`](https://stedolan.github.io/jq/manual/v1.5/#&#46;foo&#44;&#46;foo&#46;bar)                                                                                                                                                                                                      | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`.foo?`](https://stedolan.github.io/jq/manual/v1.5/#&#46;foo&#63;)                                                                                                                                                                                                                                 | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`.[<string>]`, `.[2]`, `.[10:15]`](https://stedolan.github.io/jq/manual/v1.5/#&#46;&#91;&#60;string&#62;&#93;&#44;&#46;&#91;2&#93;&#44;&#46;&#91;10&#58;15&#93;)                                                                                                                                   | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`.[]`](https://stedolan.github.io/jq/manual/v1.5/#&#46;&#91;&#93;)                                                                                                                                                                                                                                 | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`.[]?`](https://stedolan.github.io/jq/manual/v1.5/#&#46;&#91;&#93;&#63;)                                                                                                                                                                                                                           | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`,`](https://stedolan.github.io/jq/manual/v1.5/#&#44;)                                                                                                                                                                                                                                             | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`ǀ`](https://stedolan.github.io/jq/manual/v1.5/#&#124;)                                                                                                                                                                                                                                            | ○          |
| [Types and Values](https://stedolan.github.io/jq/manual/v1.5/#TypesandValues)                                                                                                                                                                                                                                                      | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Array construction &#45; `[]`](https://stedolan.github.io/jq/manual/v1.5/#Arrayconstruction&#45;&#91;&#93;)                                                                                                                                                                                        | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Objects &#45; `{}`](https://stedolan.github.io/jq/manual/v1.5/#Objects&#45;&#123;&#125;)                                                                                                                                                                                                           | ○<sup>*4</sup> |
| [Builtin operators and functions](https://stedolan.github.io/jq/manual/v1.5/#Builtinoperatorsandfunctions)                                                                                                                                                                                                                         | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Addition &#45; `+`](https://stedolan.github.io/jq/manual/v1.5/#Addition&#45;&#43;)                                                                                                                                                                                                                 | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Subtraction &#45; `-`](https://stedolan.github.io/jq/manual/v1.5/#Subtraction&#45;&#45;)                                                                                                                                                                                                           | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Multiplication, division, modulo &#45; `*`, `/`, and `%`](https://stedolan.github.io/jq/manual/v1.5/#Multiplication&#44;division&#44;modulo&#45;&#42;&#44;&#47;&#44;and&#37;)                                                                                                                      | ○<sup>*5</sup> |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`length`](https://stedolan.github.io/jq/manual/v1.5/#length)                                                                                                                                                                                                                                       | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`keys`, `keys_unsorted`](https://stedolan.github.io/jq/manual/v1.5/#keys&#44;keys&#95;unsorted)                                                                                                                                                                                                    | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`has(key)`](https://stedolan.github.io/jq/manual/v1.5/#has&#40;key&#41;)                                                                                                                                                                                                                           | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`in`](https://stedolan.github.io/jq/manual/v1.5/#in)                                                                                                                                                                                                                                               | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`path(path_expression)`](https://stedolan.github.io/jq/manual/v1.5/#path&#40;path&#95;expression&#41;)                                                                                                                                                                                             | ○<sup>*7</sup> |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`del(path_expression)`](https://stedolan.github.io/jq/manual/v1.5/#del&#40;path&#95;expression&#41;)                                                                                                                                                                                               | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`to_entries`, `from_entries`, `with_entries`](https://stedolan.github.io/jq/manual/v1.5/#to&#95;entries&#44;from&#95;entries&#44;with&#95;entries)                                                                                                                                                 | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`select(boolean_expression)`](https://stedolan.github.io/jq/manual/v1.5/#select&#40;boolean&#95;expression&#41;)                                                                                                                                                                                   | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`arrays`, `objects`, `iterables`, `booleans`, `numbers`, `normals`, `finites`, `strings`, `nulls`, `values`, `scalars`](https://stedolan.github.io/jq/manual/v1.5/#arrays&#44;objects&#44;iterables&#44;booleans&#44;numbers&#44;normals&#44;finites&#44;strings&#44;nulls&#44;values&#44;scalars) | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`empty`](https://stedolan.github.io/jq/manual/v1.5/#empty)                                                                                                                                                                                                                                         | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`error(message)`](https://stedolan.github.io/jq/manual/v1.5/#error&#40;message&#41;)                                                                                                                                                                                                               | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`$__loc__`](https://stedolan.github.io/jq/manual/v1.5/#&#36;&#95;&#95;loc&#95;&#95;)                                                                                                                                                                                                               | ×          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`map(x)`, `map_values(x)`](https://stedolan.github.io/jq/manual/v1.5/#map&#40;x&#41;&#44;map&#95;values&#40;x&#41;)                                                                                                                                                                                | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`paths`, `paths(node_filter)`, `leaf_paths`](https://stedolan.github.io/jq/manual/v1.5/#paths&#44;paths&#40;node&#95;filter&#41;&#44;leaf&#95;paths)                                                                                                                                               | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`add`](https://stedolan.github.io/jq/manual/v1.5/#add)                                                                                                                                                                                                                                             | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`any`, `any(condition)`, `any(generator; condition)`](https://stedolan.github.io/jq/manual/v1.5/#any&#44;any&#40;condition&#41;&#44;any&#40;generator&#59;condition&#41;)                                                                                                                          | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`all`, `all(condition)`, `all(generator; condition)`](https://stedolan.github.io/jq/manual/v1.5/#all&#44;all&#40;condition&#41;&#44;all&#40;generator&#59;condition&#41;)                                                                                                                          | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`flatten`, `flatten(depth)`](https://stedolan.github.io/jq/manual/v1.5/#flatten&#44;flatten&#40;depth&#41;)                                                                                                                                                                                        | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`range(upto)`, `range(from;upto)` `range(from;upto;by)`](https://stedolan.github.io/jq/manual/v1.5/#range&#40;upto&#41;&#44;range&#40;from&#59;upto&#41;range&#40;from&#59;upto&#59;by&#41;)                                                                                                       | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`floor`](https://stedolan.github.io/jq/manual/v1.5/#floor)                                                                                                                                                                                                                                         | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`sqrt`](https://stedolan.github.io/jq/manual/v1.5/#sqrt)                                                                                                                                                                                                                                           | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`tonumber`](https://stedolan.github.io/jq/manual/v1.5/#tonumber)                                                                                                                                                                                                                                   | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`tostring`](https://stedolan.github.io/jq/manual/v1.5/#tostring)                                                                                                                                                                                                                                   | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`type`](https://stedolan.github.io/jq/manual/v1.5/#type)                                                                                                                                                                                                                                           | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`infinite`, `nan`, `isinfinite`, `isnan`, `isfinite`, `isnormal`](https://stedolan.github.io/jq/manual/v1.5/#infinite&#44;nan&#44;isinfinite&#44;isnan&#44;isfinite&#44;isnormal)                                                                                                                  | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`sort, sort_by(path_expression)`](https://stedolan.github.io/jq/manual/v1.5/#sort&#44;sort&#95;by&#40;path&#95;expression&#41;)                                                                                                                                                                    | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`group_by(path_expression)`](https://stedolan.github.io/jq/manual/v1.5/#group&#95;by&#40;path&#95;expression&#41;)                                                                                                                                                                                 | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`min`, `max`, `min_by(path_exp)`, `max_by(path_exp)`](https://stedolan.github.io/jq/manual/v1.5/#min&#44;max&#44;min&#95;by&#40;path&#95;exp&#41;&#44;max&#95;by&#40;path&#95;exp&#41;)                                                                                                            | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`unique`, `unique_by(path_exp)`](https://stedolan.github.io/jq/manual/v1.5/#unique&#44;unique&#95;by&#40;path&#95;exp&#41;)                                                                                                                                                                        | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`reverse`](https://stedolan.github.io/jq/manual/v1.5/#reverse)                                                                                                                                                                                                                                     | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`contains(element)`](https://stedolan.github.io/jq/manual/v1.5/#contains&#40;element&#41;)                                                                                                                                                                                                         | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`indices(s)`](https://stedolan.github.io/jq/manual/v1.5/#indices&#40;s&#41;)                                                                                                                                                                                                                       | ○<sup>*9</sup> |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`index(s)`, `rindex(s)`](https://stedolan.github.io/jq/manual/v1.5/#index&#40;s&#41;&#44;rindex&#40;s&#41;)                                                                                                                                                                                        | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`inside`](https://stedolan.github.io/jq/manual/v1.5/#inside)                                                                                                                                                                                                                                       | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`startswith(str)`](https://stedolan.github.io/jq/manual/v1.5/#startswith&#40;str&#41;)                                                                                                                                                                                                             | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`endswith(str)`](https://stedolan.github.io/jq/manual/v1.5/#endswith&#40;str&#41;)                                                                                                                                                                                                                 | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`combinations`, `combinations(n)`](https://stedolan.github.io/jq/manual/v1.5/#combinations&#44;combinations&#40;n&#41;)                                                                                                                                                                            | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`ltrimstr(str)`](https://stedolan.github.io/jq/manual/v1.5/#ltrimstr&#40;str&#41;)                                                                                                                                                                                                                 | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`rtrimstr(str)`](https://stedolan.github.io/jq/manual/v1.5/#rtrimstr&#40;str&#41;)                                                                                                                                                                                                                 | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`explode`](https://stedolan.github.io/jq/manual/v1.5/#explode)                                                                                                                                                                                                                                     | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`implode`](https://stedolan.github.io/jq/manual/v1.5/#implode)                                                                                                                                                                                                                                     | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`split`](https://stedolan.github.io/jq/manual/v1.5/#split)                                                                                                                                                                                                                                         | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`join(str)`](https://stedolan.github.io/jq/manual/v1.5/#join&#40;str&#41;)                                                                                                                                                                                                                         | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`ascii_downcase`, `ascii_upcase`](https://stedolan.github.io/jq/manual/v1.5/#ascii&#95;downcase&#44;ascii&#95;upcase)                                                                                                                                                                              | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`while(cond; update)`](https://stedolan.github.io/jq/manual/v1.5/#while&#40;cond&#59;update&#41;)                                                                                                                                                                                                  | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`until(cond; next)`](https://stedolan.github.io/jq/manual/v1.5/#until&#40;cond&#59;next&#41;)                                                                                                                                                                                                      | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`recurse(f)`, `recurse`, `recurse(f; condition)`, `recurse_down`](https://stedolan.github.io/jq/manual/v1.5/#recurse&#40;f&#41;&#44;recurse&#44;recurse&#40;f&#59;condition&#41;&#44;recurse&#95;down)                                                                                             | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`..`](https://stedolan.github.io/jq/manual/v1.5/#&#46;&#46;)                                                                                                                                                                                                                                       | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`env`](https://stedolan.github.io/jq/manual/v1.5/#env)                                                                                                                                                                                                                                             | ○<sup>*6</sup> |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`transpose`](https://stedolan.github.io/jq/manual/v1.5/#transpose)                                                                                                                                                                                                                                 | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`bsearch(x)`](https://stedolan.github.io/jq/manual/v1.5/#bsearch&#40;x&#41;)                                                                                                                                                                                                                       | ×          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [String interpolation &#45; `\(foo)`](https://stedolan.github.io/jq/manual/v1.5/#Stringinterpolation&#45;&#92;&#40;foo&#41;)                                                                                                                                                                        | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Convert to&#47;from JSON](https://stedolan.github.io/jq/manual/v1.5/#Convertto&#47;fromJSON)                                                                                                                                                                                                       | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Format strings and escaping](https://stedolan.github.io/jq/manual/v1.5/#Formatstringsandescaping)                                                                                                                                                                                                  | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Dates](https://stedolan.github.io/jq/manual/v1.5/#Dates)                                                                                                                                                                                                                                           | ×          |
| [Conditionals and Comparisons](https://stedolan.github.io/jq/manual/v1.5/#ConditionalsandComparisons)                                                                                                                                                                                                                              | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`==`, `!=`](https://stedolan.github.io/jq/manual/v1.5/#&#61;&#61;&#44;&#33;&#61;)                                                                                                                                                                                                                  | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [if&#45;then&#45;else](https://stedolan.github.io/jq/manual/v1.5/#if&#45;then&#45;else)                                                                                                                                                                                                             | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`>, >=, <=, <`](https://stedolan.github.io/jq/manual/v1.5/#&#62;&#44;&#62;&#61;&#44;&#60;&#61;&#44;&#60;)                                                                                                                                                                                          | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [and&#47;or&#47;not](https://stedolan.github.io/jq/manual/v1.5/#and&#47;or&#47;not)                                                                                                                                                                                                                 | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Alternative operator &#45; `//`](https://stedolan.github.io/jq/manual/v1.5/#Alternativeoperator&#45;&#47;&#47;)                                                                                                                                                                                    | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [try&#45;catch](https://stedolan.github.io/jq/manual/v1.5/#try&#45;catch)                                                                                                                                                                                                                           | ○<sup>*1</sup> |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Breaking out of control structures](https://stedolan.github.io/jq/manual/v1.5/#Breakingoutofcontrolstructures)                                                                                                                                                                                     | ○<sup>*2</sup> |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`?` operator](https://stedolan.github.io/jq/manual/v1.5/#&#63;operator)                                                                                                                                                                                                                            | ○          |
| [Regular expressions &#40;PCRE&#41;](https://stedolan.github.io/jq/manual/v1.5/#Regularexpressions&#40;PCRE&#41;)                                                                                                                                                                                                                  | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`test(val)`, `test(regex; flags)`](https://stedolan.github.io/jq/manual/v1.5/#test&#40;val&#41;&#44;test&#40;regex&#59;flags&#41;)                                                                                                                                                                 | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`match(val)`, `match(regex; flags)`](https://stedolan.github.io/jq/manual/v1.5/#match&#40;val&#41;&#44;match&#40;regex&#59;flags&#41;)                                                                                                                                                             | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`capture(val)`, `capture(regex; flags)`](https://stedolan.github.io/jq/manual/v1.5/#capture&#40;val&#41;&#44;capture&#40;regex&#59;flags&#41;)                                                                                                                                                     | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`scan(regex)`, `scan(regex; flags)`](https://stedolan.github.io/jq/manual/v1.5/#scan&#40;regex&#41;&#44;scan&#40;regex&#59;flags&#41;)                                                                                                                                                             | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`split(regex; flags)`](https://stedolan.github.io/jq/manual/v1.5/#split&#40;regex&#59;flags&#41;)                                                                                                                                                                                                  | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`splits(regex)`, `splits(regex; flags)`](https://stedolan.github.io/jq/manual/v1.5/#splits&#40;regex&#41;&#44;splits&#40;regex&#59;flags&#41;)                                                                                                                                                     | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`sub(regex; tostring)` `sub(regex; string; flags)`](https://stedolan.github.io/jq/manual/v1.5/#sub&#40;regex&#59;tostring&#41;sub&#40;regex&#59;string&#59;flags&#41;)                                                                                                                             | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`gsub(regex; string)`, `gsub(regex; string; flags)`](https://stedolan.github.io/jq/manual/v1.5/#gsub&#40;regex&#59;string&#41;&#44;gsub&#40;regex&#59;string&#59;flags&#41;)                                                                                                                       | ○          |
| [Advanced features](https://stedolan.github.io/jq/manual/v1.5/#Advancedfeatures)                                                                                                                                                                                                                                                   | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Variables](https://stedolan.github.io/jq/manual/v1.5/#Variables)                                                                                                                                                                                                                                   | ○<sup>*11</sup> |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Destructuring Alternative Operator: ?//](https://stedolan.github.io/jq/manual/v1.6/#DestructuringAlternativeOperator:?//)                                                                                                                                                                          | ✕ (#44)    |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Defining Functions](https://stedolan.github.io/jq/manual/v1.5/#DefiningFunctions)                                                                                                                                                                                                                  | ○<sup>*3</sup> |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Reduce](https://stedolan.github.io/jq/manual/v1.5/#Reduce)                                                                                                                                                                                                                                         | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`limit(n; exp)`](https://stedolan.github.io/jq/manual/v1.5/#limit&#40;n&#59;exp&#41;)                                                                                                                                                                                                              | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`first(expr)`, `last(expr)`, `nth(n; expr)`](https://stedolan.github.io/jq/manual/v1.5/#first&#40;expr&#41;&#44;last&#40;expr&#41;&#44;nth&#40;n&#59;expr&#41;)                                                                                                                                    | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`first`, `last`, `nth(n)`](https://stedolan.github.io/jq/manual/v1.5/#first&#44;last&#44;nth&#40;n&#41;)                                                                                                                                                                                           | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`foreach`](https://stedolan.github.io/jq/manual/v1.5/#foreach)                                                                                                                                                                                                                                     | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Recursion](https://stedolan.github.io/jq/manual/v1.5/#Recursion)                                                                                                                                                                                                                                   | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Generators and iterators](https://stedolan.github.io/jq/manual/v1.5/#Generatorsanditerators)                                                                                                                                                                                                       | ○          |
| [Math](https://stedolan.github.io/jq/manual/v1.5/#Math)                                                                                                                                                                                                                                                                            | △          |
| [I&#47;O](https://stedolan.github.io/jq/manual/v1.5/#I&#47;O)                                                                                                                                                                                                                                                                      | N/A        |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`input`](https://stedolan.github.io/jq/manual/v1.5/#input)                                                                                                                                                                                                                                         | N/A        |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`inputs`](https://stedolan.github.io/jq/manual/v1.5/#inputs)                                                                                                                                                                                                                                       | N/A        |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`debug`](https://stedolan.github.io/jq/manual/v1.5/#debug)                                                                                                                                                                                                                                         | N/A        |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`input_filename`](https://stedolan.github.io/jq/manual/v1.5/#input&#95;filename)                                                                                                                                                                                                                   | N/A        |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`input_line_number`](https://stedolan.github.io/jq/manual/v1.5/#input&#95;line&#95;number)                                                                                                                                                                                                         | N/A        |
| [Streaming](https://stedolan.github.io/jq/manual/v1.5/#Streaming)                                                                                                                                                                                                                                                                  | N/A        |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`truncate_stream(stream_expression)`](https://stedolan.github.io/jq/manual/v1.5/#truncate&#95;stream&#40;stream&#95;expression&#41;)                                                                                                                                                               | N/A        |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`fromstream(stream_expression)`](https://stedolan.github.io/jq/manual/v1.5/#fromstream&#40;stream&#95;expression&#41;)                                                                                                                                                                             | N/A        |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`tostream`](https://stedolan.github.io/jq/manual/v1.5/#tostream)                                                                                                                                                                                                                                   | N/A        |
| [Assignment](https://stedolan.github.io/jq/manual/v1.5/#Assignment)                                                                                                                                                                                                                                                                | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`=`](https://stedolan.github.io/jq/manual/v1.5/#&#61;)                                                                                                                                                                                                                                             | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`ǀ=`](https://stedolan.github.io/jq/manual/v1.5/#&#124;&#61;)                                                                                                                                                                                                                                      | ○<sup>*8</sup> |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`+=`, `-=`, `*=`, `/=`, `%=`, `//=`](https://stedolan.github.io/jq/manual/v1.5/#&#43;&#61;&#44;&#45;&#61;&#44;&#42;&#61;&#44;&#47;&#61;&#44;&#37;&#61;&#44;&#47;&#47;&#61;)                                                                                                                        | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Complex assignments](https://stedolan.github.io/jq/manual/v1.5/#Complexassignments)                                                                                                                                                                                                                | ○          |
| [Modules](https://stedolan.github.io/jq/manual/v1.5/#Modules)                                                                                                                                                                                                                                                                      | △          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`import RelativePathString as NAME [<metadata>];`](https://stedolan.github.io/jq/manual/v1.5/#importRelativePathStringasNAME&#91;&#60;metadata&#62;&#93;&#59;)                                                                                                                                     | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`include RelativePathString [<metadata>];`](https://stedolan.github.io/jq/manual/v1.5/#includeRelativePathString&#91;&#60;metadata&#62;&#93;&#59;)                                                                                                                                                 | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`import RelativePathString as $NAME [<metadata>];`](https://stedolan.github.io/jq/manual/v1.5/#importRelativePathStringas&#36;NAME&#91;&#60;metadata&#62;&#93;&#59;)                                                                                                                               | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`module <metadata>;`](https://stedolan.github.io/jq/manual/v1.5/#module&#60;metadata&#62;&#59;)                                                                                                                                                                                                    | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`modulemeta`](https://stedolan.github.io/jq/manual/v1.5/#modulemeta)                                                                                                                                                                                                                               | ×          |

</details>

### Known Compatibility Issues / Differences

#### Category: BUG

<details>
<summary>(*11) Operator Precedences in <code>1 + 3 as $a | ($a * 2)</code></summary>
  
##### Description

The presence of `as $a` affects precedence of `|` and other operators in jq:

```console
$ jq -n '1 + 3 | (. * 2)' # interpreted as (1 + 3) | (. * 2)
8
$ jq -n '1 + 3 as $a | ($a * 2)' # interpreted as 1 + (3 as $a | ($a * 2))
7
```

whereas jackson-jq consistently interprets them as `(1 + 3)` whether `as $a` is used or not:

```console
$ java -jar jackson-jq-cli-1.0.0.jar -n '1 + 3 | (. * 2)' # interpreted as (1 + 3) | (. * 2)
8
$ java -jar jackson-jq-cli-1.0.0.jar -n '1 + 3 as $a | ($a * 2)' # interpreted as (1 + 3) as $a | ($a * 2)
8
```

##### Examples

```console
$ jq -n '1 + 3 as $a | ($a * 2)' # interpreted as 1 + (3 as $a | ($a * 2))
7
$ java -jar jackson-jq-cli-1.0.0.jar -n '1 + 3 as $a | ($a * 2)' # interpreted as (1 + 3) as $a | ($a * 2)
8
```

##### Workaround

Use explicit parentheses.

##### Links

* [jackson-jq#72](https://github.com/eiiches/jackson-jq/issues/72)

</details>


<details>
<summary>(*3) Multiple functions with the same name in the same scope</summary>

##### Description

If the function with the same is defined more than once at the same scope, jackson-jq uses the last one.

##### Examples

```console
$ jq -n 'def f: 1; def g: f; def f: 2; g'
1
$ java -jar jackson-jq-cli-1.0.0.jar -n 'def f: 1; def g: f; def f: 2; g'
2
```

##### Workaround

Avoid using the duplicate function name.

```console
$ java -jar jackson-jq-cli-1.0.0.jar -n 'def f1: 1; def g: f1; def f2: 2; g'
1
```

</details>


#### Category: BY DESIGN

<details>
<summary>(*1) Error Message Wording</summary>

##### Description

Error messages differ between jq and jackson-jq and they also tend to change between versions.

##### Workaround

None. This is by design and will not be fixed.

</details>

<details>
<summary>(*6) <code>env/0</code> is not available by default.</summary>

##### Description

`env/0` is not available by default for security reasons and must be added manually to the scope.

##### Workaround

Add `env/0` manually into the scope:

```java
SCOPE.addFunction("env", 0, new EnvFunction())
```

</details>

<details>
<summary>(*4) Field Ordering in JSON Object</summary>

##### Description
  
The order of the keys in JSON is not preserved. It was a design decision but we are slowly trying to fix this in order to improve the compatibility with jq.

##### Workaround

None. Use array if the order is important.

</details>
  
<details>
<summary>(*5) <code>0 / 0</code> is an error in jackson-jq.</summary>

##### Description

jq evaluates `0 / 0`, if hard-coded, to NaN without any errors, whereas `0 | 0 / .` results in a zero-division error. jackson-jq always raises an error in both cases.

##### Examples

```console
$ jq -n '0 / 0'
null
$ jq -n '10 / 0'
jq: error: Division by zero? at <top-level>, line 1:
10 / 0
jq: 1 compile error
$ jq '. / 0' <<< 0
jq: error (at <stdin>:1): number (0) and number (0) cannot be divided because the divisor is zero
$ java -jar jackson-jq-cli-1.0.0.jar -n '0 / 0'
jq: error: number (0) and number (0) cannot be divided because the divisor is zero
```

##### Workaround

If you need NaN, use `nan` instead of `0 / 0`.
  
</details>

<details>
<summary>(*8) <code>... |= empty</code> is an error in jackson-jq.</summary>

##### Description

`.foo |= empty` always throws an error in jackson-jq instead of producing an unexpected result. jq-1.5 and jq-1.6 respectively produces a different and incorrect result for `[1,2,3] | ((.[] | select(. > 1)) |= empty)`. [jq#897](https://github.com/stedolan/jq/issues/897) says "empty in the RHS is undefined". You can still use `_modify/2` directly if you really want to emulate the exact jq-1.5 or jq-1.6 behavior.

##### Examples

```console
$ jq-1.6 -n '[1,2,3] | ((.[] | select(. > 1)) |= empty)'
[
  1,
  3
]
$ jq-1.5 -n '[1,2,3] | ((.[] | select(. > 1)) |= empty)'
null
$ jq-1.2 -n '[1,2,3] | ((.[] | select(. > 1)) |= empty)'
[
  1,
  2,
  3
]
$ java -jar jackson-jq-cli-1.0.0.jar --jq 1.6 -n '[1,2,3] | ((.[] | select(. > 1)) |= empty)'
jq: error: `|= empty` is undefined. See https://github.com/stedolan/jq/issues/897
$ java -jar jackson-jq-cli-1.0.0.jar --jq 1.5 -n '[1,2,3] | ((.[] | select(. > 1)) |= empty)'
jq: error: `|= empty` is undefined. See https://github.com/stedolan/jq/issues/897
```

##### Workaround

You can use `_modify/2` if you really want to the original behavior.

```console
$ java -jar jackson-jq-cli-1.0.0.jar --jq 1.6 -n '[1,2,3] | _modify((.[] | select(. > 1)); empty)'
[ 1, 3 ]
$ java -jar jackson-jq-cli-1.0.0.jar --jq 1.5 -n '[1,2,3] | _modify((.[] | select(. > 1)); empty)'
null
```

</details>
  
<details>
<summary>(*7) Variables don't carry path information even in jq 1.5 compat mode.</summary>

##### Description
  
`path(.foo as $a | $a)` always throws an error as $variables in jackson-jq do not carry path information like jq-1.5 accidentally? did. The behavior is fixed in jq-1.6 whose [documentation](https://stedolan.github.io/jq/manual/v1.6/#Assignment) explicitly states them as "not a valid or useful path expression". So, I dicided not to implement it even in jq-1.5 compatible mode.

##### Examples
  
jq 1.5

```console
$ jq-1.5 -c 'path(.foo as $a | $a)' <<< '{"foo": 1}'
["foo"]
$ java -jar jackson-jq-cli-1.0.0.jar --jq 1.5 -c 'path(.foo as $a | $a)' <<< '{"foo": 1}'
jq: error: Invalid path expression with result 1
```

jq 1.6

```console
$ jq-1.6 -c 'path(.foo as $a | $a)' <<< '{"foo": 1}'
jq: error (at <stdin>:1): Invalid path expression with result 1
$ java -jar jackson-jq-cli-1.0.0.jar --jq 1.6 -c 'path(.foo as $a | $a)' <<< '{"foo": 1}'
jq: error: Invalid path expression with result 1
```

##### Workaround

None

</details>

<details>
<summary>(*2) <code>try (break $label) catch .</code> always produces <code>{"__jq": 0}</code>.</summary>

##### Description

<code>try (break $label) catch .</code> always produces <code>{"__jq": 0}</code> in jackson-jq, while `__jq` should contain the index of the label the `break` statement jumps to.

##### Examples

```console
$ jq -n 'label $a | label $b | try (break $b) catch .'
{
  "__jq": 1
}
$ java -jar jackson-jq-cli-1.0.0.jar -n 'label $a | label $b | try (break $b) catch .'
{
  "__jq" : 0
}
```

##### Workaround

None. Tell us your use case if you need this feature.

</details>

#### Category: BUGFIX

<details>
<summary>(*9) <code>indices("")</code> returns <code>[]</code> (empty array) in jackson-jq.</summary>

##### Description

`indices/1` implementation in jq-1.5 and jq-1.6 had a bug that caused `indices("")` to end up in infinite loop which eventually leads to OOM. The bug is [fixed](https://github.com/stedolan/jq/commit/2660b04a731568c54eb4b91fe811d81cbbf3470b) and likely to be in jq-1.7 (not released yet). jackson-jq chose not to simulate this bug.

##### Examples
  
```console
$ jq-1.5 -n '"x" | indices("")' # stuck in infinite loop
^C
$ jq-1.6 -n '"x" | indices("")' # stuck in infinite loop
^C
$ jq-1.6-83-gb52fc10 -n '"x" | indices("")'
[]
$ java -jar jackson-jq-cli-1.0.0.jar -n '"x" | indices("")'
[ ]
```

</details>

Using jackson-jq/extras module
------------------------------

The `jackson-jq/extras` module is a jq module that provides some useful functions that do not exist in jq.

To use this module, you need to add the following Maven dependency and set `BuiltinModuleLoader` (see [jackson-jq/src/test/java/examples/Usage.java](jackson-jq/src/test/java/examples/Usage.java)) to the scope.

```xml
<dependency>
	<groupId>net.thisptr</groupId>
	<artifactId>jackson-jq-extra</artifactId>
	<version>1.0.0</version>
</dependency>
```

Now, you can import the module in jq:

```jq
import "jackson-jq/extras" as extras;

extras::uuid4
```

For a historical reason, adding the Maven dependency also makes the functions directly available to jq. This behavior is deprecated and will be removed at some point in the future.

<details>
<summary>List of Functions</summary>

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

</details>

Contributing
------------

* If you are planning to send a PR and the change is not small, please open an issue and discuss it with the authors first.
* Other than bug reports or patches, documentation improvements (including small grammatical or wording corrections) would be greatly appreciated.

License
-------

This software is licensed under Apache Software License, Version 2.0, with some exceptions:

 - [jackson-jq/src/test/resources](jackson-jq/src/test/resources) contains test cases from [stedolan/jq](https://github.com/stedolan/jq).
 - [jackson-jq/src/main/resources/net/thisptr/jackson/jq/jq.json](jackson-jq/src/main/resources/net/thisptr/jackson/jq/jq.json) contains function definitions extracted from [stedolan/jq](https://github.com/stedolan/jq).

See [COPYING](COPYING) for details.
