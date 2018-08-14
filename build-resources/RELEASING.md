RELEASING
====

Here are the steps for going through a release of test-operations


Integration with CI/CD
-----

The actual submission to Sonatype/Maven Central is done by the CI/CD system, when a pom file with a fixed version is submitted.


Steps
-----

1. `mvn version:update`
  - 
1. Build and test the project.  Make sure everything is ready to go.  Fix all javadoc and style issues.

2. `mvn release:prepare -P release`
  - as part of the prepare process, the pom will be checked in with a non-snapshot version.  This will be handled by the release.sh script and the build products will be deployed to Sonatype.

Versioning
-----
Test-operations uses semantic versioning, in the form of `MAJOR`.`MINOR`.`PATCH` .  In a nutshell that means that revisions that require substantial changes affect the major version, changes that require recompiling affect the minor version, and changes that require just new library deployment affect the patch version.
