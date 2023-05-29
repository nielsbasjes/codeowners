# CodeOwners
In several systems (like Github and Gitlab) you can have a CODEOWNERS file which is used to ensure all changes are approved by the right people.

Reality: The systax of thes files can be tricky and it is quite easy to write a config that has the effect that not all files are covered.

# What is this
1) Some software to read and parse a CODEOWNER file.
2) Some software to read and parse a .gitignore file.
3) An extra set of rules for the Maven Enforcer plugin to check if everything is good.

# Usage

    <plugin>
      <artifactId>maven-enforcer-plugin</artifactId>
      <version>@maven-enforcer-plugin.version@</version>
      <dependencies>
        <dependency>
          <groupId>nl.basjes.maven.enforcer.codeowners</groupId>
          <artifactId>codeowners-enforcer-rules</artifactId>
          <version>@project.version@</version>
        </dependency>
      </dependencies>
      <configuration>
        <rules>
          <codeOwners>
            <codeOwnersFile>.gitlab/CODEOWNERS</codeOwnersFile>
            <allFilesMustHaveCodeOwner>true</allFilesMustHaveCodeOwner>
          </codeOwners>
        </rules>
      </configuration>
      <executions>
        <execution>
        <phase>compile</phase>
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
