package src.parser.parser;

import src.lexer.token.*;
import src.parser.ast.SymbolTable;
import src.parser.ast.nodes.ExpressionNode;
import src.parser.ast.nodes.expression.*;
import src.parser.ast.nodes.expression.access.ArrayAccessExpression;
import src.parser.ast.nodes.expression.access.MemberAccessExpression;
import src.parser.ast.nodes.expression.literal.BoolLiteralExpression;
import src.parser.ast.nodes.expression.literal.CharLiteralExpression;
import src.parser.ast.nodes.expression.literal.IdentifierLiteralExpression;
import src.parser.ast.nodes.expression.literal.NullLiteralExpression;
import src.parser.ast.nodes.expression.literal.NumberLiteralExpression;
import src.parser.ast.nodes.expression.literal.StringLiteralExpression;
import src.parser.parser.util.ParseException;
import src.parser.parser.util.ParserBase;
import src.parser.parser.util.ParserState;
import src.token.family.Delimiter;
import src.token.family.Keyword;
import src.token.family.Literal;
import src.token.family.Operator;
import java.util.ArrayList;

/// Parser for expressions, handling operator precedence and associativity. It constructs an AST for expressions based
/// on the defined grammar.
/// The parser supports various expression types, including literals, identifiers, binary operations, unary operations,
/// function calls, member access, and array access. It also handles assignment expressions with proper validation of assignment targets.
///
/// <hr>
///
/// GRAMMAR FOR EXPRESSIONS
///
/// ```
/// - expression → assignment
/// - assignment → (lValue) "=" assignment | logicOr
///     where lValue is: IDENTIFIER | arrayAccess | memberAccess
/// - logicOr    → logicAnd ("||" logicAnd)*
/// - logicAnd   → equality ("&&" equality)*
/// - equality   → comparison (("==" | "!=") comparison)*
/// - comparison → term (("<" | ">" | "<=" | ">=") term)*
/// - term       → factor (("+" | "-") factor)*
/// - factor     → unary (("*" | "/" | "%") unary)*
/// - unary      → ("!" | "-" | "++" | "--") unary | call
/// - call       → primary (postfixOp)*
///     where postfixOp is: "(" arguments? ")" | "." IDENTIFIER | "[" expression "]" | "++" | "--"
/// - primary    → NUMBER | STRING | "true" | "false" | IDENTIFIER | "(" expression ")"
/// ```
///
/// TOKEN DEFINITIONS
///
/// ```
/// - IDENTIFIER: A sequence starting with a letter or underscore, followed by any combination of letters, digits, or underscores.
///   Pattern: [a-zA-Z_][a-zA-Z0-9_]*
///   Example: myVariable, _private, x1
///
/// - NUMBER: A sequence of digits with an optional decimal point, possibly followed by a type suffix (l, f, d).
///   Pattern: \d+(\.\d+)?([lLfFdD])?
///   Example: 42, 3.14, 100L, 2.5f, 1.0d
///
/// - STRING: A sequence of characters enclosed in double quotes, with escape sequences supported.
///   Pattern: "([^"\\]|\\.)*"
///   Example: "hello", "line 1\nline 2"
///
/// - true, false: Boolean literal keywords, recognized as KEYWORDS by the Lexer.
///
/// - OPERATORS: Single or two-character operators recognized by the Lexer.
///   Example: +, -, *, /, ==, !=, &&, ||, ++, --, =, etc.
///
/// - DELIMITERS: Single or two-character delimiters recognized by the Lexer.
///   Example: (, ), [, ], {, }, ., ,, :, etc.
/// ```
///
/// The parser implements recursive descent parsing, where each method corresponds to a production rule in the grammar.
/// It ensures correct operator precedence and associativity while constructing the AST for expressions.
public class ExpressionParser extends ParserBase {

    /// Constructs a new ExpressionParser with the given parser state and symbol table.
    /// @param state The current state of the parser, including the list of tokens and the current position.
    /// @param symbolTable The symbol table for managing variable and function scopes during parsing.
    public ExpressionParser(ParserState state, SymbolTable symbolTable) { super(state, symbolTable); }

