grammar Program;


program : (variableDeclaration ';' | functionDeclaration | preprocessor)*;

scope: '{' statement* '}';

functionDeclaration : type IDENT '(' arguments ')' scope;

arguments: (argumentDeclaration (',' argumentDeclaration)*)?;
argumentDeclaration: type declarators singleVariableDeclaration;

simpleStatement:
               variableDeclaration
               | assignment
               | expression;

oneLineStatement: simpleStatement | return;

statement:
         oneLineStatement ';'
         | scope
         | if
         | while
         | for;

type:
    'const'?
    ( 'long long'
    | 'long int'
    | 'long double'
    | 'unsigned int'
    | 'unsigned char'
    | 'unsigned short'
    | 'unsigned long'
    | 'unsigned long long'
    | 'signed int'
    | 'signed char'
    | 'signed short'
    | 'signed long'
    | 'signed long long'
    | IDENT
    );


declarators: ('*'|'&')*;

singleVariableDeclaration: declarators IDENT ('=' expression)?;
variableDeclaration: type singleVariableDeclaration (',' singleVariableDeclaration)*;

assignment: expression assignmentOperator expression;
assignmentOperator:
                  '='
                  | '+='
                  | '-='
                  | '*='
                  | '/=';
return: 'return' (expression?);
if: 'if' '(' expression ')' statement ('else' statement)?;
while: 'while' '(' expression ')' statement;
for: 'for' '(' simpleStatement ';' simpleStatement ';' simpleStatement ')' statement;

prefixOperator: '-' | '&' | '*' | '++' | '--';
postfixOperator: '++' | '--';

expression:
          expression postfixOperator
          | prefixOperator expression
          | expression ( '*' | '/' ) expression
          | expression ( '+' | '-' ) expression
          | expression ( '<' | '>' | '<=' | '>=' ) expression
          | expression ( '==' | '!=' ) expression
          | '(' expression ')'
          | expression '(' (expression (',' expression)*)? ')'
          | primary
          ;

name: IDENT;
primary: name | literal;

literal: INT | STRING | CHAR;

preprocessor: PREPROCESSOR;

INT: [0-9]+;
CHAR: '\'' (ESC | ~[\\'\n\r]) '\'';
STRING: '"' (ESC | ~[\\"\n\r])* '"';
ESC: '\\' ('"' | 'n' | 't' | 'r' | '\'' | '0');
IDENT: [a-zA-Z_] [a-zA-Z0-9_]*;
WS: [ \t\n\r]+ -> skip;
PREPROCESSOR: '#' (~[\n\r])+;
COMMENT: '//' ~[\n\r]* -> skip;
MULTILINE_COMMENT: '/*' .*? '*/' -> skip;
