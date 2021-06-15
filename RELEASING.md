# Release

The following steps should be followed to release the add-on:
 1. Run the workflow [Prepare Release](https://github.com/kingthorin/neonmarker/actions/workflows/prepare-release.yml),
    to prepare the release. It creates a pull request updating the version and changelog;
 2. Merge the pull request.

After merging the pull request the [Build Release](https://github.com/kingthorin/neonmarker/actions/workflows/release.yml) workflow
will create the tag, create the release, trigger the update of the marketplace, and create a pull request preparing the next development iteration.
