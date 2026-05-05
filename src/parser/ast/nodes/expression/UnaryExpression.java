package parser.ast.nodes.expression;

import lexer.token.OperatorToken;
import parser.ast.nodes.ExpressionNode;

/// Represents a unary expression in the AST, consisting of an operator and a single operand expression.
public class UnaryExpression extends ExpressionNode {
    
    private final OperatorToken operator;
    private final ExpressionNode operand;

    /// Constructs a new UnaryExpression with the specified line, column, operator, and operand expression.
    /// @param line The line number in the source code where this expression occurs.
    /// @param column The column number in the source code where this expression starts.
    /// @param operator The operator token representing the unary operation (e.g., -, !).
    /// @param operand The expression that is the operand of the unary operation.
    public UnaryExpression(int line, int column, OperatorToken operator, ExpressionNode operand) {
        
        super(line, column);
        this.operator = operator;
        this.operand = operand;
    }

    /// Returns the operator token representing the unary operation.
    /// @return The operator token.
    public OperatorToken getOperator() { return operator; }

    /// Returns the expression that is the operand of the unary operation.
    /// @return The operand expression.
    public ExpressionNode getOperand() { return operand; }
}
