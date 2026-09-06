# Branches and versioning

The project currently maintains two development branches:

* `develop/2.x`: The active development branch for the 2.x series. Preview releases are available on the [Releases](https://github.com/eiiches/jackson-jq/releases) page.
* `develop/1.x`: The maintenance branch for the 1.x series. New features that require breaking API changes are added only to `develop/2.x`.

The `develop/0.x` branch is no longer maintained.

Pull requests may target either development branch. The maintainers will port changes to the other branch when necessary.

Starting with version 1.0.0, the Java API follows [Semantic Versioning 2.0.0](https://semver.org/). A correction that brings jackson-jq behavior into alignment with jq may be released without a major version bump when its impact on existing users is limited. Changes that affect many users require a major version bump. Such compatibility changes are documented in the release notes.

## Build and release tooling

The library is built and tested with Bazel. Run `bazelisk run //:idea-format -- --check` (or format with `bazelisk run //:idea-format`), format and lint Bazel files with `bazel run //:buildifier` (or check with `bazel run //:buildifier.check`), and run `bazel test //...` before submitting changes. The Maven projects under `smoke-tests/` are consumers, not part of the library build; populate the local repository with `bazel run //:maven_install` before running them.

Each Java package is its own Bazel target: a `BUILD.bazel` sits in the package directory and declares a `jjq_java_library` (or, under `src/test/java`, a `jjq_java_test_suite`), and the module's own `BUILD.bazel` lists them in `jjq_java_mrjar(packages = ...)`, which merges the per-package jars back into the one artifact the module publishes. Adding an import across package lines therefore means adding the corresponding dependency to that package's `BUILD.bazel`, and Bazel rejects the change outright if the result would be a cycle.

`jackson-jq-core` is the one exception: its packages are still a single cycle, so it is built from one module-level target. `scripts/package-deps.py` reports the cycles that remain there, and `--check` fails if one appears in any other module; `docs/package-cycles.md` describes what the core cycle consists of.

`version.bzl` is the single source of the project version. `scripts/update-version-refs.sh` updates it and the Maven consumer fixtures. Release artifacts are staged with the Bazel `maven_install` target, signed in memory, and uploaded as one bundle to the Central Publisher Portal.

Build from source
-----------------

The library uses Bazel 9. Bazelisk reads the pinned version from `.bazelversion`.

```sh
bazelisk run //:idea-format -- --check
bazel run //:buildifier_test
bazel test //...
```

The CLI runs straight out of the checkout:

```sh
echo '{"foo": 42}' | bazelisk run //jackson-jq-cli -- '.foo'
```

To exercise the Maven consumer projects, publish the Bazel-built artifacts locally first:

```sh
bazel run //:maven_install
for project in java8 jpms osgi quarkus graalvm; do
	mvn -f "smoke-tests/$project/pom.xml" clean verify
done
```

Updating maven_install.json
---------------------------

```sh
bazel run --repo_env=REPIN=1 @maven//:pin
```
