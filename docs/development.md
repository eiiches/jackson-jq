# Branches and versioning

The project currently maintains two development branches:

* `develop/2.x`: The active development branch for the 2.x series. Preview releases are available on the [Releases](https://github.com/eiiches/jackson-jq/releases) page.
* `develop/1.x`: The maintenance branch for the 1.x series. New features that require breaking API changes are added only to `develop/2.x`.

The `develop/0.x` branch is no longer maintained.

Pull requests may target either development branch. The maintainers will port changes to the other branch when necessary.

Starting with version 1.0.0, the Java API follows [Semantic Versioning 2.0.0](https://semver.org/). A correction that brings jackson-jq behavior into alignment with jq may be released without a major version bump when its impact on existing users is limited. Changes that affect many users require a major version bump. Such compatibility changes are documented in the release notes.
