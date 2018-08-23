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
		<version>0.0.9</version>
	</dependency>
</dependencies>
```

### Requirements

 - Java 8 or later

Usage
-----

```java
// First of all, you have to prepare a Scope which is a container of built-in/user-defined functions and variables.
Scope rootScope = Scope.newEmptyScope();

// Scope#loadFunctions(ClassLoader) loads built-in functions (implemented in java) via ServiceLoader mechanism
// and other built-in functions (implemented in jq) from classpath:net/thisptr/jackson/jq/jq.json.
rootScope.loadFunctions(Scope.class.getClassLoader());

// You can also define a custom function. E.g.
rootScope.addFunction("repeat", 1, new Function() {
	@Override
	public void apply(Scope scope, List<Expression> args, JsonNode in, Output output) throws JsonQueryException {
		args.get(0).apply(scope, in, (times) -> {
			output.emit(new TextNode(Strings.repeat(in.asText(), times.asInt())));
		});
	}
});

// After this initial setup, rootScope should not be modified (via Scope#setValue(...),
// Scope#addFunction(...), etc.) so that it can be shared (in a read-only manner) across mutliple threads
// because you want to avoid heavy lifting of loading built-in functions every time which involves
// file system operations and a lot of parsing.

// You can create a child Scope instead of directly modifying the rootScope. This is especially useful when
// you want to use variables or functions that is only local to the specific execution context (such as
// a thread, request, etc).
// Creating a child Scope is a very light-weight operation that just allocates a Scope and sets
// one of its fields to point to the given parent scope. It's completely okay to create a child Scope
// per every apply() invocations if you need to do so.
Scope childScope = Scope.newChildScope(rootScope);

// Scope#setValue(...) sets a custom variable that can be used from jq expressions. This variable is local to the
// childScope and cannot be accessed from the rootScope. The rootScope will not be modified by this call.
childScope.setValue("param", IntNode.valueOf(42));

// JsonQuery#compile(...) parses and compiles a given expression. The resulting JsonQuery instance
// is immutable and thread-safe. It should be reused as possible if you repeatedly use the same expression.
JsonQuery q = JsonQuery.compile("$param * 2");

// You need a JsonNode to use as an input to the JsonQuery. There are many ways you can grab a JsonNode.
// In this example, we just parse a JSON text into a JsonNode.
JsonNode in = MAPPER.readTree("{\"ids\":\"12,15,23\",\"name\":\"jackson\",\"timestamp\":1418785331123}");

