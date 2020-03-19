start: eqn;

// Rules
eqn		: lefthand '=' righthand ;
lefthand	: '\frac{' numerator '}{' denominator '}' | numerator '/' denominator ;
numerator	: statevariable;
denominator	: variable;
righthand	: expression;
expression	: eventrate | '-' negeventrate | expression '\+' eventrate | expression '-' negeventrate;
eventrate	: rate ;
negeventrate	: rate ;
rate		: variable | '\(' rate '\)' | '\(' sum '\)' | rate mult rate | rate div rate ;
sum		: '\(' sum '\)' | sum add sum | sum sub sum | variable | sum mult sum | sum div sum ;

// Tokens
mult: '\*';
div: '/';
add: '\+';
sub: '-';
statevariable: '\w+';
variable: '\w+';
number: '[\d.]+';

WS: '[ \t]+' (%ignore);
