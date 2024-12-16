grammar Program;


program : (variableDeclaration ';' | functionDeclaration)*;

scope: '{' statement* '}';

functionDeclaration : type IDENT '(' ')' scope;
simpleStatement:
               variableDeclaration
               | assignment
               | expression;
statement:
         ( simpleStatement
         | return
         ) ';'
         | scope
         | if
         | while;

type: IDENT;

singleVariableDeclaration: '*'* IDENT ('=' expression)?;
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

unaryOperator: '-' | '&' | '*';

expression:
          unaryOperator expression
          | expression ( '*' | '/' ) expression
          | expression ( '+' | '-' ) expression
          | '(' expression ')'
          | expression '(' (expression (',' expression)*)? ')'
          | primary
          ;

primary: IDENT | literal;

literal: INT | STRING;

INT: [0-9]+;
STRING: '"' (ESC | ~[\\"\n\r])* '"';
ESC: '\\"' | '\\n' | '\\t' | '\\r';
IDENT: [a-zA-Z] [a-zA-Z0-9]*;
WS: [ \t\n\r] -> skip;
