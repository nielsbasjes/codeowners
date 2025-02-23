/*
 * CodeOwners Tools
 * Copyright (C) 2023-2025 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

fragment SPACE
     : (' '| '\u2002' | '\u0220' |'\t'|'+')
     ;


OPTIONAL    : '^';
BLOCKOPEN   : '[' SPACE* -> channel(HIDDEN), pushMode(SECTION_MODE);

COMMENT
    : '#' ~[\n\r]* EOL -> skip
    ;

SPACES
    : SPACE+ -> skip
    ;

USERID
    : '@'  [a-zA-Z0-9/._-]+ EOL?               // A username or groupname
    | '@@' [a-zA-Z0-9_-]+ EOL?                 // A role name
    | [a-zA-Z0-9_-]+ '@' [a-zA-Z0-9._-]+ EOL?  // An email address
    ;

NEWLINE
    : EOL+ ->skip
    ;


FILEEXPRESSION
    : ('\\ '|'\\#'|[a-zA-Z0-9/*_.-])+
    ;

mode SECTION_MODE;
    SECTION_SPACES : SPACE+ -> skip;
    BLOCKCLOSE      : SPACE* ']' -> channel(HIDDEN), type(BLOCKOPEN), popMode ;
    SECTIONVALUE    : [0-9]+|[a-zA-Z]|~[\]]+;
