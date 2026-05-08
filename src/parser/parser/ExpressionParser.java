package parser.parser;

import lexer.Token;
import lexer.token.family.literal.*;
import lexer.token.type.LiteralToken;
import lexer.token.type.OperatorToken;
import parser.ast.SymbolTable;
import parser.ast.nodes.ExpressionNode;
import parser.ast.nodes.expression.*;
import parser.ast.nodes.expression.access.ArrayAccessExpression;
import parser.ast.nodes.expression.access.MemberAccessExpression;
import parser.ast.nodes.expression.literal.BoolLiteralExpression;
import parser.ast.nodes.expression.literal.CharLiteralExpression;
import parser.ast.nodes.expression.literal.IdentifierLiteralExpression;
import parser.ast.nodes.expression.literal.NullLiteralExpression;
import parser.ast.nodes.expression.literal.NumberLiteralExpression;
import parser.ast.nodes.expression.literal.StringLiteralExpression;
import parser.parser.util.ParseException;
import parser.parser.util.ParserBase;
import parser.parser.util.ParserState;
import lexer.token.TypeRegistry;
import lexer.token.family.Delimiter;
import lexer.token.family.Keyword;
import lexer.token.family.Operator;
import java.util.ArrayList;

/// Parser for expressions, handling operator precedence and associativity.
///
/// GRAMMAR FOR EXPRESSIONS
///
/// ```
/// expression  → assignment
/// assignment  → (lValue) ("=" | "+=" | "-=" | "*=" | "/=") assignment | ternary
/// ternary     → logicOr ("?" expression ":" expression)?
/// logicOr     → logicAnd ("||" logicAnd)*
/// logicAnd    → equality ("&&" equality)*
/// equality    → comparison (("==" | "!=") comparison)*
/// comparison  → term (("<" | ">" | "<=" | ">=") term)*
/// term        → factor (("+" | "-") factor)*
/// factor      → unary (("*" | "/" | "%") unary)*
/// unary       → ("!" | "-" | "++" | "--") unary | call
/// call        → primary (postfixOp)*
///   postfixOp → "(" arguments? ")" | "." IDENTIFIER | "[" expression "]" | "++" | "--"
/// primary     → NUMBER | STRING | CHAR | "true" | "false" | "null" | IDENTIFIER
///             | "new" CLASSNAME "(" arguments? ")" | "(" expression ")"
/// ```
public class ExpressionParser extends ParserBase {

    /// Constructs a new ExpressionParser.
    /// @param state        The current parser state (token stream).
    /// @param symbolTable  The symbol table for the current scope.
    /// @param typeRegistry The per-session type registry.
    public ExpressionParser(ParserState state, SymbolTable symbolTable, TypeRegistry typeRegistry) {
        super(state, symbolTable, typeRegistry);
    }

    /// Entry point: parses a full expression.
    /// @return The root ExpressionNode of the parsed expression AST.
    public ExpressionNode parseExpression() { return assignment(); }

    // ─── Precedence levels ────────────────────────────────────────────────────

    /// Parses an assignment expression, which can be a simple assignment or a compound assignment.
    /// Grammar rule: {@code assignment → (lValue) ("=" | "+=" | "-=" | "*=" | "/=") assignment | ternary}
    /// > Note: The left-hand side of an assignment must be a valid l-value (identifier, array access, or member access).
    /// @return An ExpressionNode representing the assignment expression, which may be a simple assignment or a compound assignment.
    private ExpressionNode assignment() {

        var expr = ternary();
        if (match(Operator.ASSIGN, Operator.PLUS_ASSIGN, Operator.MINUS_ASSIGN, Operator.MULTIPLY_ASSIGN, Operator.DIVIDE_ASSIGN, Operator.MODULO_ASSIGN)) {

            var operator = previous();
            var value = assignment();

            if (!(expr instanceof IdentifierLiteralExpression ||
            expr instanceof ArrayAccessExpression ||
            expr instanceof MemberAccessExpression)) throw new ParseException("Invalid assignment target", operator);

            var assignmentValue = expandCompound(expr, operator, value);
            return new AssignmentExpression(operator.getLine(), operator.getColumn(), expr, assignmentValue);
        }
        return expr;
    }

