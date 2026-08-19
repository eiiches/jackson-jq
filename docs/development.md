# Branches and versioning

There are currently two development branches.

* `develop/1.x`: This branch (you are viewing), which is currently under development for the future 1.0 release. You can find preview releases at [Releases](https://github.com/eiiches/jackson-jq/releases) page (tags: `1.0.0-preview.yyyyMMdd`). Although the API is not stable yet, I recommend new users to use these releases insetad of 0.x versions, because these releases have more features, better compatibility, and better performance.
* `develop/0.x`: The development branch for 0.x versions. Features that need breaking API changes will no longer be added. Go to [Releases](https://github.com/eiiches/jackson-jq/releases) and find the latest 0.x.y version.

PRs can be sent to any of the develop/\* branches. The patch will be ported to the other branch(es) if necessary.

We use [Semantic Versioning 2.0.0](https://semver.org/) for Java API versioning, 1.0.0 onwards. A jq behavior fix (even if it may possibly affect users) will not be considered a major change if the fix is to make the bahavior compatible with ./jq; these kind of incompatible changes are documented in the release note.

If you get different results between ./jq and jackson-jq, please [file an issue](https://github.com/eiiches/jackson-jq/issues). That is a bug on jackson-jq side.