    /// Parses an expression according to the defined grammar and returns the corresponding AST node.
    ///
    /// Grammar rule: `expression → assignment`
    /// @return An ExpressionNode representing the parsed expression in the abstract syntax tree (AST).
    public ExpressionNode parseExpression() { return assignment(); }

     /// Parses an assignment expression, handling both simple assignments and compound assignments.
     ///
     /// Grammar rule: `assignment → (lValue) ("=" | "+=" | "-=" | "*=" | "/=") assignment | logicOr`,
     /// where `lValue` can be an identifier, array access, or member access.
     /// @return An ExpressionNode representing the parsed assignment expression in the AST, or a logical OR expression if no assignment is present.
     private ExpressionNode assignment() {

         var expr = logicOr();
         if (match(Operator.ASSIGN, Operator.PLUS_ASSIGN, Operator.MINUS_ASSIGN, Operator.MULTIPLY_ASSIGN, Operator.DIVIDE_ASSIGN)) {

             var operator = previous();
             var value = assignment();

             // Valid assignment targets: identifier, array access, member access
             if (!(expr instanceof IdentifierLiteralExpression ||
                 expr instanceof ArrayAccessExpression ||
                 expr instanceof MemberAccessExpression))
                 throw new ParseException("Invalid assignment target", operator);

             // For compound assignments, convert them to binary operations
             ExpressionNode assignmentValue = value;
             if (operator.getType() instanceof Operator op && op != Operator.ASSIGN) {

                 // Map compound operators to their base binary operators
                 Operator binaryOp = switch (op) {
                     case PLUS_ASSIGN -> Operator.PLUS;
                     case MINUS_ASSIGN -> Operator.MINUS;
                     case MULTIPLY_ASSIGN -> Operator.MULTIPLY;
                     case DIVIDE_ASSIGN -> Operator.DIVIDE;
                     default -> throw new ParseException("Unknown compound assignment operator", operator);
                 };

                 // Create binary expression: target += value becomes target = target + value
                 assignmentValue = new BinaryExpression(
                     operator.getLine(),
                     operator.getColumn(),
                     expr,
                     new OperatorToken(binaryOp, operator.getLine(), operator.getColumn()),
                     value
                 );
             }

             return new AssignmentExpression(
                 operator.getLine(),
                 operator.getColumn(),
                 expr,
                 assignmentValue
             );
         }

         return expr;
     }

    /// Parses logical OR expressions, handling the "||" operator with left associativity.
    ///
    /// Grammar rule: `logicOr → logicAnd ("||" logicAnd)*`
    /// @return An ExpressionNode representing the parsed logical OR expression in the AST.
    private ExpressionNode logicOr() {

        var expr = logicAnd();
        while (match(Operator.LOGICAL_OR)) {

            var operator = previous();
            var right = logicAnd();
            expr = new BinaryExpression(expr.getLine(), expr.getColumn(), expr, (OperatorToken) operator, right);
        }
        return expr;
    }

    /// Parses logical AND expressions, handling the "&&" operator with left associativity.
    ///
    /// Grammar rule: `logicAnd → equality ("&&" equality)*`
    /// @return An ExpressionNode representing the parsed logical AND expression in the AST.
    private ExpressionNode logicAnd() {

        var expr = equality();
        while (match(Operator.LOGICAL_AND)) {

            var operator = previous();
            var right = equality();
            expr = new BinaryExpression(expr.getLine(), expr.getColumn(), expr, (OperatorToken) operator, right);
        }
        return expr;
    }

    /// Parses equality expressions, handling the "==" and "!=" operators with left associativity.
    ///
    /// Grammar rule: `equality → comparison (("==" | "!=") comparison)*`
    /// @return An ExpressionNode representing the parsed equality expression in the AST.
    private ExpressionNode equality() {

        var expr = comparison();
        while (match(Operator.EQUAL, Operator.NOT_EQUAL)) {

            var operator = previous();
            var right = comparison();
            expr = new BinaryExpression(expr.getLine(), expr.getColumn(), expr, (OperatorToken) operator, right);
        }
        return expr;
    }