    /// Parses a ternary expression: {@code condition ? thenExpr : elseExpr}.
    /// Grammar rule: {@code ternary → logicOr ("?" expression ":" expression)?}
    /// @return An ExpressionNode representing the ternary expression if the `?` operator is present, or the result of `logicOr()` if not.
    private ExpressionNode ternary() {

        var expr = logicOr();
        if (match(Operator.QUESTION)) {

            var questionToken = previous();
            var thenExpr = parseExpression();
            consume(Delimiter.COLON, "Expect ':' in ternary expression");
            var elseExpr = parseExpression();
            return new TernaryExpression(questionToken.getLine(), questionToken.getColumn(), expr, thenExpr, elseExpr);
        }
        return expr;
    }

    /// Parses logical OR expressions, which have the lowest precedence among binary operators.
    /// Grammar rule: {@code logicOr → logicAnd ("||" logicAnd)*}
    /// @return An ExpressionNode representing the logical OR expression, which may consist of multiple `||` operations.
    private ExpressionNode logicOr() {

        var expr = logicAnd();
        while (match(Operator.LOGICAL_OR)) {

            var op = (OperatorToken) previous();
            var right = logicAnd();
            expr = new BinaryExpression(expr.getLine(), expr.getColumn(), expr, op, right);
        }
        return expr;
    }

    /// Parses logical AND expressions, which have higher precedence than logical OR.
    /// Grammar rule: {@code logicAnd → equality ("&&" equality)*}
    /// @return An ExpressionNode representing the logical AND expression, which may consist of multiple `&&` operations.
    private ExpressionNode logicAnd() {

        var expr = equality();
        while (match(Operator.LOGICAL_AND)) {

            var op = (OperatorToken) previous();
            var right = equality();
            expr = new BinaryExpression(expr.getLine(), expr.getColumn(), expr, op, right);
        }
        return expr;
    }

    /// Parses equality expressions, which have higher precedence than logical AND.
    /// Grammar rule: {@code equality → comparison (("==" | "!=") comparison)*}
    /// @return An ExpressionNode representing the equality expression, which may consist of multiple `==` or `!=` operations.
    private ExpressionNode equality() {

        var expr = comparison();
        while (match(Operator.EQUAL, Operator.NOT_EQUAL)) {

            var op = (OperatorToken) previous();
            var right = comparison();
            expr = new BinaryExpression(expr.getLine(), expr.getColumn(), expr, op, right);
        }
        return expr;
    }

    /// Parses comparison expressions, which have higher precedence than equality.
    /// Grammar rule: {@code comparison → term (("<" | ">" | "<=" | ">=") term)*}
    /// @return An ExpressionNode representing the comparison expression, which may consist of multiple `<`, `>`, `<=`, or `>=` operations.
    private ExpressionNode comparison() {

        var expr = term();
        while (match(Operator.LESS_THAN, Operator.GREATER_THAN, Operator.LESS_EQUAL, Operator.GREATER_EQUAL)) {

            var op = (OperatorToken) previous();
            var right = term();
            expr = new BinaryExpression(expr.getLine(), expr.getColumn(), expr, op, right);
        }
        return expr;
    }

    /// Parses addition and subtraction expressions, which have higher precedence than comparison.
    /// Grammar rule: {@code term → factor (("+" | "-") factor)*}
    /// @return An ExpressionNode representing the addition/subtraction expression, which may consist of multiple `+` or `-` operations.
    private ExpressionNode term() {

        var expr = factor();
        while (match(Operator.PLUS, Operator.MINUS)) {

            var op = (OperatorToken) previous();
            var right = factor();
            expr = new BinaryExpression(expr.getLine(), expr.getColumn(), expr, op, right);
        }
        return expr;
    }

    /// Parses multiplication, division, and modulo expressions, which have higher precedence than addition and subtraction.
    /// Grammar rule: {@code factor → unary (("*" | "/" | "%") unary)*}
    /// @return An ExpressionNode representing the multiplication/division/modulo expression, which may consist of multiple `*`, `/`, or `%` operations.
    private ExpressionNode factor() {

        var expr = unary();
        while (match(Operator.MULTIPLY, Operator.DIVIDE, Operator.MODULO)) {

            var op = (OperatorToken) previous();
            var right = unary();
            expr = new BinaryExpression(expr.getLine(), expr.getColumn(), expr, op, right);
        }
        return expr;
    }

