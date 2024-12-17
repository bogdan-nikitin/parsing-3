grammar Program;


program : (variableDeclaration ';' | functionDeclaration)*;

scope: '{' statement* '}';

functionDeclaration : type IDENT '(' arguments ')' scope;

arguments: (argumentDeclaration (',' argumentDeclaration)*)?;
argumentDeclaration: type singleVariableDeclaration;

simpleStatement:
               variableDeclaration
               | assignment
               | expression;

oneLineStatement: simpleStatement | return;

statement:
         oneLineStatement ';'
         | scope
         | if
         | while;

type: IDENT;

declarators: ('*'|'&')*;

singleVariableDeclaration: declarators IDENT ('=' expression)?;
variableDeclaration: type singleVariableDeclaration (',' singleVariableDeclaration)*;

assignment: expression assignmentOperator expression;
assignmentOperator:
                  '+='
                  | '-='
                  | '*='
                  | '/=';
return: 'return' expression;
if: 'if' '(' expression ')' statement ('else' statement)?;
while: 'while' '(' expression ')' statement;
for: 'for' '(' simpleStatement ';' simpleStatement ';' simpleStatement ')' statement;

prefixOperator: '-' | '&' | '*' | '++' | '--';

expression:
          expression ('++' | '--')
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

literal: INT | STRING;

INT: [0-9]+;
STRING: '"' (ESC | ~[\\"\n\r])* '"';
ESC: '\\"' | '\\n' | '\\t' | '\\r';
IDENT: [a-zA-Z_] [a-zA-Z0-9_]*;
WS: [ \t\n\r] -> skip;
