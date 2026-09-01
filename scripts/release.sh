#!/bin/bash
set -euo pipefail

if [ "$#" -ne 2 ]; then
	echo "Usage: $0 release_version next_development_version" 1>&2
	exit 1
fi

scriptpath="$(readlink -f "$0")"
scriptdir="$(dirname "$scriptpath")"
cd "$scriptdir/.."

release_version="$1"
next_development_version="$2"
develop_branch=develop/2.x
master_branch=master/2.x
release_branch=release/$release_version
tag_name=$release_version

if git rev-parse --verify "refs/tags/$tag_name" > /dev/null 2>&1; then
	echo "Git tag already exists: $tag_name" 1>&2
	exit 1
fi

git checkout -b "$release_branch" "$develop_branch"
scripts/update-version-refs.sh prepare-release "$release_version"
git add -u
git commit -m "release: prepare release $release_version"
mvn clean verify

# merge release branch to develop
git checkout "$develop_branch"
git merge --no-ff "$release_branch" --no-edit
scripts/update-version-refs.sh prepare-next-development-iteration "$next_development_version"
git add -u
git commit -m "release: prepare for next development iteration"

# merge release branch to master
git checkout "$master_branch"
git merge --no-ff "$release_branch" --no-edit
git tag "$tag_name"
git branch -d "$release_branch"

# go back to develop branch
git checkout "$develop_branch"
