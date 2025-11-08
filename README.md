[![Github actions Build status](https://img.shields.io/github/actions/workflow/status/nielsbasjes/codeowners/build.yml?branch=main)](https://github.com/nielsbasjes/codeowners/actions)
[![Coverage Status](https://img.shields.io/codecov/c/github/nielsbasjes/codeowners)](https://app.codecov.io/gh/nielsbasjes/codeowners)
[![License](https://img.shields.io/:license-apache-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Maven Central: codeowners-reader](https://img.shields.io/maven-central/v/nl.basjes.codeowners/codeowners-reader.svg?label=codeowners-reader)](https://central.sonatype.com/namespace/nl.basjes.codeowners)
[![Maven Central: gitignore-reader](https://img.shields.io/maven-central/v/nl.basjes.gitignore/gitignore-reader.svg?label=gitignore-reader)](https://central.sonatype.com/namespace/nl.basjes.gitignore)
[![Maven Central: codeowners-enforcer-rules](https://img.shields.io/maven-central/v/nl.basjes.maven.enforcer.codeowners/codeowners-enforcer-rules.svg?label=codeowners-enforcer-rules)](https://central.sonatype.com/namespace/nl.basjes.maven.enforcer.codeowners)
[![GitHub stars](https://img.shields.io/github/stars/nielsbasjes/codeowners?label=GitHub%20stars)](https://github.com/nielsbasjes/codeowners/stargazers)
[![If this project has business value for you then don't hesitate to support me with a small donation.](https://img.shields.io/badge/Sponsor%20me-via%20Github-red.svg)](https://github.com/sponsors/nielsbasjes)
[![If this project has business value for you then don't hesitate to support me with a small donation.](https://img.shields.io/badge/Donations-via%20Paypal-red.svg)](https://www.paypal.me/nielsbasjes)

# CodeOwners
In several systems (like GitHub and Gitlab) you can have a CODEOWNERS file which is used to ensure all changes are approved by the right people.

Reality: The syntax of these files can be tricky, and it is quite easy to write a config that has the effect that not all files are covered.

# What is this
1) A Java library to read a CODEOWNERS file.
2) A Java library to read a/all .gitignore file(s) in directory tree.
3) A Java library to validate the CODEOWNERS against the non-ignored project files.
3.1) This also includes the ability to validate against the actual users, groups and roles in Gitlab. (See [README-gitlab.md](README-gitlab.md))
4) An extra rule for the Maven Enforcer plugin that wraps the validation library. (See Usage below)
5) An example commandline script (using Koltin script) that wraps the validation library.

The intended goal is to make the build fail if the codeowners file does not cover all files and directories in the project.

- The libraries for the CODEOWNERS and gitignore files are usable in Java 8 and newer.
- The Maven Enforcer rule needs Java 11 or newer.

# CodeOwners Enforcer rule
## Configuration parameters

- **baseDir**
  - In case you run the plugin from a child module.
- **codeOwnersFile**
  - The name of the codeowners file when you are not using a common standard.
- **allExisingFilesMustHaveCodeOwner**
  - Check that all existing files have at least one mandatory codeowner.
  - Gitlab also supports "Optional" code owners but that is useless to check for.
- **allNewlyCreatedFilesMustHaveCodeOwner**
  - Check that if a new file is created in any of the directories in the project that it would automatically have a mandatory code owner.
  - Note when a specific filename exception is used in the gitignore rules then this check is not perfect.
- **allFilesMustHaveCodeOwner**
  - Do both allExisingFilesMustHaveCodeOwner and allNewlyCreatedFilesMustHaveCodeOwner
- **verbose**
    - Make the rule output much more details than you would normally like to see.
- **showApprovers**
    - Make the rule output show the approvers for all non-ignored files in the entire project. The intended usage is that with this you can debug the CODEOWNERS result and manually see for every file in your project if you are happy with the resulting approvers.

## Checking against the users present on a Gitlab Instance (BETA)
If you have a Gitlab instance with your project then you can get additional checks done.
This plugin can also verify if all mentioned users, groups and roles are actually allowed to approve a change.
This is important because when Gitlab determines that all the mentioned approvers either do not exist or do not have the appropriate approval level then everyone can merge any change on a file.

___**This is currently a BETA feature.**___

Checking this page for all the information: [README-gitlab.md](README-gitlab.md)

## Example
In one of my projects it looks like this:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-enforcer-plugin</artifactId>
  <version>3.4.1</version>
  <dependencies>
    <dependency>
      <groupId>nl.basjes.maven.enforcer.codeowners</groupId>
      <artifactId>codeowners-enforcer-rules</artifactId>
      <version>1.12.2</version>
    </dependency>
  </dependencies>
  <executions>
    <execution>
      <id>Ensure the CODEOWNERS is correct</id>
      <phase>verify</phase>
      <goals>
        <goal>enforce</goal>
      </goals>
      <inherited>false</inherited>
      <configuration>
        <rules>
          <codeOwners>
            <baseDir>${maven.multiModuleProjectDirectory}</baseDir>
            <codeOwnersFile>${maven.multiModuleProjectDirectory}/CODEOWNERS</codeOwnersFile>
            <allFilesMustHaveCodeOwner>true</allFilesMustHaveCodeOwner>
            <!-- <verbose>true</verbose> -->
            <!-- <showApprovers>true</showApprovers> -->
            <gitlab>
              <accessToken>
                <environmentVariableName>CHECK_USERS_TOKEN</environmentVariableName>
              </accessToken>
            </gitlab>
          </codeOwners>
        </rules>
      </configuration>
    </execution>
  </executions>
</plugin>
```

# GitIgnore library

## Basic use
Simply create an instance of the GitIgnoreFileSet and pass the root directory of the project as a parameter
```java
GitIgnoreFileSet ignoreFileSet = new GitIgnoreFileSet(new File("/home/niels/workspace/project"));
```
You can then check for each file in the project if it is ignored or not
```java
if (ignoreFileSet.ignoreFile("/home/niels/workspace/project/something/something/README.md")) {
    ...
}
```

## Full path or just or a project relative filename
The directory name with which you initialize the `GitIgnoreFileSet` is considered to be the directory name of the project root.
By default, if you ask for a file if it is ignored or not, this library assumes you are specifying all files within the SAME base directory structure.

So loading the gitignore files from `workspace/project` then a file in the root of the project must (by default) be specified as `workspace/project/pom.xml` and a file deeper in the project as for example `workspace/project/src/main/java/nl/basjes/Something.java`

Something like this:
```java
GitIgnoreFileSet ignoreFileSet = new GitIgnoreFileSet(new File("workspace/project"));

if (ignoreFileSet.ignoreFile("workspace/project/pom.xml")) {
    ...
}
```
So comming from the default situation the `ignoreFileSet.assumeQueriesIncludeProjectBaseDir()` does nothing.


You can **optionally specify** that the files you request are **relative to the project root**.

So loading the gitignore files from `workspace/project` then a file in the root of the project must be specified as `/pom.xml` and a file deeper in the project as for example `/src/main/java/nl/basjes/Something.java`

Something like this:
```java
GitIgnoreFileSet ignoreFileSet = new GitIgnoreFileSet(new File("workspace/project"));

if (ignoreFileSet.ignoreFile("/pom.xml", true)) {
    ...
}
```

Or you can set it as the default assumption.

```java
GitIgnoreFileSet ignoreFileSet = new GitIgnoreFileSet(new File("workspace/project"));
ignoreFileSet.assumeQueriesAreProjectRelative();

if (ignoreFileSet.ignoreFile("/pom.xml")) {
    ...
}
```


## GitIgnore edge case
This [tutorial page](https://www.atlassian.com/git/tutorials/saving-changes/gitignore) documents this edge case that this library also follows.

I see this as unexpected behaviour yet this is really what git does !

**Pattern**

    logs/
    !logs/important.log

**Matches**

    logs/debug.log
    logs/important.log

**Explanation**

    Wait a minute! Shouldn't logs/important.log be negated in the example on the left
    Nope! Due to a performance-related quirk in Git, you can not negate a file that is ignored due to a pattern matching a directory

# Building
The maven build must be run under Java 17 or newer (because of plugins) and will use toolchains to actually build the software using JDK 21.

# License

    CodeOwners Tools
    Copyright (C) 2023-2025 Niels Basjes

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an AS IS BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
