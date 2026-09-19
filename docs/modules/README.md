# Using extension modules

Additional functions that are not part of jq are available through separate extension modules. An extension module can implement `JavaModule` for functions written in Java and ready to call, `JqModule` for jq source that the compiler compiles, or both. In a hybrid module, the jq source can call the Java functions, and the materialized module exports both sets. Add only the dependencies your application needs and configure its `Environment` with a `ClassPathModuleLoader` -- `EnvironmentBuilder.withDefaultLoaders(...)` installs one already, over this library's own class loader or over one you name. An environment can hold several loaders, asked in the order they were added: `addModuleLoader(...)` appends one (see [FileSystemModuleTest.java](../../examples/FileSystemModuleTest.java), which searches the classpath and then the filesystem), and `clearModuleLoaders()` drops the default when the search order should be entirely yours.

For example, add the UUID extension:

```xml
<dependency>
	<groupId>net.thisptr.jackson.jq.v2</groupId>
	<artifactId>jackson-jq-ext-module-uuid</artifactId>
	<version>2.0.0-alpha2</version>
</dependency>
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
