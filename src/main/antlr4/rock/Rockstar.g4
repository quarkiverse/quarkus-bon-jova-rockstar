parser grammar Rockstar;

options { tokenVocab=RockstarLexer; }

program: (NL|WS)* (statementList | functionDeclaration)* WS*;

statementList: statement+;

statement: (ifStmt | inputStmt | outputStmt | assignmentStmt | incrementStmt | decrementStmt | loopStmt | returnStmt | continueStmt | breakStmt) (NL+?|EOF);

expression: functionCall
          | lhe=expression WS op=(KW_MULTIPLY|KW_DIVIDE) WS rhe=expression
          | lhe=expression WS op=(KW_ADD|KW_SUBTRACT) WS rhe=expression
          | lhe=expression WS comparisionOp WS rhe=expression
          | lhe=expression WS op=(KW_AND|KW_OR|KW_NOR) WS rhe=expression
          | (literal|variable|constant)
;

functionCall: functionName=variable WS KW_TAKING WS argList;

argList: (variable|literal) ((WS KW_AND|COMMA) WS (variable|literal))*;

functionDeclaration: functionName=variable WS KW_TAKES WS paramList NL statementList NL;

paramList: variable ((COMMA? WS KW_AND WS | COMMA | AMPERSAND | APOSTROPHED_N) variable)*;

comparisionOp: KW_IS
             | KW_NOT_EQUAL
             | KW_IS WS (KW_GREATER|KW_LESS) WS KW_THAN
             | KW_IS WS KW_AS WS (KW_GREATER_EQUAL|KW_LESS_EQUAL) WS KW_AS
;

assignmentStmt: variable (APOSTROPHE_S | WS (KW_IS|KW_WAS_WERE)) WS (poeticNumberLiteral|constant|literal)
              | KW_LET WS variable WS KW_BE WS expression
              | KW_PUT WS expression WS KW_INTO WS variable
              | variable WS (KW_SAYS | KW_SAY) WS poeticStringLiteral
;

inputStmt: KW_LISTEN WS KW_TO WS variable;

outputStmt: (KW_SHOUT | KW_SAY) WS expression;

ifStmt: KW_IF WS expr=expression NL statementList (KW_ELSE NL statementList)?;

loopStmt: KW_LOOP WS expr=expression NL statementList;

incrementStmt: KW_BUILD WS variable WS ups;

ups: KW_UP (COMMA WS KW_UP)*;

decrementStmt: KW_KNOCK WS variable WS downs;

downs: KW_DOWN (COMMA WS KW_DOWN)*;

returnStmt: KW_GIVE WS KW_BACK WS expression;

continueStmt: KW_CONTINUE;

breakStmt: KW_BREAK;

constant: CONSTANT_UNDEFINED
        | CONSTANT_NULL
        | CONSTANT_TRUE
        | CONSTANT_FALSE
        | CONSTANT_EMPTY
;

literal: NUMERIC_LITERAL | STRING_LITERAL;

variable: COMMON_VARIABLE_PREFIXES WS WORD
        | WORD
        | PROPER_NOUN (WS PROPER_NOUN)*
        | PRONOUNS
;

poeticNumberLiteral: poeticNumberLiteralWord poeticNumberLiteralGarbage* poeticNumberLiteralDecimalSeparator? (WS poeticNumberLiteralGarbage* poeticNumberLiteralWord poeticNumberLiteralGarbage* poeticNumberLiteralDecimalSeparator?)*;

poeticNumberLiteralGarbage: COMMA
;

poeticNumberLiteralWord: COMMON_VARIABLE_PREFIXES
                       | allKeywords
                       | PRONOUNS
                       | WORD
                       | PROPER_NOUN
;

poeticNumberLiteralDecimalSeparator: DOT;

poeticStringLiteral: poeticStringLiteralWord poeticStringLiteralGarbage* (WS poeticStringLiteralGarbage* poeticStringLiteralWord poeticStringLiteralGarbage*)*;

poeticStringLiteralGarbage: DOT
                          | COMMA
                          | QUESTION_MARK
                          | EXCLAMATION_MARK
                          | AMPERSAND
;

poeticStringLiteralWord: COMMON_VARIABLE_PREFIXES
                       | PRONOUNS
                       | CONSTANT_UNDEFINED
                       | CONSTANT_NULL
                       | CONSTANT_TRUE
                       | CONSTANT_FALSE
                       | allKeywords
                       | WORD
                       | PROPER_NOUN
;

allKeywords: KW_PUT
           | KW_INTO
           | KW_SAYS
           | KW_TAKING
           | KW_TAKES
           | KW_LISTEN
           | KW_TO
           | KW_SAY
           | KW_SHOUT
           | KW_LOOP
           | KW_IF
           | KW_ELSE
           | KW_BUILD
           | KW_UP
           | KW_KNOCK
           | KW_DOWN
           | KW_GIVE
           | KW_BACK
           | KW_CONTINUE
           | KW_BREAK
           | KW_NOT
           | KW_MULTIPLY
           | KW_DIVIDE
           | KW_ADD
           | KW_SUBTRACT
           | KW_IS
           | KW_NOT_EQUAL
           | KW_WAS_WERE
           | KW_THAN
           | KW_AS
           | KW_GREATER
           | KW_LESS
           | KW_GREATER_EQUAL
           | KW_LESS_EQUAL
           | KW_AND
           | KW_OR
           | KW_NOR
;