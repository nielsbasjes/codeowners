//
// CodeOwners Tools
// Copyright (C) 2023-2024 Niels Basjes
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

assert text.contains("[INFO] Using GitIgnore : \${baseDir}/.gitignore")
assert text.contains("[INFO] Using GitIgnore : \${baseDir}/dir1/.gitignore")
assert text.contains("[INFO] Using GitIgnore : \${baseDir}/dir3/.gitignore")

// These exist but are ignored so they may not be read.
assert !text.contains("[INFO] Using GitIgnore : \${baseDir}/dir4/.gitignore")
assert !text.contains("[INFO] Using GitIgnore : \${baseDir}/dir4/subdir/.gitignore")
assert !text.contains("[INFO] Using GitIgnore : \${baseDir}/dir4/subdir/subdir/.gitignore")

assert text.contains("[INFO] Using CODEOWNERS: \${baseDir}/docs/CODEOWNERS")

assert text.contains("[INFO] Approvers for: \${baseDir}/ : [@integrationtest]")
assert text.contains("[INFO] Approvers for: \${baseDir}/.gitignore : [@integrationtest]")
assert text.contains("[INFO] Approvers for: \${baseDir}/.mvn/ : [@integrationtest]")
assert text.contains("[INFO] Approvers for: \${baseDir}/docs/CODEOWNERS : [@documentation]")
assert text.contains("[INFO] Approvers for: \${baseDir}/dir1/ : [@integrationtest]")
assert text.contains("[INFO] Approvers for: \${baseDir}/dir1/.gitignore : [@dir1project]")
assert text.contains("[INFO] Approvers for: \${baseDir}/dir1/dir1.md : [@dir1project]")
assert text.contains("[INFO] Approvers for: \${baseDir}/dir2/ : [@integrationtest]")
assert text.contains("[INFO] Approvers for: \${baseDir}/dir2/dir2.txt : [@txt]")
assert text.contains("[INFO] Approvers for: \${baseDir}/dir2/file2.log : [@log]")
assert text.contains("[INFO] Approvers for: \${baseDir}/dir3/ : [@integrationtest]")
assert text.contains("[INFO] Approvers for: \${baseDir}/dir3/.gitignore : [@dir3project]")
assert text.contains("[INFO] Approvers for: \${baseDir}/dir3/dir3.txt : [@dir3project]")
assert text.contains("[INFO] Approvers for: \${baseDir}/dir3/file3.log : [@dir3project]")
assert text.contains("[INFO] Approvers for: \${baseDir}/invoker.properties : [@integrationtest]")
assert text.contains("[INFO] Approvers for: \${baseDir}/pom.xml : [@integrationtest]")
assert text.contains("[INFO] Approvers for: \${baseDir}/verify.groovy : [@integrationtest]")

// Make sure NONE of the ignored files get an approver

assert !text.contains("[INFO] Approvers for: \${baseDir}/dir1/dir1.log")
assert !text.contains("[INFO] Approvers for: \${baseDir}/dir1/dir1.txt")
assert !text.contains("[INFO] Approvers for: \${baseDir}/dir1/file1.log")
assert !text.contains("[INFO] Approvers for: \${baseDir}/dir2/dir2.log")
assert !text.contains("[INFO] Approvers for: \${baseDir}/dir2/dir2.md")
assert !text.contains("[INFO] Approvers for: \${baseDir}/dir3/dir3.log")
assert !text.contains("[INFO] Approvers for: \${baseDir}/dir3/dir3.md")
assert !text.contains("[INFO] Approvers for: \${baseDir}/dir4/") // Which matches everything under dir4
assert !text.contains("[INFO] Approvers for: \${baseDir}/README.md")
assert !text.contains("[INFO] Approvers for: \${baseDir}/root.md")

return true
