# Checking against the users present on a Gitlab Instance (BETA)
If you have a Gitlab instance with your project then you can get additional checks done.
This plugin can also verify if all mentioned users, groups and roles are actually allowed to approve a change.
This is important because when Gitlab determines that all the mentioned approvers either do not exist or do not have the appropriate approval level then everyone can merge any change on a file.

___**This is currently a BETA feature.**___

A known limitation is that checking against the email address in a CODEOWNERS file is only possible IF the user has made that specific email address their public address.

## Usage
There are 3 parameters needed to activate this feature:
- The Gitlab Project/Personal Access Token (only `read_api` and `GUEST` role are needed)
- The Gitlab server url
- The Gitlab project id of this project

This check will fail on existing users/groups that are not part of the project, on illegal roles (i.e. `@@something`) and if it is certain a rule has 0 approvers.
If a user is configured by email address and this cannot be found it is NOT certain this user does or does not have access.
This is because of security limitations in the Gitlab API which only allow a regular user to find other users by their public email address.
See https://docs.gitlab.com/api/users/#as-a-regular-user

## Output
By default a table is output with the warnings and errors.
You can set this to get output on all approvers.

```xml
<gitlab>
  <showAllApprovers>true</showAllApprovers>
</gitlab>
```

## Gitlab Project/Personal Access Token
To allow this plugin to use the Gitlab API an access token is needed that is created with only `read_api` and the `GUEST` role.

**You should never commit ANY token to a repository so this plugin simply cannot directly read any token.
It is ONLY possible to read it from an environment variable.**

So the token can only be retrieved from an environment variable.
In this example it is expected that the environment variable `CHECK_USERS_TOKEN` contains the value of you token.

```xml
<gitlab>
  <accessToken>
    <environmentVariableName>CHECK_USERS_TOKEN</environmentVariableName>
  </accessToken>
</gitlab>
```

**TIP**: When running in Gitlab CI only the access token is needed via an environment variable. If you configure it this way (which is recommended) then starting with version 1.11.1 it will automatically skip this check if you are NOT running within a GitlabCI environment.

## When to fail
For some projects it is not wanted to fail if there is a problem with the CODEOWNERS in relation to the Gitlab accounts and permissions.
Normally this rule will fail if an Error level situation is found.

You can change when to fail in relation to the Gitlab users and groups:
- **NEVER**:   **Never fail**, even under configuration errors that will never be correct (i.e. `FATAL`).
- **FATAL**:   Only fail if there is a Fatal problem (configuration error).
- **ERROR**:   Only fail if there is an Error or Fatal problem. (this is the default setting)
- **WARNING**: Fail if there is at least a single Warning, Error or Fatal.

```xml
<gitlab>
  <failLevel>FATAL</failLevel>
</gitlab>
```

You can also configure what level specific problems are.
You can specify either `INFO`, `WARNING`, `ERROR` or `FATAL`.

Note that specifying a non-existent role (i.e. `@@something`) is always of level `FATAL` because it is a configuration error.

The available settings with the default value in the example
- There are no users in the mentioned role (i.e. @@developer, @@maintainer or @owner).
  ```xml
  <gitlab>
    <problemLevels>
      <roleNoUsers>WARNING</roleNoUsers>
  ```
- A user was specified by email, but it was not their public email so it is impossible to verify if this user actually exists.
  There are 2 settings here; what level of problem is this, and if it happens should we assume the users exists and has enough permissions to approve?
  ```xml
  <gitlab>
    <assumeUncheckableEmailExistsAndCanApprove>true</assumeUncheckableEmailExistsAndCanApprove>
    <problemLevels>
      <userUnknownEmail>WARNING</userUnknownEmail>
    </problemLevels>
  </gitlab>
    ```
- The user was found and their account was disabled (either locked and/or not active).
  ```xml
  <gitlab>
    <problemLevels>
      <userDisabled>WARNING</userDisabled>
    </problemLevels>
  </gitlab>
  ```
- The mentioned approver does not exist; unknown if this was intended as a group or a user.
  ```xml
  <gitlab>
    <problemLevels>
      <approverDoesNotExist>WARNING</approverDoesNotExist>
    </problemLevels>
  </gitlab>
  ```
- The mentioned user exists but does not have enough permissions to actually do any approving.
  ```xml
  <gitlab>
    <problemLevels>
      <userTooLowPermissions>WARNING</userTooLowPermissions>
    </problemLevels>
  </gitlab>
  ```
