# RE2 module

Maven artifact: `jackson-jq-ext-module-re2`

jq module: `jackson-jq/re2`

This module provides jq's regular-expression functions using [RE2/J](https://github.com/google/re2j), which guarantees linear-time matching but does not support backreferences or look-around assertions. Add the artifact and import it explicitly:

```jq
import "jackson-jq/re2" as re;

"abc" | re::test("^a")
```

The namespaced form above is recommended because it cannot accidentally replace another regex
provider. To intentionally make the RE2 functions available without a namespace, include the
module instead:

```jq
include "jackson-jq/re2";

"abc" | test("^a")
```

The functions have the same signatures and result shapes as the standard jq regex functions. Patterns use RE2 syntax. Mode flags `g`, `i`, `m`, `p`, `s`, and `l` are supported; `n`, `x`, and unknown flags are rejected. As in jq, `m` lets `.` match newlines, `s` keeps anchors at string boundaries, `p` combines `m` and `s`, and `l` selects the longest match. Inline flags inside a pattern use RE2's flag meanings.
