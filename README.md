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
2) A Java library to read a .gitignore file.
3) An extra rule for the Maven Enforcer plugin to check the CODEOWNERS against the actual project files. (See Usage below)

The intended goal is to make the build fail if the codeowners file does not cover all files and directories in the project.

# Usage
## Configuration parameters

- **baseDir**
  - In case you run the plugin from a child module.
- **codeOwnersFile**
  - The name of the codeowners file incase you are not using a common standard.
- **allExisingFilesMustHaveCodeOwner**
  - Check that all existing files have a mandatory codeowner.
  - Gitlab also supports "Optional" code owners but that is useless to check for.
- **allNewlyCreatedFilesMustHaveCodeOwner**
  - Check that if a new file is created in any of the directories in the project that it would automatically have a mandatory code owner.
- **allFilesMustHaveCodeOwner**
  - Do both allExisingFilesMustHaveCodeOwner and allNewlyCreatedFilesMustHaveCodeOwner
- **verbose**
    - Make the rule output much more details than you would normally like to see.
- **showApprovers**
    - Make the rule output show the approvers for all non-ignored files in the entire project. The intended usage is that with this you can debug the CODEOWNERS result and manually see for every file in your project if you are happy with the resulting approvers.

## Example
In one of my projects it looks like this:

    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-enforcer-plugin</artifactId>
      <version>3.4.1</version>
      <dependencies>
        <dependency>
          <groupId>nl.basjes.maven.enforcer.codeowners</groupId>
          <artifactId>codeowners-enforcer-rules</artifactId>
          <version>1.3.1</version>
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
              </codeOwners>
            </rules>
          </configuration>
        </execution>
      </executions>
    </plugin>

# Important
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


# License

    CodeOwners Tools
    Copyright (C) 2023-2024 Niels Basjes

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an AS IS BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
