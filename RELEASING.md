Releasing builds to sonatype
====


This is the set of steps on how to release to sonatype


1 Configure Authorization
----
Get your access token from Sonatype, and embed it in your ~/.m2/settings.xml

```
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
   <!-- Create a user token on sonatype.org -->
   <server>
      <id>ossrh</id>
      <username>your-token-key</username>
      <password>your-token-secret</password>
   </server>
  </servers>
</settings>
```

2 Deploy the snapshot
---
Tests have alreayd been run at this point, so no need to run them again, as they obfuscate the process:

```
mvn clean deploy -Dmaven.test.skip=true
```

3 Verify the snapshot
---
Log into https://oss.sonatype.org/#nexus-search;quick~com.vmware.test-operations and verify the
snapshots are visible

<!-- Create a github token
---

https://github.com/settings/tokens/new

With "repo" scope -->


4 Configure for release
---

After a few tries, I don't believe we can use the maven release plugin, because the github project
is set up to only allow pull requests, and the release plugin doesn't work in that configuration.

Ensure that `git status` is clean, no extra files or diffs.

5 Create the release pull request
---

This will create a pull request with the non-SNAPSHOT build.  Replace with the correct version number.

```
git switch -c release-1.2.1
mvn versions:set -DnewVersion=1.2.1
git add -u
git commit
git push
```

6 Push to sonatype
---

This time we have to also sign the build products

```
mvn -Dmaven.test.skip=true -PskipChecks clean verify source:jar javadoc:jar gpg:sign deploy
```


7 Promote the build in sonatype
---
From the sonatype UI, you can click onthe "Build Promotion -> Staging Repositories" in the left nav

Your deployed items should be found there.

You will want to find the staging repository with the javadoc, jar, and pom content.  You should "Drop" the unneeded repositories.

The full repository will have folders for test-operations, test-utilities, and test-operations-parent.


"Close" the repository you want to promote.  This locks it, preventing further changes.  It also runs a series of sanity checks.

"Refresh" to get the updated status.  If there was a problem closing the repository, an icon will appear that you can click on for details.  It can take a while for the close operation to complete.

Finally, "Release" the closed repository to merge it into maven central.

8 Test the availability in sonatype
---

Visit https://repo.maven.apache.org/maven2/com/vmware/test-operations/


9 Create the release in GitHub
---

It’s common practice to prefix your version names with the letter v, e.g. `v1.2.0`

You can create a new tag at the time of making the release.


10 Create the new development pull request
---
This puts the repository in a mode ready for more development and pull requests.

```
git switch -c release-1.2.2-SNAPSHOT
mvn versions:set -DnewVersion=1.2.2-SNAPSHOT
git add -u
git commit
git push
```
