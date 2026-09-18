# Filesystem module

Maven artifact: `jackson-jq-ext-module-fs`

jq module: `jackson-jq/fs`

This module reads and writes files and lists directories. Adding it allows jq programs to access and
modify every path the JVM process can access, so only make it available to programs that are trusted
to choose filesystem paths. Relative paths are resolved against the JVM working directory; absolute
paths are accepted.

```jq
import "jackson-jq/fs" as fs;

fs::read_text("message.txt")
fs::read_text("legacy.txt"; {encoding: "ISO-8859-1"})
fs::read_binary("image.png")
"hello" | fs::write_text("message.txt")
"café" | fs::write_text("legacy.txt"; {encoding: "ISO-8859-1"})
"more" | fs::write_text("log.txt"; {append: true, create_parents: true})
fs::read_binary("image.png") | fs::write_binary("copy.png")
fs::read_binary("chunk.dat") | fs::write_binary("data.bin"; {append: true})
fs::list("data")
fs::list("data"; {recursive: true, follow_symlinks: false})
fs::read_json("data.json")
fs::read_json_stream("stream.jsonl")
{"hello": "world"} | fs::write_json("output.json")
{"hello": "world"} | fs::write_json("output.json"; {indent: 2})
.items[] | fs::write_json("stream.jsonl"; {append: true, create_parents: true})
```

`read_text` decodes a file as UTF-8 by default. Its optional `encoding` is any charset available to
`Charset.forName` on the running JVM. Malformed or unmappable input raises an error instead of being
replaced. `read_binary` returns a binary value on providers that support one, including Jackson 2 and
Jackson 3; other providers receive padded Base64.

`read_json` parses a single JSON value from a file. It raises an error if the file is empty, malformed,
or contains trailing content beyond the first JSON value.

`read_json_stream` lazily reads multiple JSON values from a file (such as NDJSON / JSON Lines or
whitespace-separated JSON documents) and emits each value into the jq evaluation stream. An empty
or whitespace-only file emits no values.

`write_text` requires string input and encodes it as UTF-8 by default. It accepts an optional options
object with `encoding` (same as `read_text`), `append` (boolean, default `false`) to append to an
existing file instead of truncating, and `create_parents` or `mkdirs` (boolean, default `false`) to
automatically create parent directories if they do not already exist. It rejects input that cannot be
represented in the selected charset.

`write_binary` accepts a native binary value or a padded Base64 string. It accepts an optional options
object with `append` (boolean, default `false`) and `create_parents` or `mkdirs` (boolean, default
`false`).

Both `write_text` and `write_binary` return `null` after a successful write. If neither `create_parents`
nor `mkdirs` is enabled, the parent directory must already exist.

`write_json` serializes any JSON value and writes it to the destination file. By default, it writes
compact JSON followed by a newline (`\n`). The optional `indent` member enables indentation (specified
as an integer space count, a boolean where `true` uses two spaces, or a string such as `"\t"`). The
optional `newline` boolean (default `true`) controls whether a trailing newline is appended, `append`
(default `false`) controls whether to append to an existing file instead of truncating, `encoding`
(default `UTF-8`) selects the output charset, and `create_parents` or `mkdirs` (default `false`)
automatically creates parent directories if they do not already exist.

`list` returns a lexicographically sorted array of `{path, type}` objects. Each path is relative to the
listed directory and each type is `file`, `directory`, `symlink`, or `other`. The directory itself is
not included. By default only immediate children are returned. The optional `recursive` member enables
recursive traversal and `follow_symlinks`, which defaults to `true`, controls traversal into directory
symlinks. Symlinks remain identified as `symlink` entries even when followed. Recursive traversal skips
a symlink descent that would revisit a directory already in its current ancestor chain.

Runtime string and binary limits bound file contents returned by reads, while string, object, and
array limits apply to directory listings. Invalid arguments, unsupported encodings, inaccessible
paths, and filesystem failures raise jq errors. Write validation is completed before the destination
is opened, so invalid input, Base64, or encoding options do not truncate an existing file.
