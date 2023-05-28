grammar CodeOwners;

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

options { tokenVocab=CodeOwnersLexer; }

codeowners
    : configLine+
    ;

configLine
    : optional=OPTIONAL? section=SECTIONVALUE approvers=SECTIONVALUE? defaultApprovers=USERID* #section
    | fileExpression=FILEEXPRESSION USERID* #approvalRule
    | COMMENT? NEWLINE+ #comment
    ;
