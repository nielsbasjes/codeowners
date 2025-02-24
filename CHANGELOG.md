
This is intended as an overview of the major changes

NEXT VERSION
===
- ...

v1.11.0
===
- The maven enforcer rule now requires Java 11 or newer
- Add support for assigning codeowners based on project role (https://gitlab.com/gitlab-org/gitlab/-/issues/282438)
- Fix parsing of email address
- Check approvers against the actual users, groups and roles in Gitlab (BETA feature)

v1.10.0
===
- Leverage new toolchains plugin: no longer needs toolchains.xml.
- Automatically load globally defined ignore rules from \$XDG_CONFIG_HOME/git/ignore or \$HOME/.config/git/ignore (same as documented here https://git-scm.com/docs/gitignore ). Feature written by https://github.com/raboof

v1.9.0
===
- Searching for the gitIgnore files returns the list of added files.
- Fixed searching for the gitIgnore files still scans an ignored directory.

v1.8.0
===
- Only search non-ignored directories for gitignore files.

v1.7.0
===
- Support multiple GitIgnore for the same directory

v1.5.1
===
- Fix bug in new directory handling

v1.5.0
===
- Handle filenames better (project relative vs full path)
- A ? and * may not match a / (directory separator)
- A \\? and \\* must match the exact character.


License
=======
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
