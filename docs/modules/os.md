# OS module

Maven artifact: `jackson-jq-ext-module-os`

jq module: `jackson-jq/os`

```jq
import "jackson-jq/os" as os;
```

## `hostname/0`, `hostname/1`

`os::hostname` returns the local host name reported by Java. Pass an options object with
`fqdn: true` to request the fully qualified domain name:

```jq
os::hostname
os::hostname({fqdn: true})
```

The `fqdn` option defaults to `false`. A failed local host lookup or an unresolved fully
qualified name raises an error. The FQDN form does not return an IP address or an unqualified
name as a fallback.
