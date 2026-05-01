package src.parser.ast.nodes.statement;

import src.parser.ast.nodes.*;

/// Represents an expression statement in the abstract syntax tree (AST), which consists of a single expression that is evaluated for its side effects.
public class ExpressionStatement extends StatementNode {

    private final ExpressionNode expression;

    /// Constructs a new ExpressionStatement with the specified line, column, and expression.
    /// @param line The line number in the source code where this expression statement occurs.
    /// @param column The column number in the source code where this expression statement starts.
    /// @param expression An ExpressionNode representing the expression that is evaluated for its side effects.
    public ExpressionStatement(int line, int column, ExpressionNode expression) {
        
        super(line, column);
        this.expression = expression;
    }

    /// Returns the expression that is evaluated for its side effects in this expression statement.
    /// @return An ExpressionNode representing the expression of this expression statement.
    public ExpressionNode getExpression() { return expression; }
}
