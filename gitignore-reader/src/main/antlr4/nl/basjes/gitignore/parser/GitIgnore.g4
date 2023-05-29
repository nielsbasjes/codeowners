/*
 * CodeOwners Tools
 * Copyright (C) 2023 Niels Basjes
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

grammar GitIgnore;

gitignore
    : configLine+
    ;

configLine
    : not=NOT? fileExpression=FILEEXPRESSION #ignoreRule
    | COMMENT? NEWLINE+ #comment
    ;


fragment EOL
    : '\r'? '\n' | '\r'
    ;

NOT
    : '!'
    ;

COMMENT
    : '#' ~[\n\r]* EOL -> skip
    ;

SPACE
    : (' '| '\u2002' | '\u0220' |'\t'|'+') -> skip
    ;

NEWLINE
    : EOL+ ->skip
    ;

FILEEXPRESSION
    : ('\\ '|'\\#'|[a-zA-Z0-9/*_.\\[\]?-])+('\\ '|'\\#'|[a-zA-Z0-9/*_.\\[\]?!-])+
    ;
