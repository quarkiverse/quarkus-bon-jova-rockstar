parser grammar Rockstar;

options { tokenVocab=RockstarLexer; }

program: (NL|ws)* (statementList | functionDeclaration)* ws*;

statementList: statement+;

statement: ws* (ifStmt | inputStmt | outputStmt | assignmentStmt | incrementStmt | decrementStmt | loopStmt | returnStmt | continueStmt | breakStmt) (NL+?|EOF);

expression: functionCall
          | lhe=expression ws op=(KW_MULTIPLY|KW_DIVIDE) ws rhe=expression
          | lhe=expression ws op=(KW_ADD|KW_SUBTRACT) ws rhe=expression
          | lhe=expression ws op=(PLUS_SIGN|HYPHEN) ws rhe=expression
          | lhe=expression ws op=(ASTERISK|SLASH) ws rhe=expression
          | lhe=expression ws comparisionOp ws rhe=expression
          | lhe=expression contractedComparisionOp ws rhe=expression
          | lhe=expression ws op=(KW_AND|KW_OR|KW_NOR) ws rhe=expression
          | (literal|variable|constant)
;

functionCall: functionName=variable WS KW_TAKING WS argList;

argList: (variable|literal) ((WS KW_AND|COMMA) WS (variable|literal))*;

functionDeclaration: functionName=variable WS KW_TAKES WS paramList NL statementList NL;

paramList: variable ((COMMA? WS KW_AND WS | COMMA | AMPERSAND | APOSTROPHED_N) variable)*;

assignmentStmt: variable (APOSTROPHE_S | APOSTROPHE_RE | ws (KW_IS|KW_WAS_WERE)) ws (poeticNumberLiteral|constant|literal)
              | KW_LET ws variable ws KW_BE ws expression
              | KW_PUT ws expression ws KW_INTO ws variable
              | variable ws (KW_SAYS | KW_SAY) WS poeticStringLiteral
;

comparisionOp: KW_IS
             | KW_NOT_EQUAL
             | KW_IS ws (KW_GREATER|KW_LESS) ws KW_THAN
             | KW_IS ws KW_AS ws (KW_GREATER_EQUAL|KW_LESS_EQUAL) ws KW_AS
;

contractedComparisionOp: APOSTROPHE_S;

inputStmt: KW_LISTEN ws KW_TO ws variable;

outputStmt: (KW_SHOUT | KW_SAY) ws expression;

ifStmt: KW_IF ws expr=expression NL statementList  (ws* KW_ELSE NL? statementList)?;

loopStmt: (KW_WHILE | KW_UNTIL) ws expr=expression NL statementList;

incrementStmt: KW_BUILD ws variable ws ups;

ups: KW_UP (COMMA? ws KW_UP)*;

decrementStmt: KW_KNOCK ws variable ws downs;

downs: KW_DOWN (COMMA? ws KW_DOWN)*;

returnStmt: KW_GIVE ws KW_BACK ws expression;

continueStmt: KW_CONTINUE;

breakStmt: KW_BREAK;

ws: WS+;

constant: CONSTANT_UNDEFINED
        | CONSTANT_NULL
        | CONSTANT_TRUE
        | CONSTANT_FALSE
        | CONSTANT_EMPTY
;

literal: NUMERIC_LITERAL | STRING_LITERAL;

variable: COMMON_VARIABLE_PREFIXES ws WORD
        | WORD
        | WORD_WITH_AP
        | PROPER_NOUN (ws PROPER_NOUN)*
        | PRONOUNS
;

poeticNumberLiteral: poeticNumberLiteralWord poeticNumberLiteralGarbage* poeticNumberLiteralDecimalSeparator? (ws poeticNumberLiteralGarbage* ws* poeticNumberLiteralWord poeticNumberLiteralGarbage* poeticNumberLiteralDecimalSeparator?)*;

poeticNumberLiteralGarbage: ws* (COMMA | EXCLAMATION_MARK | QUESTION_MARK | PLUS_SIGN | AMPERSAND | SINGLE_QUOTE) ws*
;

poeticNumberLiteralWord: poeticNumberLiteralWord HYPHEN poeticNumberLiteralWord
                       | poeticNumberLiteralWord SINGLE_QUOTE poeticNumberLiteralWord?
                       | poeticNumberLiteralWord APOSTROPHE_S poeticNumberLiteralWord?
                       | poeticNumberLiteralWord APOSTROPHE_RE poeticNumberLiteralWord?
                       | poeticNumberLiteralWord APOSTROPHED_N poeticNumberLiteralWord?
                       | SINGLE_QUOTE poeticNumberLiteralWord
                       | COMMON_VARIABLE_PREFIXES
                       | allKeywords
                       | PRONOUNS
                       | WORD
                       | PROPER_NOUN
;

poeticNumberLiteralDecimalSeparator: DOT;

poeticStringLiteral: ws* poeticStringLiteralWord poeticStringLiteralGarbage* (ws poeticStringLiteralGarbage* poeticStringLiteralWord poeticStringLiteralGarbage*)*;

poeticStringLiteralGarbage: DOT
                          | COMMA
                          | QUESTION_MARK
                          | EXCLAMATION_MARK
                          | AMPERSAND
                          | PLUS_SIGN
                          | SINGLE_QUOTE
;

poeticStringLiteralWord: COMMON_VARIABLE_PREFIXES
                       | PRONOUNS
                       | CONSTANT_UNDEFINED
                       | CONSTANT_NULL
                       | CONSTANT_TRUE
                       | CONSTANT_FALSE
                       | allKeywords
                       | WORD
                       | WORD_WITH_QUOTES
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
           | KW_WHILE
           | KW_UNTIL
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