    /// Parses comparison expressions, handling the "<", ">", "<=", and ">=" operators with left associativity.
    ///
    /// Grammar rule: `comparison → term (("<" | ">" | "<=" | ">=") term)*`
    /// @return An ExpressionNode representing the parsed comparison expression in the AST.
    private ExpressionNode comparison() {

        var expr = term();
        while (match(Operator.LESS_THAN, Operator.GREATER_THAN, Operator.LESS_EQUAL, Operator.GREATER_EQUAL)) {

            var operator = previous();
            var right = term();
            expr = new BinaryExpression(expr.getLine(), expr.getColumn(), expr, (OperatorToken) operator, right);
        }
        return expr;
    }

    /// Parses term expressions, handling the "+" and "-" operators with left associativity.
    ///
    /// Grammar rule: `term → factor (("+" | "-") factor)*`
    /// @return An ExpressionNode representing the parsed term expression in the AST.
    private ExpressionNode term() {

        var expr = factor();
        while (match(Operator.PLUS, Operator.MINUS)) {

            var operator = previous();
            var right = factor();
            expr = new BinaryExpression(expr.getLine(), expr.getColumn(), expr, (OperatorToken) operator, right);
        }
        
        return expr;
    }

     /// Parses factor expressions, handling the "*", "/", and "%" operators with left associativity.
     ///
     /// Grammar rule: `factor → unary (("*" | "/" | "%") unary)*`
     /// @return An ExpressionNode representing the parsed factor expression in the AST.
     private ExpressionNode factor() {

         var expr = unary();
         while (match(Operator.MULTIPLY, Operator.DIVIDE, Operator.MODULO)) {

             var operator = previous();
             var right = unary();
             expr = new BinaryExpression(expr.getLine(), expr.getColumn(), expr, (OperatorToken) operator, right);
         }

         return expr;
    }

    /// Parses unary expressions, handling the "!", "-", "++", and "--" operators with right associativity.
    ///
    /// Grammar rule: `unary → ("!" | "-" | "++" | "--") unary | call`
    /// @return An ExpressionNode representing the parsed unary expression in the AST, or a call expression if no unary operator is present.
    private ExpressionNode unary() {

        if (match(Operator.NOT, Operator.MINUS, Operator.INCREMENT, Operator.DECREMENT)) {

            var operator = previous();
            var right = unary();
            return new UnaryExpression(operator.getLine(), operator.getColumn(), (OperatorToken) operator, right);
        }
        return call();
    }

    /// Parses call expressions, handling function calls, member access, array access, and postfix increment/decrement.
    ///
    /// Grammar rule: `call → primary (postfixOp)*`, where `postfixOp` can be a function call, member access, array access, or postfix increment/decrement.
    /// @return An ExpressionNode representing the parsed call expression in the AST, which may include nested member access, array access, and function calls.
    private ExpressionNode call() {

        var expr = primary();
        
        while (true) if (match(Delimiter.LPAREN)) expr = finishCall(expr);
        else if (match(Delimiter.DOT)) {

            var nameToken = consume(new Literal.IdentifierLiteral(), "Expect property name after '.'");
            var memberName = getLiteralValue(nameToken);
            expr = new MemberAccessExpression(nameToken.getLine(), nameToken.getColumn(), expr, memberName);
        } else if (match(Delimiter.LSQUARE)) {

            var index = parseExpression();
            var bracket = consume(Delimiter.RSQUARE, "Expect ']' after array index");
            expr = new ArrayAccessExpression(bracket.getLine(), bracket.getColumn(), expr, index);
        } else if (match(Operator.INCREMENT, Operator.DECREMENT)) {

            var operator = previous();
            expr = new UnaryExpression(operator.getLine(), operator.getColumn(), (OperatorToken) operator, expr);
        } else break;
        
        return expr;
    }

