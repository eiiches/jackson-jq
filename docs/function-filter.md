# Function filter

A `FunctionFilter` removes functions from an `Environment` when it is built. Use it for restricted environments,
where queries should not be able to call everything that is on the classpath, whatever the reason: protecting
resources, keeping a stable and predictable set of functions, or simply limiting what a query language exposes.

A typical case is the [filesystem module](modules/fs.md): it can read and write every path the JVM process can
access. An application that wants queries to read files, but never to modify them, can keep the module and
exclude its write functions:

```java
Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(jsonProvider, Versions.JQ_1_8_2)
		.registerModule(new FsModule())
		.setFunctionFilter(FunctionFilter.denyNames(Set.of("write_text", "write_binary", "write_json")))
		.build();

env.compile("import \"jackson-jq/fs\" as fs; fs::read_text(\"message.txt\")"); // fine
env.compile("import \"jackson-jq/fs\" as fs; \"x\" | fs::write_text(\"message.txt\")");
// fails: Function write_text/1 does not exist
```

The same applies to any function you do not want to expose, whether it is a builtin, comes from an extension
module, or from your own `FunctionLoader`.

## The contract

```java
@FunctionalInterface
public interface FunctionFilter {
	boolean accept(FunctionSignature signature);
}
```

A filter is a predicate on the `(name, arity)` signature of a function. It does not know, and does not
care, whether the function is written in Java or in jq, or where it comes from. A signature the filter
rejects is invisible to the compiler: calling it fails with the usual `Function name/arity does not exist`.

Implementations must be thread-safe and must always give the same answer for the same signature.

## Factories

| Factory | Rejects |
| --- | --- |
| `FunctionFilter.denySignatures(Set<FunctionSignature>)` | exactly the given signatures |
| `FunctionFilter.denyNames(Set<String>)` | every function with a given name, whatever its arity |
| `FunctionFilter.acceptAll()` | nothing; this is the default |

`denySignatures` compares both parts of the signature. For instance, to reject only the two-argument form of
`write_text` and keep `write_text/1`:

```java
FunctionFilter.denySignatures(Set.of(FunctionSignature.of("write_text", 2)));
```

A variadic signature (`FunctionSignature.ofVariadic("f")`, written `f/*`) matches only the variadic function `f`,
not `f/0` or `f/1`. `denyNames` rejects variadic and fixed-arity functions alike.

The filter looks at names only, not at where a function comes from. `denyNames(Set.of("list"))` also removes
any other function called `list`, for example one defined by a different module or loader.

Only deny-lists are provided. For an allow-list, implement the interface:

```java
Set<String> allowed = Set.of("length", "keys", "map", "select");
builder.setFunctionFilter(signature -> allowed.contains(signature.name()));
```

An allow-list must also include every function the allowed ones depend on, see
[Excluding a function can break others](#excluding-a-function-can-break-others).

## What is filtered

The filter is set once with `EnvironmentBuilder.setFunctionFilter` and applies to:

* every `FunctionLoader` in the environment: the default builtins, `ClassPathFunctionLoader` and any custom
  loader. It does not matter whether the filter is set before or after `addFunctionLoader`, nor whether
  `clearFunctionLoaders()` is used. Java functions and jq-defined functions are filtered identically.
* the functions a module exports, however the module reaches the compiler: a query's `import` / `include`
  (resolved through `registerModule` or a `ModuleLoader`), `EnvironmentBuilder.includeModule` and
  `EnvironmentBuilder.importModule`. For a module that mixes Java and jq (a hybrid), both kinds of export are
  filtered.

The filter is inherited by the environments the compiler derives internally, so the jq source of a builtin or of
a module is compiled with the same restrictions as the query.

## What is not filtered

* Functions registered explicitly on the builder with `defineFunction`, `defineJqFunction` and
  `declareFunction`. Those are a deliberate choice of the application and always stay available.
* A module's own definitions, as seen by that module's source: a jq module can still call its own `def`s even
  when it does not export them.
* `modulemeta`. It reports a module's metadata, its dependencies and, from jq 1.7, a `defs` list of signature
  strings. `defs` may name a signature the filter rejects. This is informational only; the function cannot be
  called. The `modulemeta/1` builtin itself is filtered like any other function and can be excluded by name.

## Excluding a function can break others

The filter applies to the functions that other builtins and modules call, too. Many builtins are written in jq
on top of smaller ones: `values` is defined with `numbers`, `strings` and others, for example. Excluding a
function that a builtin (or a module) depends on makes the dependent function fail with
`Function name/arity does not exist`.

Choosing what to exclude is therefore the responsibility of the application. Exclude functions that are leaf
operations you do not want to expose, and test that the queries your application accepts still compile. An
allow-list is the most restrictive option and needs the most care.
