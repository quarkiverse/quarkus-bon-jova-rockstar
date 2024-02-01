parser grammar Rockstar;

options { tokenVocab=RockstarLexer; }

program: (NL|ws)* (statementList | functionDeclaration)* ws*;

statementList: statement+;

statement: ws* (ifStmt | inputStmt | outputStmt | assignmentStmt | roundingStmt | incrementStmt | decrementStmt | loopStmt | arrayStmt | stringStmt | castStmt | joinStmt | continueStmt | breakStmt) (NL+?|EOF);


expression: functionCall
          | lhe=expression ws op=(KW_MULTIPLY|KW_DIVIDE) ws rhe=expression extraExpressions?
          | lhe=expression ws op=(KW_ADD|KW_WITH|KW_SUBTRACT) ws rhe=expression extraExpressions?
          | lhe=expression ws op=(PLUS_SIGN|HYPHEN) ws rhe=expression extraExpressions?
          | lhe=expression ws* op=(ASTERISK|SLASH) ws* rhe=expression extraExpressions?
          | lhe=expression ws comparisionOp ws rhe=expression
          | lhe=expression contractedComparisionOp ws rhe=expression
          | lhe=expression ws op=(KW_AND|KW_OR|KW_NOR) ws rhe=expression extraExpressions?
          | KW_NOT ws rhe=expression
          | variable ws KW_AT ws expression
          | KW_ROLL ws variable
          | (literal|variable|constant)
;

extraExpressions: (COMMA ws (KW_AND ws)? expression)+;

list: (expression) (COMMA ws expression)*;

arrayStmt: KW_ROCK ws variable
       | KW_ROCK ws list ws KW_INTO ws variable
       | KW_ROCK ws variable ws KW_WITH ws list
       | KW_LET ws variable ws KW_AT ws expression ws KW_BE ws expression
;

stringStmt: KW_SPLIT ws expression?? ws KW_WITH ws expression
        | KW_SPLIT ws expression?? (ws KW_INTO ws variable)? (ws KW_WITH ws expression)?
        | KW_SPLIT ws expression?? (ws KW_WITH ws expression)? (ws KW_INTO ws variable)?
;

functionCall: functionName=variable WS KW_TAKING WS argList;


// the second set of entries here isn't just an expression, to try and force the shortest match, rather than the longest match
argList: (expression) ((WS KW_AND|COMMA|WS AMPERSAND|WS APOSTROPHED_N) WS (literal|variable|constant|expression))*;

functionDeclaration: functionName=variable WS KW_TAKES WS paramList NL statementList? returnStmt (NL+?|EOF);

paramList: variable ((COMMA? WS* KW_AND WS | WS COMMA WS | WS AMPERSAND WS | WS APOSTROPHED_N WS) variable)*;

assignmentStmt: variable (APOSTROPHE_S | APOSTROPHE_RE | ws KW_IS) ws (poeticNumberLiteral|constant|literal)
              | KW_LET ws variable ws KW_BE ws expression
              | KW_PUT ws expression ws KW_INTO ws variable
              | variable ws (KW_SAYS | KW_SAY) WS poeticStringLiteral
              | KW_ROLL ws variable ws KW_INTO ws variable
;

roundingStmt: KW_TURN ws variable ws (KW_ROUND | KW_UP | KW_DOWN)
            | KW_TURN ws (KW_ROUND | KW_UP | KW_DOWN) ws variable
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

castStmt: KW_CAST ws expression (ws KW_INTO ws variable)? ws KW_WITH ws expression
        | KW_CAST ws expression (ws KW_INTO ws variable)?
;

joinStmt: KW_JOIN ws variable (ws KW_INTO ws variable)? (ws KW_WITH ws expression)?;

returnStmt: KW_GIVE (ws KW_BACK)? ws expression (ws KW_BACK)?;

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

poeticNumberLiteral: poeticNumberLiteralLeadingWord poeticNumberLiteralGarbage* poeticNumberLiteralDecimalSeparator? (ws poeticNumberLiteralGarbage* ws* poeticNumberLiteralWord poeticNumberLiteralGarbage* poeticNumberLiteralDecimalSeparator?)*;

poeticNumberLiteralGarbage: ws* (COMMA | EXCLAMATION_MARK | QUESTION_MARK | PLUS_SIGN | AMPERSAND | SINGLE_QUOTE) ws*
;

poeticNumberLiteralLeadingWord: poeticNumberLiteralLeadingWord HYPHEN poeticNumberLiteralLeadingWord
                       | poeticNumberLiteralLeadingWord SINGLE_QUOTE poeticNumberLiteralLeadingWord?
                       | poeticNumberLiteralLeadingWord APOSTROPHE_S poeticNumberLiteralLeadingWord?
                       | poeticNumberLiteralLeadingWord APOSTROPHE_RE poeticNumberLiteralLeadingWord?
                       | poeticNumberLiteralLeadingWord APOSTROPHED_N poeticNumberLiteralLeadingWord?
                       | SINGLE_QUOTE poeticNumberLiteralLeadingWord
                       | COMMON_VARIABLE_PREFIXES
                       | allKeywords
                       | PRONOUNS
                       | WORD
                       | PROPER_NOUN
;


poeticNumberLiteralWord: poeticNumberLiteralLeadingWord
                        | poeticNumberLiteralWord HYPHEN poeticNumberLiteralWord
                         | poeticNumberLiteralWord SINGLE_QUOTE poeticNumberLiteralWord?
                         | poeticNumberLiteralWord APOSTROPHE_S poeticNumberLiteralWord?
                         | poeticNumberLiteralWord APOSTROPHE_RE poeticNumberLiteralWord?
                         | poeticNumberLiteralWord APOSTROPHED_N poeticNumberLiteralWord?
                       | allConstants
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
                       | allConstants
                       | allKeywords
                       | WORD
                       | WORD_WITH_QUOTES
                       | PROPER_NOUN
;

allConstants:  CONSTANT_UNDEFINED
                       | CONSTANT_NULL
                       | CONSTANT_TRUE
                       | CONSTANT_FALSE
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
           | KW_WITH
           | KW_SUBTRACT
           | KW_IS
           | KW_NOT_EQUAL
           | KW_THAN
           | KW_AS
           | KW_GREATER
           | KW_LESS
           | KW_GREATER_EQUAL
           | KW_LESS_EQUAL
           | KW_AND
           | KW_OR
           | KW_NOR
           | KW_CAST
           | KW_ROUND
;