// Finally, JsonQuery#apply(...)  executes the query with given input and returns a list of JsonNode.
// The childScope will not be modified by this call because it internally creates a child scope as necessary.
q.apply(childScope, in); // => [84]
```

Using a jackson-jq command line tool
------------------------------------

We provide a CLI tool for testing a jackson-jq query. The tool has to be build with `mvn package`, but alternatively, Homebrew (or Linuxbrew) users can just `brew tap eiiches/jackson-jq && brew install jackson-jq` and `jackson-jq` will be available on $PATH.

```
$ bin/jackson-jq '.foo' <<< '{"foo":10}'
10
```

See `bin/jackson-jq --help` for more information.


Implementation status and current limitations
---------------------------------------------

jackson-jq aims to be a compatible jq implementation. However, not all the features are available; some features are not relevant as being a java library and some features are just yet to be implemented.
The following table (generated from jq-1.5 manual) lists all the features jq provides. I try to keep this list accurate but if you find something wrong, please report an issue.


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
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Objects &#45; `{}`](https://stedolan.github.io/jq/manual/v1.5/#Objects&#45;&#123;&#125;)                                                                                                                                                                                                           | ○          |
| [Builtin operators and functions](https://stedolan.github.io/jq/manual/v1.5/#Builtinoperatorsandfunctions)                                                                                                                                                                                                                         | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Addition &#45; `+`](https://stedolan.github.io/jq/manual/v1.5/#Addition&#45;&#43;)                                                                                                                                                                                                                 | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Subtraction &#45; `-`](https://stedolan.github.io/jq/manual/v1.5/#Subtraction&#45;&#45;)                                                                                                                                                                                                           | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Multiplication, division, modulo &#45; `*`, `/`, and `%`](https://stedolan.github.io/jq/manual/v1.5/#Multiplication&#44;division&#44;modulo&#45;&#42;&#44;&#47;&#44;and&#37;)                                                                                                                      | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`length`](https://stedolan.github.io/jq/manual/v1.5/#length)                                                                                                                                                                                                                                       | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`keys`, `keys_unsorted`](https://stedolan.github.io/jq/manual/v1.5/#keys&#44;keys&#95;unsorted)                                                                                                                                                                                                    | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`has(key)`](https://stedolan.github.io/jq/manual/v1.5/#has&#40;key&#41;)                                                                                                                                                                                                                           | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`in`](https://stedolan.github.io/jq/manual/v1.5/#in)                                                                                                                                                                                                                                               | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`path(path_expression)`](https://stedolan.github.io/jq/manual/v1.5/#path&#40;path&#95;expression&#41;)                                                                                                                                                                                             | ○          |
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
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`indices(s)`](https://stedolan.github.io/jq/manual/v1.5/#indices&#40;s&#41;)                                                                                                                                                                                                                       | ○          |
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
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`env`](https://stedolan.github.io/jq/manual/v1.5/#env)                                                                                                                                                                                                                                             | ×          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`transpose`](https://stedolan.github.io/jq/manual/v1.5/#transpose)                                                                                                                                                                                                                                 | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`bsearch(x)`](https://stedolan.github.io/jq/manual/v1.5/#bsearch&#40;x&#41;)                                                                                                                                                                                                                       | ×          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [String interpolation &#45; `\(foo)`](https://stedolan.github.io/jq/manual/v1.5/#Stringinterpolation&#45;&#92;&#40;foo&#41;)                                                                                                                                                                        | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Convert to&#47;from JSON](https://stedolan.github.io/jq/manual/v1.5/#Convertto&#47;fromJSON)                                                                                                                                                                                                       | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Format strings and escaping](https://stedolan.github.io/jq/manual/v1.5/#Formatstringsandescaping)                                                                                                                                                                                                  | △<sup>※4</sup> |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Dates](https://stedolan.github.io/jq/manual/v1.5/#Dates)                                                                                                                                                                                                                                           | ×          |
| [Conditionals and Comparisons](https://stedolan.github.io/jq/manual/v1.5/#ConditionalsandComparisons)                                                                                                                                                                                                                              | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`==`, `!=`](https://stedolan.github.io/jq/manual/v1.5/#&#61;&#61;&#44;&#33;&#61;)                                                                                                                                                                                                                  | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [if&#45;then&#45;else](https://stedolan.github.io/jq/manual/v1.5/#if&#45;then&#45;else)                                                                                                                                                                                                             | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`>, >=, <=, <`](https://stedolan.github.io/jq/manual/v1.5/#&#62;&#44;&#62;&#61;&#44;&#60;&#61;&#44;&#60;)                                                                                                                                                                                          | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [and&#47;or&#47;not](https://stedolan.github.io/jq/manual/v1.5/#and&#47;or&#47;not)                                                                                                                                                                                                                 | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Alternative operator &#45; `//`](https://stedolan.github.io/jq/manual/v1.5/#Alternativeoperator&#45;&#47;&#47;)                                                                                                                                                                                    | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [try&#45;catch](https://stedolan.github.io/jq/manual/v1.5/#try&#45;catch)                                                                                                                                                                                                                           | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Breaking out of control structures](https://stedolan.github.io/jq/manual/v1.5/#Breakingoutofcontrolstructures)                                                                                                                                                                                     | ○<sup>※2</sup> |
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
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Variables](https://stedolan.github.io/jq/manual/v1.5/#Variables)                                                                                                                                                                                                                                   | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Defining Functions](https://stedolan.github.io/jq/manual/v1.5/#DefiningFunctions)                                                                                                                                                                                                                  | ○<sup>※3</sup> |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Reduce](https://stedolan.github.io/jq/manual/v1.5/#Reduce)                                                                                                                                                                                                                                         | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`limit(n; exp)`](https://stedolan.github.io/jq/manual/v1.5/#limit&#40;n&#59;exp&#41;)                                                                                                                                                                                                              | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`first(expr)`, `last(expr)`, `nth(n; expr)`](https://stedolan.github.io/jq/manual/v1.5/#first&#40;expr&#41;&#44;last&#40;expr&#41;&#44;nth&#40;n&#59;expr&#41;)                                                                                                                                    | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`first`, `last`, `nth(n)`](https://stedolan.github.io/jq/manual/v1.5/#first&#44;last&#44;nth&#40;n&#41;)                                                                                                                                                                                           | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`foreach`](https://stedolan.github.io/jq/manual/v1.5/#foreach)                                                                                                                                                                                                                                     | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Recursion](https://stedolan.github.io/jq/manual/v1.5/#Recursion)                                                                                                                                                                                                                                   | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Generators and iterators](https://stedolan.github.io/jq/manual/v1.5/#Generatorsanditerators)                                                                                                                                                                                                       | ○          |
| [Math](https://stedolan.github.io/jq/manual/v1.5/#Math)                                                                                                                                                                                                                                                                            | ○          |
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
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`ǀ=`](https://stedolan.github.io/jq/manual/v1.5/#&#124;&#61;)                                                                                                                                                                                                                                      | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`+=`, `-=`, `*=`, `/=`, `%=`, `//=`](https://stedolan.github.io/jq/manual/v1.5/#&#43;&#61;&#44;&#45;&#61;&#44;&#42;&#61;&#44;&#47;&#61;&#44;&#37;&#61;&#44;&#47;&#47;&#61;)                                                                                                                        | ○          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [Complex assignments](https://stedolan.github.io/jq/manual/v1.5/#Complexassignments)                                                                                                                                                                                                                | ○          |
| [Modules](https://stedolan.github.io/jq/manual/v1.5/#Modules)                                                                                                                                                                                                                                                                      | ×          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`import RelativePathString as NAME [<metadata>];`](https://stedolan.github.io/jq/manual/v1.5/#importRelativePathStringasNAME&#91;&#60;metadata&#62;&#93;&#59;)                                                                                                                                     | ×          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`include RelativePathString [<metadata>];`](https://stedolan.github.io/jq/manual/v1.5/#includeRelativePathString&#91;&#60;metadata&#62;&#93;&#59;)                                                                                                                                                 | ×          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`import RelativePathString as $NAME [<metadata>];`](https://stedolan.github.io/jq/manual/v1.5/#importRelativePathStringas&#36;NAME&#91;&#60;metadata&#62;&#93;&#59;)                                                                                                                               | ×          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`module <metadata>;`](https://stedolan.github.io/jq/manual/v1.5/#module&#60;metadata&#62;&#59;)                                                                                                                                                                                                    | ×          |
| &nbsp;&nbsp;&nbsp;&nbsp;&bull; [`modulemeta`](https://stedolan.github.io/jq/manual/v1.5/#modulemeta)                                                                                                                                                                                                                               | ×          |

※1) Currently, complex assignments only work when the left-hand side is a simple field access. Won't work if `select/1` or any filters are used in left-hand side. E.g.
 - `jackson-jq '.a[]|.b += 10' <<< '{"a": [{"b": 1}, {"b": 2}]}` does work.
 - `jackson-jq '.a[]|select(.b>1) += 10' <<< '{"a": [{"b": 1}, {"b": 2}]}'` does not work.

※2) Catching a break (`try (break $out) catch .`) always produces a `{__jq: 0}` in jackson-jq, while jq produces `{__jq: n}` where n is the index of the label the `break` statement tries to jump to. E.g.
 - `label $a | label $b | try (break $b) catch .` evaluates to `{"__jq":0}` not `{"__jq":1}`.

※3) When the function with the same name is defined more than once in the same-level scope, jackson-jq uses the last one. E.g.
 - `def f: 1; def g: f; def f: 2; g` evaluates to 2 in jackson-jq, while jq evaluates it to 1.

※4) `@html`, `@uri`, `@sh`, `@base64` are not implemented yet.


Additionally, test cases used in jackson-jq (from the jq unit tests) might be useful to know what kind of queries work or not work.

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
        <version>0.0.9</version>
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
