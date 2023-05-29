# CodeOwners
In several systems (like Github and Gitlab) you can have a CODEOWNERS file which is used to ensure all changes are approved by the right people.

Reality: The systax of thes files can be tricky and it is quite easy to write a config that has the effect that not all files are covered.

# What is this
1) Some software to read and parse a CODEOWNER file.
2) Some software to read and parse a .gitignore file.
3) An extra set of rules for the Maven Enforcer plugin to check if everything is good.

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

## Example
In one of my projects it looks like this:

    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-enforcer-plugin</artifactId>
      <version>3.3.0</version>
      <dependencies>
        <dependency>
          <groupId>nl.basjes.maven.enforcer.codeowners</groupId>
          <artifactId>codeowners-enforcer-rules</artifactId>
          <version>0.0.1-SNAPSHOT</version>
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
              </codeOwners>
            </rules>
          </configuration>
        </execution>
      </executions>
    </plugin>


# License

    CodeOwners Tools
    Copyright (C) 2023 Niels Basjes

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an AS IS BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