- The mentioned group exists but does not have enough permissions to actually do any approving.
  ```xml
  <gitlab>
    <problemLevels>
      <groupTooLowPermissions>WARNING</groupTooLowPermissions>
    </problemLevels>
  </gitlab>
  ```
- The mentioned user exists but is not a member in this project.
  ```xml
  <gitlab>
    <problemLevels>
      <userNotProjectMember>ERROR</userNotProjectMember>
    </problemLevels>
  </gitlab>
  ```
- The mentioned group exists but is not a member in this project.
  ```xml
  <gitlab>
    <problemLevels>
      <groupNotProjectMember>ERROR</groupNotProjectMember>
    </problemLevels>
  </gitlab>
  ```
- After removing all users and groups that cannot approve there are no approvers left for the mentioned rule.
  ```xml
  <gitlab>
    <problemLevels>
      <noValidApprovers>ERROR</noValidApprovers>
    </problemLevels>
  </gitlab>
  ```


## Gitlab server url
The Gitlab server url can be configured directly or retrieved from an environment variable.
- If not configured it is assumed it can be found in the environment variable `CI_SERVER_URL` which is the default place where Gitlab CI puts it.
- Directly configured:
    ```xml
    <gitlab>
      <serverUrl>
        <url>https://gitlab.example.nl</url>
      </serverUrl>
    </gitlab>
    ```
- Retrieved from a configured environment variable. In this example it is expected that the environment variable `MY_ENVIRONMENT_VARIABLE` contains something like `https://gitlab.example.nl`.
    ```xml
    <gitlab>
      <serverUrl>
        <environmentVariableName>MY_ENVIRONMENT_VARIABLE</environmentVariableName>
      </serverUrl>
    </gitlab>
    ```

Effectively the default config for this is
```xml
<gitlab>
  <serverUrl>
    <environmentVariableName>CI_SERVER_URL</environmentVariableName>
  </serverUrl>
</gitlab>
```

## Gitlab project id
The Gitlab project id can be configured directly or retrieved from an environment variable.
- If not configured it is assumed it can be found in the environment variable `CI_PROJECT_ID` which is the default place where Gitlab CI puts it.
- Directly configured:
    ```xml
    <gitlab>
      <projectId>
        <id>group/project</id>
      </projectId>
    </gitlab>
    ```
- Retrieved from a configured environment variable. In this example it is expected that the environment variable `MY_ENVIRONMENT_VARIABLE` contains something like `group/project`.
    ```xml
    <gitlab>
      <projectId>
        <environmentVariableName>MY_ENVIRONMENT_VARIABLE</environmentVariableName>
      </projectId>
    </gitlab>
    ```

Effectively the default config for this is
```xml
<gitlab>
  <projectId>
    <environmentVariableName>CI_PROJECT_ID</environmentVariableName>
  </projectId>
</gitlab>
```


## Example (Normal usage)
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
      <version>1.14.1</version>
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

## Example (All options example)

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-enforcer-plugin</artifactId>
  <version>3.4.1</version>
  <dependencies>
    <dependency>
      <groupId>nl.basjes.maven.enforcer.codeowners</groupId>
      <artifactId>codeowners-enforcer-rules</artifactId>
      <version>1.14.1</version>
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
              <serverUrl>
                <environmentVariableName>CI_SERVER_URL</environmentVariableName>
              </serverUrl>
              <projectId>
                <environmentVariableName>CI_PROJECT_ID</environmentVariableName>
              </projectId>
              <showAllApprovers>true</showAllApprovers>

              <failLevel>FATAL</failLevel>

              <assumeUncheckableEmailExistsAndCanApprove>true</assumeUncheckableEmailExistsAndCanApprove>
              <problemLevels>
                <roleNoUsers>WARNING</roleNoUsers>
                <userUnknownEmail>WARNING</userUnknownEmail>
                <userDisabled>WARNING</userDisabled>
                <approverDoesNotExist>WARNING</approverDoesNotExist>
                <userTooLowPermissions>WARNING</userTooLowPermissions>
                <groupTooLowPermissions>WARNING</groupTooLowPermissions>
                <userNotProjectMember>ERROR</userNotProjectMember>
                <groupNotProjectMember>ERROR</groupNotProjectMember>
                <noValidApprovers>FATAL</noValidApprovers>
              </problemLevels>
            </gitlab>
          </codeOwners>
        </rules>
      </configuration>
    </execution>
  </executions>
</plugin>
```

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
