# Type annotations

Type checking reads a `def` by analysing its body. That is usually what you want, but it cannot
always say anything useful: a body that dispatches on `type` is opaque to it, and a body whose exact
output the type model cannot represent gets widened until the answer is `ANY`. A definition can state
its signature instead, with a `#jackson-jq:type` comment:

```jq
#jackson-jq:type () => (STRING -> NUMBER)
def n: length;
```

A caller is then checked against the statement rather than against the body, so `n` accepts a string
and answers a number — whatever its body happens to do, and at every call site alike.

jq itself reads the line as an ordinary comment, so a query carrying one still runs there. It just
says nothing about types.

## Writing one

The text after the marker is the notation of
[`TypeScheme`](../jackson-jq-spi/src/main/java/net/thisptr/jackson/jq/v2/spi/type/TypeNotation.java),
which documents the full grammar. A `def` is a function, so what it states is a function type:
`(parameters) => (input -> output)`. A definition taking no parameters writes an empty parameter
list:

```jq
#jackson-jq:type () => (STRING -> NUMBER)
def n: length;
```

Each parameter is stated, in order, as the filter it is run as — its input is what the body feeds it,
and its output is what the body requires back:

```jq
#jackson-jq:type (STRING -> STRING; STRING -> NUMBER) => (STRING -> [*:STRING])
def f(a; b): [a, (b | tostring)];
```

A statement may quantify over its own variables, which is how it says what a definition does to
whatever it is given:

```jq
#jackson-jq:type <T> () => (T -> [T])
def w: [.];
```

## Overloads

Consecutive statements in front of one definition are alternatives. A call is checked against every
one of them: it answers the union of what those accepting it answer, and is rejected when none does.
This is how a body that branches on its argument's type states the shapes it really accepts:

```jq
#jackson-jq:type (STRING -> STRING) => (STRING -> BOOLEAN)
#jackson-jq:type (STRING -> [STRING]) => (STRING -> BOOLEAN)
#jackson-jq:type (STRING -> [STRING, STRING]) => (STRING -> BOOLEAN)
def test($val): ...;
```

## Rules

- A statement stands directly in front of a `def` — a top-level one, a nested one, or one inside a
  module's source. Anywhere else it is a syntax error.
- It states a function type, and as many parameters as the definition takes.
- A comment that opens with `#jackson-jq:type` and then says something unreadable is reported, not
  passed over: a misspelt directive that quietly did nothing would be worse than a rejected one.
- Only type checking reads a statement. A query compiled without it is unaffected by one.
- A statement is taken as given. The body it stands in front of is not analysed at all, so a
  definition that states something its body does not do will mislead its callers rather than fail.
  That also costs something: what would have been an error in the body of an unstated definition —
  found when a call site analyses the body it is about to run — goes unreported once the definition
  states a signature.

## In a module

A module's jq source is the only place a definition it exports can state its signature, and stating
one is what makes the module's surface typed for whoever imports it. `jackson-jq/re2` is written this
way: see
[`Re2RegexModule`](../jackson-jq-ext-module-re2/src/main/java/net/thisptr/jackson/jq/v2/ext/re2/Re2RegexModule.java),
which renders its shapes from the same Java constants its primitives publish, so a stated signature
cannot drift from the primitive behind it.
