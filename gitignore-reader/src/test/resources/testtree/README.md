# Git ignore analysis:
$ git check-ignore --no-index --verbose $(find . -type f | sort)
```
gitignore-reader/src/test/resources/testtree/.gitignore:4:*.log ./dir1/dir1.log
gitignore-reader/src/test/resources/testtree/dir1/.gitignore:2:!dir1.md ./dir1/dir1.md
gitignore-reader/src/test/resources/testtree/dir1/.gitignore:1:*.txt    ./dir1/dir1.txt
gitignore-reader/src/test/resources/testtree/dir1/.gitignore:3:file1.log        ./dir1/file1.log
gitignore-reader/src/test/resources/testtree/.gitignore:4:*.log ./dir2/dir2.log
gitignore-reader/src/test/resources/testtree/.gitignore:1:*.md  ./dir2/dir2.md
gitignore-reader/src/test/resources/testtree/.gitignore:7:!file*.log    ./dir2/file2.log
gitignore-reader/src/test/resources/testtree/.gitignore:4:*.log ./dir3/dir3.log
gitignore-reader/src/test/resources/testtree/.gitignore:1:*.md  ./dir3/dir3.md
gitignore-reader/src/test/resources/testtree/.gitignore:7:!file*.log    ./dir3/file3.log
gitignore-reader/src/test/resources/testtree/.gitignore:1:*.md  ./README.md
gitignore-reader/src/test/resources/testtree/.gitignore:1:*.md  ./root.md
```

List of ignored files (note: removing the '!' matches):
./dir1/dir1.log
./dir1/dir1.txt
./dir1/file1.log
./dir2/dir2.log
./dir2/dir2.md
./dir3/dir3.log
./dir3/dir3.md
./README.md
./root.md




