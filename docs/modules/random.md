# Random module

Maven artifact: `jackson-jq-ext-module-random`

jq module: `jackson-jq/random`

```jq
import "jackson-jq/random" as random;
```

## `random/0`

Returns a pseudorandom number greater than or equal to `0.0` and less than `1.0`.

```console
$ jackson-jq -n 'import "jackson-jq/random" as random; random::random'
0.43292159535427466
```
