lexer grammar CodeOwnersLexer;

// Gitlab supports Sections (the ^ means approval is OPTIONAL)
// ^[SectionName] [NumberOfApprovers] @users

// [Documentation][2] @docs-team
// *.md @tech-writer-team
// docs/
// README.md
//
// [Ruby]
// *.rb @dev-team
//
// ^[Database] @database-team
// model/db/
// config/db/database-setup.md @docs-team

// ^[Java][2] @java-team
// src/**/java/
// src/main/java/README.md @docs-team

fragment EOL
    : '\r'? '\n' | '\r'
    ;

OPTIONAL    : '^';
BLOCKOPEN   : '[' -> channel(HIDDEN), pushMode(SECTION_MODE);

COMMENT
    : '#' ~[\n\r]+ EOL -> skip
    ;

SPACE
    : (' '| '\u2002' | '\u0220' |'\t'|'+') -> skip
    ;

USERID
    : [a-zA-Z]* '@' [a-zA-Z0-9/.-]+ EOL?
    ;

NEWLINE
    : EOL+ ->skip
    ;


FILEEXPRESSION
    : [a-zA-Z0-9/*_.-]+
    ;

mode SECTION_MODE;
    BLOCKCLOSE      : ']' -> channel(HIDDEN), type(BLOCKOPEN), popMode ;
    SECTIONVALUE    : [0-9]+|[a-zA-Z]|[a-zA-Z0-9][ a-zA-Z0-9.-]*[a-zA-Z0-9];