    /// Parses unary expressions, which have higher precedence than multiplication/division/modulo.
    /// Grammar rule: {@code unary → ("!" | "-" | "++" | "--") unary | call}
    /// @return An ExpressionNode representing the unary expression if a unary operator is present, or the result of `call()` if not.
    private ExpressionNode unary() {

        if (match(Operator.NOT, Operator.MINUS, Operator.INCREMENT, Operator.DECREMENT)) {

            var op = (OperatorToken) previous();
            var right = unary();
            return new UnaryExpression(op.getLine(), op.getColumn(), op, right);
        }
        return call();
    }

    /// Parses call expressions, which include function calls, member access, array access, and postfix increment/decrement.
    /// Grammar rule: {@code call → primary (postfixOp)*}
    /// > Note: This method handles the chaining of member access, array access, and function calls, as well as postfix increment/decrement operators.
    /// @return An ExpressionNode representing the call expression, which may include nested member access, array access, function calls, and postfix operators.
    private ExpressionNode call() {

        var expr = primary();
        while (true) {

            if (match(Delimiter.LPAREN)) expr = finishCall(expr);
            else if (match(Delimiter.DOT)) {

                var nameToken = consume(new IdentifierLiteral(), "Expect property name after '.'");
                expr = new MemberAccessExpression(nameToken.getLine(), nameToken.getColumn(), expr, getLiteralValue(nameToken));
            } else if (match(Delimiter.LSQUARE)) {

                var index = parseExpression();
                var bracket = consume(Delimiter.RSQUARE, "Expect ']' after array index");
                expr = new ArrayAccessExpression(bracket.getLine(), bracket.getColumn(), expr, index);
            } else if (match(Operator.INCREMENT, Operator.DECREMENT)) {

                // Bug fix: postfix ++ / -- use a dedicated node to distinguish from prefix
                var op = (OperatorToken) previous();
                expr = new PostfixUnaryExpression(op.getLine(), op.getColumn(), expr, op);
            } else break;
        }
        return expr;
    }

    /// Finishes parsing a function call after the initial `(` has been consumed.
    /// Grammar rule: {@code call → primary "(" arguments? ")"}
    /// @param callee The expression representing the function being called (e.g., an identifier or a member access).
    /// @return A CallExpression node representing the function call, including the callee and the list of argument expressions.
    private ExpressionNode finishCall(ExpressionNode callee) {

        var arguments = new ArrayList<ExpressionNode>();
        if (!check(Delimiter.RPAREN)) do {

            if (arguments.size() >= 255) throw new ParseException("Cannot have more than 255 arguments", peek());
            arguments.add(parseExpression());
        } while (match(Delimiter.COMMA));

        var paren = consume(Delimiter.RPAREN, "Expect ')' after arguments");
        return new CallExpression(paren.getLine(), paren.getColumn(), callee, arguments.toArray(new ExpressionNode[0]));
    }

