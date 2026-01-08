package src.parser.parser;

import src.lexer.token.*;
import src.parser.ast.SymbolTable;
import src.parser.ast.nodes.ExpressionNode;
import src.parser.ast.nodes.expression.*;
import src.parser.ast.nodes.expression.access.ArrayAccessExpression;
import src.parser.ast.nodes.expression.access.MemberAccessExpression;
import src.parser.ast.nodes.expression.literal.BoolLiteralExpression;
import src.parser.ast.nodes.expression.literal.IdentifierLiteralExpression;
import src.parser.ast.nodes.expression.literal.NumberLiteralExpression;
import src.parser.ast.nodes.expression.literal.StringLiteralExpression;
import src.parser.parser.util.ParseException;
import src.parser.parser.util.ParserBase;
import src.parser.parser.util.ParserState;
import src.token.family.Delimiter;
import src.token.family.Literal;
import src.token.family.Operator;
import java.util.ArrayList;

/**
 * Parser for expressions following operator precedence.
 * 
 * Grammar:
 * expression     → assignment
 * assignment     → IDENTIFIER "=" assignment | logicOr
 * logicOr        → logicAnd ("||" logicAnd)*
 * logicAnd       → equality ("&&" equality)*
 * equality       → comparison (("==" | "!=") comparison)*
 * comparison     → term (("<" | ">" | "<=" | ">=") term)*
 * term           → factor (("+" | "-") factor)*
 * factor         → unary (("*" | "/") unary)*
 * unary          → ("!" | "-" | "++") unary | call
 * call           → primary ("(" arguments? ")")*
 * primary        → NUMBER | STRING | "true" | "false" | IDENTIFIER | "(" expression ")"
 */
public class ExpressionParser extends ParserBase {

    /**
     * Constructor that shares the symbol table with parent parser.
     */
    public ExpressionParser(ParserState state, SymbolTable symbolTable) { super(state, symbolTable); }

    /**
     * expression → assignment
     */
    public ExpressionNode parseExpression() { return assignment(); }
    
    /**
     * assignment → (IDENTIFIER | arrayAccess | memberAccess) "=" assignment | logicOr
     */
    private ExpressionNode assignment() {

        var expr = logicOr();
        
        if (match(Operator.ASSIGN)) {

            var equals = previous();
            var value = assignment();
            
            // Valid assignment targets: identifier, array access, member access
            if (expr instanceof IdentifierLiteralExpression || 
            expr instanceof ArrayAccessExpression ||
            expr instanceof MemberAccessExpression)
                return new AssignmentExpression(
                    equals.getLine(), 
                    equals.getColumn(), 
                    expr,  // target
                    value
                );
            throw new ParseException("Invalid assignment target", equals);
        }
        
        return expr;
    }
    
    /**
     * logicOr → logicAnd ("||" logicAnd)*
     */
    private ExpressionNode logicOr() {

        var expr = logicAnd();
        while (match(Operator.LOGICAL_OR)) {

            var operator = previous();
            var right = logicAnd();
            expr = new BinaryExpression(expr.getLine(), expr.getColumn(), expr, (OperatorToken) operator, right);
        }
        return expr;
    }
    
    /**
     * logicAnd → equality ("&&" equality)*
     */
    private ExpressionNode logicAnd() {

        var expr = equality();
        while (match(Operator.LOGICAL_AND)) {

            var operator = previous();
            var right = equality();
            expr = new BinaryExpression(expr.getLine(), expr.getColumn(), expr, (OperatorToken) operator, right);
        }
        return expr;
    }
    
    /**
     * equality → comparison (("==" | "!=") comparison)*
     */
    private ExpressionNode equality() {

        var expr = comparison();
        while (match(Operator.EQUAL, Operator.NOT_EQUAL)) {

            var operator = previous();
            var right = comparison();
            expr = new BinaryExpression(expr.getLine(), expr.getColumn(), expr, (OperatorToken) operator, right);
        }
        return expr;
    }
    
    /**
     * comparison → term (("<" | ">" | "<=" | ">=") term)*
     */
    private ExpressionNode comparison() {

        var expr = term();
        while (match(Operator.LESS_THAN, Operator.GREATER_THAN, Operator.LESS_EQUAL, Operator.GREATER_EQUAL)) {

            var operator = previous();
            var right = term();
            expr = new BinaryExpression(expr.getLine(), expr.getColumn(), expr, (OperatorToken) operator, right);
        }
        return expr;
    }
    
