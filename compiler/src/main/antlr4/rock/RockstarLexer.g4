lexer grammar RockstarLexer;

fragment A: [Aa];
fragment B: [Bb];
fragment C: [Cc];
fragment D: [Dd];
fragment E: [Ee];
fragment F: [Ff];
fragment G: [Gg];
fragment H: [Hh];
fragment I: [Ii];
fragment J: [Jj];
fragment K: [Kk];
fragment L: [Ll];
fragment M: [Mm];
fragment N: [Nn];
fragment O: [Oo];
fragment P: [Pp];
fragment Q: [Qq];
fragment R: [Rr];
fragment S: [Ss];
fragment T: [Tt];
fragment U: [Uu];
fragment V: [Vv];
fragment W: [Ww];
fragment X: [Xx];
fragment Y: [Yy];
fragment Z: [Zz];

PRONOUNS: I T
        | H E
        | S H E
        | H I M
        | H E R
        | T H E Y
        | T H E M
        | Z E
        | H I R
        | Z I E
        | Z I R
        | X E
        | X E M
        | V E
        | V E R
;

COMMON_VARIABLE_PREFIXES: A
                        | A N
                        | T H E
                        | M Y
                        | Y O U R
                        | O U R
;

CONSTANT_UNDEFINED: M Y S T E R I O U S;
CONSTANT_NULL: N U L L
       | G O N E
       | N O B O D Y
       | N O W H E R E
       | N O T H I N G
;
CONSTANT_TRUE: T R U E
       | R I G H T
       | Y E S
       | O K
;
CONSTANT_FALSE: F A L S E
        | W R O N G
        | N O
        | L I E S
;
CONSTANT_EMPTY: E M P T Y
        | S I L E N T
        | S I L E N C E
;

KW_SAY: S A Y;

KW_PUT: P U T;
KW_LET: L E T;
KW_INTO: I N T O;
KW_BE: B E;
KW_SAYS: S A Y S | S A I D;

KW_TAKING: T A K I N G;
KW_TAKES: T A K E S | W A N T S;

KW_LISTEN: L I S T E N;
KW_TO: T O ;
KW_SHOUT:  S H O U T | W H I S P E R | S C R E A M;

KW_WHILE: W H I L E ;
KW_UNTIL: U N T I L;
KW_IF: I F;
KW_ELSE: E L S E;

KW_BUILD: B U I L D;
KW_UP: U P;
KW_KNOCK: K N O C K;
KW_DOWN: D O W N;

KW_GIVE: G I V E | S E N D | R E T U R N;
KW_BACK: B A C K;

KW_CONTINUE: C O N T I N U E
           | T A K E WS I T WS T O WS T H E WS T O P
;

KW_BREAK: B R E A K (WS I T WS KW_DOWN)?;

KW_NOT: N O T;
KW_MULTIPLY: T I M E S | O F ;
KW_DIVIDE: O V E R | B E T W E E N;
KW_WITH: W I T H;
KW_ADD: P L U S | W I T H;
KW_SUBTRACT: M I N U S | W I T H O U T;

KW_IS: I S | A R E | W A S | W E R E;
KW_NOT_EQUAL: I S N SINGLE_QUOTE* T | A I N SINGLE_QUOTE* T;
KW_THAN: T H A N;
KW_AS: A S;

KW_GREATER: H I G H E R | G R E A T E R | B I G G E R | S T R O N G E R;
KW_LESS: L O W E R | L E S S | S M A L L E R | W E A K E R;
KW_GREATER_EQUAL: H I G H | G R E A T | B I G | S T R O N G;
KW_LESS_EQUAL: L O W | L I T T L E | S M A L L | W E A K;

KW_AND: A N D;
KW_OR: O R;
KW_NOR: N O R;

KW_TURN: T U R N;
KW_ROUND: R O U N D | A R O U N D;

KW_ROCK: R O C K | P U S H;
KW_ROLL: R O L L | P O P;
KW_AT: A T;

KW_SPLIT: C U T | S P L I T | S H A T T E R;

KW_CAST: C A S T | B U R N;

KW_JOIN: J O I N | U N I T E;


PROPER_NOUN: [A-Z][A-Z|a-z]*;
WORD: [a-z|A-Z]+;

NUMERIC_LITERAL: HYPHEN?[0-9]+ ('.' [0-9]+)?;
STRING_LITERAL: '"' .*? '"';

NL: [,. ]* '\r'? '\n';
DOT: '.';
COMMA: ',';
QUESTION_MARK: '?';
EXCLAMATION_MARK: '!';
AMPERSAND: '&';
HYPHEN: '-';
ASTERISK: '*';
SLASH: '/';
PLUS_SIGN: '+';
APOSTROPHE_S: '\'s';
APOSTROPHE_RE: '\'re';
APOSTROPHED_N: '\'n\'';
SINGLE_QUOTE: '\'';
IGNORED: [,;] -> skip;
WS: [ \t]+?;
COMMENT: WS* '(' .*? ')' WS* -> skip;