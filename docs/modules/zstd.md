# Zstandard module

Maven artifact: `jackson-jq-ext-module-zstd`

jq module: `jackson-jq/zstd`

This module provides Zstandard compression and decompression:

```jq
import "jackson-jq/zstd" as zstd;
```

The filters are named after the *uncompressed* side of the conversion. Compressed data is always a
binary value: a binary node on a JSON provider that has a binary node type, such as the Jackson 2 or
Jackson 3 providers, and a padded standard Base64 string on every other provider. This is the same
representation `fs::read_binary` and `http::get`'s `raw_body` use, so those results can be piped
straight in.

| Function | Input | Output |
| --- | --- | --- |
| `compress_binary/0` | a binary value, or a string containing standard Base64 | a binary value |
| `compress_text/{0,1}` | a string | a binary value |
| `decompress_binary/0` | a binary value, or a string containing standard Base64 | a binary value |
| `decompress_text/{0,1}` | a binary value, or a string containing standard Base64 | a string |

```console
$ jackson-jq -n 'import "jackson-jq/zstd" as zstd; "hello" | zstd::compress_text | zstd::decompress_text'
"hello"
```

`compress_text` and `decompress_text` encode and decode as UTF-8 by default. Their optional argument is
an object whose `encoding` member names any charset available to `Charset.forName` on the running JVM:

```console
$ jackson-jq -n 'import "jackson-jq/zstd" as zstd; "café" | zstd::compress_text({encoding: "ISO-8859-1"}) | zstd::decompress_text({encoding: "ISO-8859-1"})'
"café"
```

Malformed or unmappable text raises an error instead of being replaced, as in `fs::read_text`.

`RuntimeOptions.Builder.setMaxBinaryLength(...)` bounds binary results, while `setMaxStringLength(...)`
bounds Base64 results and the text `decompress_text` returns; decompression stops as soon as the
relevant limit would be exceeded.

The module uses `zstd-jni`, which bundles platform-specific native libraries and supports Java 8 and
later on its published platforms. In a GraalVM native image, `zstd-jni` needs reachability metadata for
the fields its JNI code writes and for the bundled native library it loads as a resource. That metadata
is published in the [GraalVM reachability metadata repository](https://github.com/oracle/graalvm-reachability-metadata),
which GraalVM Native Build Tools apply on their own once
`<metadataRepository><enabled>true</enabled></metadataRepository>` is set. The module itself ships only a
`native-image.properties` that initializes `com.github.luben.zstd` at run time, which build systems that
default to build-time initialization require. On JVMs that restrict native-library access, enable it for
`com.github.luben.zstd_jni` on the module path or for `ALL-UNNAMED` on the classpath.

Quarkus does not consume the metadata repository ([quarkusio/quarkus#28994](https://github.com/quarkusio/quarkus/discussions/28994)),
so a Quarkus application has to unpack it and hand the directory to `native-image` itself. Add the unpack
to the build:

```xml
<plugin>
	<groupId>org.apache.maven.plugins</groupId>
	<artifactId>maven-dependency-plugin</artifactId>
	<executions>
		<execution>
			<id>unpack-reachability-metadata</id>
			<phase>generate-resources</phase>
			<goals><goal>unpack</goal></goals>
			<configuration>
				<artifactItems>
					<artifactItem>
						<groupId>org.graalvm.buildtools</groupId>
						<artifactId>graalvm-reachability-metadata</artifactId>
						<version>1.1.12</version>
						<classifier>repository</classifier>
						<type>zip</type>
						<includes>com.github.luben/zstd-jni/**</includes>
						<outputDirectory>${project.build.directory}/graalvm-reachability-metadata</outputDirectory>
					</artifactItem>
				</artifactItems>
			</configuration>
		</execution>
	</executions>
</plugin>
```

Then point the native build at the unpacked directory:

```properties
quarkus.native.additional-build-args=-H:ConfigurationFileDirectories=${project.build.directory}/graalvm-reachability-metadata/com.github.luben/zstd-jni/1.5.7-2
```

The last path segment is the metadata version, which is not the `zstd-jni` version; the repository's
`com.github.luben/zstd-jni/index.json` maps one to the other. `smoke-tests/quarkus` does exactly this.
