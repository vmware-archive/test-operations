RELEASING
====

Here are the steps for going through a release of test-operations


Integration with CI/CD
-----

The actual submission to Sonatype/Maven Central is done by the CI/CD system, when a `pom.xml` file with a fixed version is submitted.


Steps
-----

1. `mvn versions:display-dependency-updates -P release`

   This command will verify that the dependent versions are up to date, and ensures that the project has the latest security fixes.

2. `mvn clean verify -P release`

   Build and test the project to make sure everything is ready to go.

3. `mvn release:prepare -P release`

   Respond to the prompts about release version numbers based on semantic version (below)

   As part of the prepare process, the pom will be checked in with a non-snapshot version.  This will be picked up by the `travis-release.sh` script and the build products will be deployed to Sonatype.

4. `mvn release:clean`

    To clean up the files created during the release process

5. Release the binary in nexus

    Log into http://oss.sonatype.org and find the artifacts in the staging repository.  Close and promote the repository to make them available publicly.
    Note that due to the issue described in https://issues.sonatype.org/browse/OSSRH-5454, releasing from a corporate network can fail.


Versioning
-----
Test-operations uses semantic versioning, in the form of `MAJOR`.`MINOR`.`PATCH` .  In a nutshell that means that revisions that require substantial changes affect the major version, changes that require recompiling affect the minor version, and changes that require just new library deployment affect the patch version.