    /// Finishes parsing a function call expression by parsing the arguments and constructing a CallExpression node.
    /// @param callee The expression representing the function or method being called.
    /// @return A CallExpression node representing the function call with the parsed arguments.
    private ExpressionNode finishCall(ExpressionNode callee) {

        var arguments = new ArrayList<ExpressionNode>();
        if (!check(Delimiter.RPAREN)) do {

            if (arguments.size() >= 255) throw new ParseException("Cannot have more than 255 arguments", peek());
            arguments.add(parseExpression());
        } while (match(Delimiter.COMMA));
        
        var paren = consume(Delimiter.RPAREN, "Expect ')' after arguments");
        return new CallExpression(paren.getLine(), paren.getColumn(), callee, arguments.toArray(new ExpressionNode[0]));
    }

    /// Parses primary expressions, which can be literals, identifiers, or parenthesized expressions.
    ///
    /// Grammar rule: `primary → NUMBER | STRING | "true" | "false" | IDENTIFIER | "(" expression ")"`
    /// @return An ExpressionNode representing the parsed primary expression in the AST.
    private ExpressionNode primary() {

        var token = peek();
        
        if (match(Literal.BooleanLiteral.TRUE)) return new BoolLiteralExpression(token.getLine(), token.getColumn(), true);
        if (match(Literal.BooleanLiteral.FALSE)) return new BoolLiteralExpression(token.getLine(), token.getColumn(), false);
        if (match(Keyword.NULL)) return new NullLiteralExpression(token.getLine(), token.getColumn());

        if (match(new Literal.NumberLiteral())) {

            var lit = (LiteralToken) previous();
            return parseNumber(lit);
        }
        
        if (match(new Literal.StringLiteral())) {

            var lit = (LiteralToken) previous();
            return new StringLiteralExpression(lit.getLine(), lit.getColumn(), lit.getValue());
        }
        
        if (match(new Literal.CharLiteral())) {

            var lit = (LiteralToken) previous();
            var value = lit.getValue();
            char ch = value.length() > 0 ? value.charAt(0) : '\0';
            return new CharLiteralExpression(lit.getLine(), lit.getColumn(), ch);
        }

        if (match(new Literal.IdentifierLiteral())) {

            var lit = (LiteralToken) previous();
            var name = lit.getValue();

            var symbol = symbolTable.lookup(name);
            if (symbol == null) throw new ParseException("Undefined variable '" + name + "'", lit);
            return new IdentifierLiteralExpression(lit.getLine(), lit.getColumn(), name);
        }
        
        if (match(Delimiter.LPAREN)) {

            var expr = parseExpression();
            consume(Delimiter.RPAREN, "Expect ')' after expression");
            return expr;
        }
        
        throw new ParseException("Expect expression", peek());
    }

    /// Parses a number literal token and returns the corresponding NumberLiteralExpression node based on the value and type suffix.
    /// @param token The LiteralToken representing the number literal to be parsed.
    /// @return A NumberLiteralExpression node representing the parsed number literal in the AST, with the appropriate
    /// type (integer, long, float, byte, or double) based on the value and suffix.
    private NumberLiteralExpression parseNumber(LiteralToken token) {

        var value = token.getValue();
        var line = token.getLine();
        var column = token.getColumn();
        var lower = value.toLowerCase();
        
        try {
            
            if (lower.endsWith("l")) {

                var val = Long.parseLong(value.substring(0, value.length() - 1));
                return new NumberLiteralExpression.LongLiteralExpression(line, column, val);
            } else if (lower.endsWith("f")) {

                var val = Float.parseFloat(value);
                return new NumberLiteralExpression.FloatLiteralExpression(line, column, val);
            } else if (lower.endsWith("d") || value.contains(".")) {

                var val = Double.parseDouble(value.replaceAll("[dD]", ""));
                return new NumberLiteralExpression.DoubleLiteralExpression(line, column, val);
            } else if (lower.endsWith("b")) {

                var val = Byte.parseByte(value.substring(0, value.length() - 1));
                return new NumberLiteralExpression.ByteLiteralExpression(line, column, val);
            }
            
            var val = Integer.parseInt(value);
            return new NumberLiteralExpression.IntegerLiteralExpression(line, column, val);
        } catch (NumberFormatException e) { throw new ParseException("Invalid number format: " + value, token); }
    }
}