    /// Parses primary expressions, which are the most basic building blocks of expressions.
    /// Grammar rule: {@code primary → NUMBER | STRING | CHAR | "true" | "false" | "null" | IDENTIFIER | "new" CLASSNAME "(" arguments? ")" | "(" expression ")"}
    /// @return An ExpressionNode representing the primary expression, which may be a literal, an identifier, an object creation expression, or a parenthesized expression.
    private ExpressionNode primary() {

        var token = peek();

        if (match(BoolLiteral.TRUE)) return new BoolLiteralExpression(token.getLine(), token.getColumn(), true);
        if (match(BoolLiteral.FALSE)) return new BoolLiteralExpression(token.getLine(), token.getColumn(), false);
        if (match(Keyword.NULL)) return new NullLiteralExpression(token.getLine(), token.getColumn());

        if (match(new NumberLiteral())) return parseNumber((LiteralToken) previous());

        if (match(new StringLiteral())) {

            var lit = (LiteralToken) previous();
            return new StringLiteralExpression(lit.getLine(), lit.getColumn(), lit.getValue());
        }

        if (match(new CharLiteral())) {

            var lit = (LiteralToken) previous();
            var value = lit.getValue();
            var ch = (value != null && !value.isEmpty()) ? value.charAt(0) : '\0';
            return new CharLiteralExpression(lit.getLine(), lit.getColumn(), ch);
        }

        if (match(new IdentifierLiteral())) {

            var lit = (LiteralToken) previous();
            var name = lit.getValue();
            var symbol = symbolTable.lookup(name);
            if (symbol == null) throw new ParseException("Undefined variable '" + name + "'", lit);
            return new IdentifierLiteralExpression(lit.getLine(), lit.getColumn(), name);
        }

        if (match(Keyword.NEW)) {

            var newToken = previous();
            // Require an identifier (class name); reject primitive types with a clear error
            if (isTypeToken(peek()) && !(peek() instanceof LiteralToken)) {

                // It's a type-token (primitive keyword like "int")
                var badToken = peek();
                throw new ParseException("Cannot use 'new' with primitive type '" + badToken.getType().get() + "'. Use a class name instead.", badToken);
            }

            var classNameToken = consume(new IdentifierLiteral(), "Expect class name after 'new'");
            var className = getLiteralValue(classNameToken);

            if (!typeRegistry.isCustomClass(className)) throw new ParseException("Class '" + className + "' is not a defined class. 'new' requires a class type.", classNameToken);

            consume(Delimiter.LPAREN, "Expect '(' after class name");
            var arguments = new ArrayList<ExpressionNode>();
            if (!check(Delimiter.RPAREN)) do {
                arguments.add(parseExpression());
            } while (match(Delimiter.COMMA));
            consume(Delimiter.RPAREN, "Expect ')' after arguments");
            return new ObjectCreationExpression(newToken.getLine(), newToken.getColumn(), className, arguments.toArray(new ExpressionNode[0]));
        }

        if (match(Delimiter.LPAREN)) {

            var expr = parseExpression();
            consume(Delimiter.RPAREN, "Expect ')' after expression");
            return expr;
        }

        throw new ParseException("Expect expression", peek());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /// Expands a compound assignment operator into a binary expression.
    /// For example, `x += 5` becomes `x = x + 5`.
    /// @param target The left-hand side expression of the assignment (e.g., variable, array element, or object property).
    /// @param operator The compound assignment operator token (e.g., `+=`, `-=`, `*=`, `/=`, `%=`).
    /// @param value The right-hand side expression of the assignment (the value being assigned).
    /// @return An ExpressionNode representing the expanded compound assignment, which is a binary expression combining the target and value with the appropriate operator.
    private static ExpressionNode expandCompound(ExpressionNode target, Token operator, ExpressionNode value) {

        if (!(operator.getType() instanceof Operator op) || op == Operator.ASSIGN) return value;

        var binaryOp = switch (op) {
            case PLUS_ASSIGN     -> Operator.PLUS;
            case MINUS_ASSIGN    -> Operator.MINUS;
            case MULTIPLY_ASSIGN -> Operator.MULTIPLY;
            case DIVIDE_ASSIGN   -> Operator.DIVIDE;
            case MODULO_ASSIGN   -> Operator.MODULO;
            default -> throw new ParseException("Unknown compound assignment operator", operator);
        };

        return new BinaryExpression(
            operator.getLine(), operator.getColumn(),
            target,
            new OperatorToken(binaryOp, operator.getLine(), operator.getColumn()),
            value
        );
    }

    /// Parses a numeric literal into the appropriate {@link NumberLiteralExpression} subtype.
    /// Supports suffixes for long (`L`), float (`F`), double (`D`), and byte (`B`) literals, as well as plain integers and doubles.
    /// @param token The literal token representing the numeric value, which may include a suffix to indicate the specific numeric type.
    /// @return A NumberLiteralExpression representing the parsed numeric literal, with the appropriate type based on the suffix or format of the value.
    private NumberLiteralExpression parseNumber(LiteralToken token) {

        var value = token.getValue();
        var line  = token.getLine();
        var col   = token.getColumn();
        var lower = value.toLowerCase();

        try {

            if (lower.endsWith("l"))
                return new NumberLiteralExpression.LongLiteralExpression(line, col, Long.parseLong(value.substring(0, value.length() - 1)));

            if (lower.endsWith("f"))
                return new NumberLiteralExpression.FloatLiteralExpression(line, col, Float.parseFloat(value));

            if (lower.endsWith("d") || value.contains("."))
                return new NumberLiteralExpression.DoubleLiteralExpression(line, col, Double.parseDouble(value.replaceAll("[dD]$", "")));

            if (lower.endsWith("b"))
                return new NumberLiteralExpression.ByteLiteralExpression(line, col, Byte.parseByte(value.substring(0, value.length() - 1)));

            return new NumberLiteralExpression.IntegerLiteralExpression(line, col, Integer.parseInt(value));
        } catch (NumberFormatException e) { throw new ParseException("Invalid number format: " + value, token); }
    }
}
