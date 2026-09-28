# Using extension modules

Additional functions that are not part of jq are available through separate extension modules. An extension module can implement `JavaModule` for functions written in Java and ready to call, `JqModule` for jq source that the compiler compiles, or both. In a hybrid module, the jq source can call the Java functions, and the materialized module exports both sets. Add only the dependencies your application needs. `EnvironmentBuilder.withDefaultLoaders(...)` installs jq's builtin functions but no module loader. Register a module directly, or add a `ClassPathModuleLoader` to discover module services. Other loaders, such as the one in [FileSystemModuleTest.java](../../examples/FileSystemModuleTest.java), can be added in the order they should be searched.

A `JqModule` can supply modules and data for imports in its own source through `loadModule()` and
`loadData()`. The compiler asks it first; for an ordinary import, `ModuleNotFoundException` lets
environment registrations and loaders answer instead. A `{search: ...}` import must be answered by
the importing module. Dependencies it supplies are not registered for unrelated queries or modules.
Joni and RE2 use this route for their private Java implementations. Only definitions in each
wrapper's jq source are exported, so its included implementation functions remain internal.

For example, add the UUID extension:

```xml
<dependency>
	<groupId>net.thisptr.jackson.jq.v2</groupId>
	<artifactId>jackson-jq-ext-module-uuid</artifactId>
	<version>2.0.0-alpha2</version>
</dependency>
```

Make it available for query imports by registering its instance:

```java
Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(jsonProvider, version)
        .registerModule(new UuidModule())
        .build();
```

Alternatively, discover all visible module services explicitly:

```java
Environment<JsonNode> env = EnvironmentBuilder.withDefaultLoaders(jsonProvider, version)
        .addModuleLoader(new ClassPathModuleLoader<>(App.class.getClassLoader()))
        .build();
```

Import the module in the jq program:

```jq
import "jackson-jq/uuid" as uuid;

uuid::uuid4
```

Prefer `import` for extension modules: its namespace prevents collisions with functions from other
includes, environment registrations, or builtins.

An extension can instead be included when its functions should intentionally be available without
a namespace:

```jq
include "jackson-jq/uuid";

uuid4
```

An explicit `import` keeps extension functions under its module namespace, while `include` adds
them to that compilation's unqualified function namespace. Conflicting includes are allowed; a
later include replaces an earlier function with the same signature, and a local `def` takes
precedence over both.

An application can apply either directive to every query it compiles, instead of requiring one in
the query text: `EnvironmentBuilder.importModule(module, "uuid")` binds the alias, and
`EnvironmentBuilder.includeModule(module)` exposes the module's functions unqualified. Both take a
`JavaModule`, a `JqModule` or a hybrid directly, so a module the application already holds needs no
loader and no import path. A query's own `import`/`include` shadows what the environment registered.
