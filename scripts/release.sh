#!/bin/bash
set -euo pipefail

if [ "$#" -ne 2 ]; then
	echo "Usage: $0 version snapshot_version" 1>&2
	exit 1
fi

scriptpath="$(readlink -f "$0")"
scriptdir="$(dirname "$scriptpath")"
cd "$scriptdir/.."

version="$1"
next_version="$2"
develop_branch=develop/1.x
master_branch=master/1.x
release_branch=release/$version
tag_name=$version

if git rev-parse $tag_name > /dev/null 2>&1; then
	echo "Git tag already exists: $tag_name" 1>&2
	exit 1
fi

git checkout -b $release_branch $develop_branch
mvn clean release:clean release:prepare -DpushChanges=false -DignoreSnapshots=true -DtagNameFormat=$tag_name -DreleaseVersion=$version -DdevelopmentVersion=$next_version

git branch $release_branch-tmp
git reset --hard HEAD~1

if [ -e scripts/update-version-refs.sh ]; then
	scripts/update-version-refs.sh $version
	git add -u
	git commit --amend -c HEAD --no-edit
fi

# merge release branch to develop
git checkout $develop_branch
git merge --no-ff $release_branch --no-edit
git cherry-pick $release_branch-tmp

# merge release branch to master
git checkout $master_branch
git merge --no-ff $release_branch --no-edit
git tag -d $tag_name
git tag $tag_name
git branch -D $release_branch-tmp
git branch -d $release_branch

# go back to develop branch
git checkout $develop_branch
