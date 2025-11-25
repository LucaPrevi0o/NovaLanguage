package src.parser.parser;

import src.lexer.*;
import src.lexer.token.*;
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
import java.util.List;

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

    public ExpressionParser(ParserState state) { 
        super(state); 
    }

    /**
     * expression → assignment
     */
    public ExpressionNode parseExpression() { return assignment(); }
    
    /**
     * assignment → (IDENTIFIER | arrayAccess | memberAccess) "=" assignment | logicOr
     */
    private ExpressionNode assignment() {

        ExpressionNode expr = logicOr();
        
        if (match(Operator.ASSIGN)) {

            Token equals = previous();
            ExpressionNode value = assignment();
            
            // Valid assignment targets: identifier, array access, member access
            if (expr instanceof IdentifierLiteralExpression || 
                expr instanceof ArrayAccessExpression ||
                expr instanceof MemberAccessExpression) {
                return new AssignmentExpression(
                    equals.getLine(), 
                    equals.getColumn(), 
                    expr,  // target
                    value
                );
            }
            
            throw new ParseException("Invalid assignment target", equals);
        }
        
        return expr;
    }
    
    /**
     * logicOr → logicAnd ("||" logicAnd)*
     */
    private ExpressionNode logicOr() {

        ExpressionNode expr = logicAnd();
        
        while (match(Operator.LOGICAL_OR)) {

            Token operator = previous();
            ExpressionNode right = logicAnd();
            expr = new BinaryExpression(expr.getLine(), expr.getColumn(), expr, (OperatorToken) operator, right);
        }
        return expr;
    }
    
    /**
     * logicAnd → equality ("&&" equality)*
     */
    private ExpressionNode logicAnd() {

        ExpressionNode expr = equality();
        
        while (match(Operator.LOGICAL_AND)) {

            Token operator = previous();
            ExpressionNode right = equality();
            expr = new BinaryExpression(expr.getLine(), expr.getColumn(), expr, (OperatorToken) operator, right);
        }
        return expr;
    }
    
    /**
     * equality → comparison (("==" | "!=") comparison)*
     */
    private ExpressionNode equality() {

        ExpressionNode expr = comparison();
        
        while (match(Operator.EQUAL, Operator.NOT_EQUAL)) {

            Token operator = previous();
            ExpressionNode right = comparison();
            expr = new BinaryExpression(expr.getLine(), expr.getColumn(), expr, (OperatorToken) operator, right);
        }
        return expr;
    }
    
    /**
     * comparison → term (("<" | ">" | "<=" | ">=") term)*
     */
    private ExpressionNode comparison() {

        ExpressionNode expr = term();
        
        while (match(Operator.LESS_THAN, Operator.GREATER_THAN, Operator.LESS_EQUAL, Operator.GREATER_EQUAL)) {

            Token operator = previous();
            ExpressionNode right = term();
            expr = new BinaryExpression(expr.getLine(), expr.getColumn(), expr, (OperatorToken) operator, right);
        }
        return expr;
    }
    
    /**
     * term → factor (("+" | "-") factor)*
     */
    private ExpressionNode term() {

        ExpressionNode expr = factor();
        
        while (match(Operator.PLUS, Operator.MINUS)) {

            Token operator = previous();
            ExpressionNode right = factor();
            expr = new BinaryExpression(expr.getLine(), expr.getColumn(), expr, (OperatorToken) operator, right);
        }
        
        return expr;
    }
    
    /**
     * factor → unary (("*" | "/") unary)*
     */
    private ExpressionNode factor() {

        ExpressionNode expr = unary();
        
        while (match(Operator.MULTIPLY, Operator.DIVIDE)) {

            Token operator = previous();
            ExpressionNode right = unary();
            expr = new BinaryExpression(expr.getLine(), expr.getColumn(), expr, (OperatorToken) operator, right);
        }
        
        return expr;
    }
    
    /**
     * unary → ("!" | "-" | "++") unary | call
     */
    private ExpressionNode unary() {

        if (match(Operator.NOT, Operator.MINUS, Operator.INCREMENT, Operator.DECREMENT)) {

            Token operator = previous();
            ExpressionNode right = unary();
            return new UnaryExpression(operator.getLine(), operator.getColumn(), (OperatorToken) operator, right);
        }
        return call();
    }
    
    /**
     * call → primary ( "(" arguments? ")" | "." IDENTIFIER | "[" expression "]" | "++" | "--" )*
     */
    private ExpressionNode call() {

        ExpressionNode expr = primary();
        
        while (true)
            if (match(Delimiter.LPAREN)) expr = finishCall(expr);
            else if (match(Delimiter.DOT)) {

                Token nameToken = consume(new Literal.IdentifierLiteral(), "Expect property name after '.'");
                String memberName = getLiteralValue(nameToken);
                expr = new MemberAccessExpression(nameToken.getLine(), nameToken.getColumn(), expr, memberName);
            } else if (match(Delimiter.LSQUARE)) {

                ExpressionNode index = parseExpression();
                Token bracket = consume(Delimiter.RSQUARE, "Expect ']' after array index");
                expr = new ArrayAccessExpression(bracket.getLine(), bracket.getColumn(), expr, index);
            } else if (match(Operator.INCREMENT, Operator.DECREMENT)) {

                // Postfix increment/decrement operators
                Token operator = previous();
                expr = new UnaryExpression(operator.getLine(), operator.getColumn(), (OperatorToken) operator, expr);
            } else break;
        
        return expr;
    }
    
    /**
     * Parse function call arguments.
     */
    private ExpressionNode finishCall(ExpressionNode callee) {

        List<ExpressionNode> arguments = new ArrayList<>();
        
        if (!check(Delimiter.RPAREN)) do {

            if (arguments.size() >= 255) throw new ParseException("Cannot have more than 255 arguments", peek());
            arguments.add(parseExpression());
        } while (match(Delimiter.COMMA));
        
        Token paren = consume(Delimiter.RPAREN, "Expect ')' after arguments");
        return new CallExpression(paren.getLine(), paren.getColumn(), callee, arguments.toArray(new ExpressionNode[0]));
    }
    
    /**
     * primary → NUMBER | STRING | "true" | "false" | IDENTIFIER | "(" expression ")"
     */
    private ExpressionNode primary() {

        Token token = peek();
        
        // Boolean literals
        if (match(Literal.BooleanLiteral.TRUE)) return new BoolLiteralExpression(token.getLine(), token.getColumn(), true);
        if (match(Literal.BooleanLiteral.FALSE)) return new BoolLiteralExpression(token.getLine(), token.getColumn(), false);

        // Number literal
        if (match(new Literal.NumberLiteral())) {

            LiteralToken lit = (LiteralToken) previous();
            return parseNumber(lit);
        }
        
        // String literal
        if (match(new Literal.StringLiteral())) {

            LiteralToken lit = (LiteralToken) previous();
            return new StringLiteralExpression(lit.getLine(), lit.getColumn(), lit.getValue());
        }
        
        // Identifier
        if (match(new Literal.IdentifierLiteral())) {

            LiteralToken lit = (LiteralToken) previous();
            String name = lit.getValue();
            
            // Validate that the identifier has been declared
            var symbol = symbolTable.lookup(name);
            if (symbol == null) {
                throw new ParseException("Undefined variable '" + name + "'", lit);
            }
            
            return new IdentifierLiteralExpression(lit.getLine(), lit.getColumn(), name);
        }
        
        // Grouped expression
        if (match(Delimiter.LPAREN)) {

            ExpressionNode expr = parseExpression();
            consume(Delimiter.RPAREN, "Expect ')' after expression");
            return expr;
        }
        
        throw new ParseException("Expect expression", peek());
    }
    
    /**
     * Parse a number literal and determine its type.
     */
    private NumberLiteralExpression parseNumber(LiteralToken token) {

        String value = token.getValue();
        int line = token.getLine();
        int column = token.getColumn();
        
        String lower = value.toLowerCase();
        
        try {
            
            if (lower.endsWith("l")) {

                long val = Long.parseLong(value.substring(0, value.length() - 1));
                return new NumberLiteralExpression.LongLiteralExpression(line, column, val);
            }
            if (lower.endsWith("f")) {

                float val = Float.parseFloat(value);
                return new NumberLiteralExpression.FloatLiteralExpression(line, column, val);
            }
            if (lower.endsWith("d") || value.contains(".")) {

                double val = Double.parseDouble(value.replaceAll("[dD]", ""));
                return new NumberLiteralExpression.DoubleLiteralExpression(line, column, val);
            }
            
            // Default: integer
            int val = Integer.parseInt(value);
            return new NumberLiteralExpression.IntegerLiteralExpression(line, column, val);
            
        } catch (NumberFormatException e) { throw new ParseException("Invalid number format: " + value, token); }
    }
}