    /**
     * term → factor (("+" | "-") factor)*
     */
    private ExpressionNode term() {

        var expr = factor();
        while (match(Operator.PLUS, Operator.MINUS)) {

            var operator = previous();
            var right = factor();
            expr = new BinaryExpression(expr.getLine(), expr.getColumn(), expr, (OperatorToken) operator, right);
        }
        
        return expr;
    }
    
    /**
     * factor → unary (("*" | "/") unary)*
     */
    private ExpressionNode factor() {

        var expr = unary();
        
        while (match(Operator.MULTIPLY, Operator.DIVIDE)) {
            var operator = previous();
            var right = unary();
            expr = new BinaryExpression(expr.getLine(), expr.getColumn(), expr, (OperatorToken) operator, right);
        }
        
        return expr;
    }
    
    /**
     * unary → ("!" | "-" | "++") unary | call
     */
    private ExpressionNode unary() {

        if (match(Operator.NOT, Operator.MINUS, Operator.INCREMENT, Operator.DECREMENT)) {

            var operator = previous();
            var right = unary();
            return new UnaryExpression(operator.getLine(), operator.getColumn(), (OperatorToken) operator, right);
        }
        return call();
    }
    
    /**
     * call → primary ( "(" arguments? ")" | "." IDENTIFIER | "[" expression "]" | "++" | "--" )*
     */
    private ExpressionNode call() {

        var expr = primary();
        
        while (true)
            if (match(Delimiter.LPAREN)) expr = finishCall(expr);
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
    
    /**
     * Parse function call arguments.
     */
    private ExpressionNode finishCall(ExpressionNode callee) {

        var arguments = new ArrayList<ExpressionNode>();
        if (!check(Delimiter.RPAREN)) do {

            if (arguments.size() >= 255) throw new ParseException("Cannot have more than 255 arguments", peek());
            arguments.add(parseExpression());
        } while (match(Delimiter.COMMA));
        
        var paren = consume(Delimiter.RPAREN, "Expect ')' after arguments");
        return new CallExpression(paren.getLine(), paren.getColumn(), callee, arguments.toArray(new ExpressionNode[0]));
    }
    
    /**
     * primary → NUMBER | STRING | "true" | "false" | IDENTIFIER | "(" expression ")"
     */
    private ExpressionNode primary() {

        var token = peek();
        
        if (match(Literal.BooleanLiteral.TRUE)) return new BoolLiteralExpression(token.getLine(), token.getColumn(), true);
        if (match(Literal.BooleanLiteral.FALSE)) return new BoolLiteralExpression(token.getLine(), token.getColumn(), false);

        if (match(new Literal.NumberLiteral())) {

            var lit = (LiteralToken) previous();
            return parseNumber(lit);
        }
        
        if (match(new Literal.StringLiteral())) {

            var lit = (LiteralToken) previous();
            return new StringLiteralExpression(lit.getLine(), lit.getColumn(), lit.getValue());
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
    
    /**
     * Parse a number literal and determine its type.
     */
    private NumberLiteralExpression parseNumber(LiteralToken token) {

        var value = token.getValue();
        var line = token.getLine();
        var column = token.getColumn();
        var lower = value.toLowerCase();
        
        try {
            
            if (lower.endsWith("l")) {

                var val = Long.parseLong(value.substring(0, value.length() - 1));
                return new NumberLiteralExpression.LongLiteralExpression(line, column, val);
            }
            if (lower.endsWith("f")) {

                var val = Float.parseFloat(value);
                return new NumberLiteralExpression.FloatLiteralExpression(line, column, val);
            }
            if (lower.endsWith("d") || value.contains(".")) {

                var val = Double.parseDouble(value.replaceAll("[dD]", ""));
                return new NumberLiteralExpression.DoubleLiteralExpression(line, column, val);
            }
            
            var val = Integer.parseInt(value);
            return new NumberLiteralExpression.IntegerLiteralExpression(line, column, val);
        } catch (NumberFormatException e) { throw new ParseException("Invalid number format: " + value, token); }
    }
}
