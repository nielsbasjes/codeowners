//
// CodeOwners Tools
// Copyright (C) 2023-2025 Niels Basjes
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an AS IS BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

File file = new File( basedir, "build.log" )
assert file.exists()

String text = file.getText( "utf-8" )

assert text.contains("[INFO] MATCH IGNORE |.svn/| ~ |^/?(.*/)?\\.svn/| --> |/.svn/|")
assert text.contains("[INFO] Using CODEOWNERS: \${baseDir}/.gitlab/CODEOWNERS")
assert !text.contains("[INFO] NO MATCH     |.svn/| ~ |^/?(.*/)?\\.svn/| --> |/.svn/|")
assert !text.contains("[INFO] MATCH IGNORE |.svn/| ~ |^/?(.*/)?\\.svn/| --> |/.svn/dummy.txt|")

return true
