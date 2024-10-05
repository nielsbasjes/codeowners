# Git ignore analysis:
$ git check-ignore --no-index --verbose $(find . | sort)
```
enforcer/src/it/testtree/.gitignore:4:*.log     ./dir1/dir1.log
enforcer/src/it/testtree/dir1/.gitignore:2:!dir1.md     ./dir1/dir1.md
enforcer/src/it/testtree/dir1/.gitignore:1:*.txt        ./dir1/dir1.txt
enforcer/src/it/testtree/dir1/.gitignore:3:file1.log    ./dir1/file1.log
enforcer/src/it/testtree/.gitignore:4:*.log     ./dir2/dir2.log
enforcer/src/it/testtree/.gitignore:1:*.md      ./dir2/dir2.md
enforcer/src/it/testtree/.gitignore:7:!file*.log        ./dir2/file2.log
enforcer/src/it/testtree/.gitignore:4:*.log     ./dir3/dir3.log
enforcer/src/it/testtree/.gitignore:1:*.md      ./dir3/dir3.md
enforcer/src/it/testtree/.gitignore:7:!file*.log        ./dir3/file3.log
enforcer/src/it/testtree/.gitignore:10:dir4     ./dir4
enforcer/src/it/testtree/.gitignore:10:dir4     ./dir4/dir4.log
enforcer/src/it/testtree/.gitignore:10:dir4     ./dir4/dir4.md
enforcer/src/it/testtree/.gitignore:10:dir4     ./dir4/dir4.txt
enforcer/src/it/testtree/.gitignore:10:dir4     ./dir4/file4.log
enforcer/src/it/testtree/.gitignore:10:dir4     ./dir4/.gitignore
enforcer/src/it/testtree/.gitignore:10:dir4     ./dir4/subdir
enforcer/src/it/testtree/.gitignore:10:dir4     ./dir4/subdir/dir1.log
enforcer/src/it/testtree/.gitignore:10:dir4     ./dir4/subdir/dir1.md
enforcer/src/it/testtree/.gitignore:10:dir4     ./dir4/subdir/dir1.txt
enforcer/src/it/testtree/.gitignore:10:dir4     ./dir4/subdir/file1.log
enforcer/src/it/testtree/.gitignore:10:dir4     ./dir4/subdir/.gitignore
enforcer/src/it/testtree/.gitignore:10:dir4     ./dir4/subdir/subdir
enforcer/src/it/testtree/.gitignore:10:dir4     ./dir4/subdir/subdir/dir1.log
enforcer/src/it/testtree/.gitignore:10:dir4     ./dir4/subdir/subdir/dir1.md
enforcer/src/it/testtree/.gitignore:10:dir4     ./dir4/subdir/subdir/dir1.txt
enforcer/src/it/testtree/.gitignore:10:dir4     ./dir4/subdir/subdir/file1.log
enforcer/src/it/testtree/.gitignore:10:dir4     ./dir4/subdir/subdir/.gitignore
enforcer/src/it/testtree/.gitignore:1:*.md      ./README.md
enforcer/src/it/testtree/.gitignore:1:*.md      ./root.md
```

List of ignored files (note: removing the '!' matches):
./dir1/dir1.log
./dir1/dir1.txt
./dir1/file1.log
./dir2/dir2.log
./dir2/dir2.md
./dir3/dir3.log
./dir3/dir3.md
./dir4
./dir4/dir4.log
./dir4/dir4.md
./dir4/dir4.txt
./dir4/file4.log
./dir4/.gitignore
./dir4/subdir
./dir4/subdir/dir1.log
./dir4/subdir/dir1.md
./dir4/subdir/dir1.txt
./dir4/subdir/file1.log
./dir4/subdir/.gitignore
./dir4/subdir/subdir
./dir4/subdir/subdir/dir1.log
./dir4/subdir/subdir/dir1.md
./dir4/subdir/subdir/dir1.txt
./dir4/subdir/subdir/file1.log
./dir4/subdir/subdir/.gitignore
./README.md
./root.md
