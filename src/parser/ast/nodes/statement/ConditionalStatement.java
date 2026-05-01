package src.parser.ast.nodes.statement;

import src.parser.ast.nodes.*;

/// Represents a conditional statement in the abstract syntax tree (AST), which includes an expression that evaluates to a boolean condition.
public abstract class ConditionalStatement extends StatementNode {

    private final ExpressionNode condition;

    /// Constructs a new ConditionalStatement with the specified line, column, and condition expression.
    /// @param line The line number in the source code where this conditional statement occurs.
    /// @param column The column number in the source code where this conditional statement starts.
    /// @param condition An ExpressionNode representing the condition that is evaluated to determine the flow of control.
    public ConditionalStatement(int line, int column, ExpressionNode condition) {

        super(line, column);
        this.condition = condition;
    }

    /// Returns the expression that represents the condition of this conditional statement.
    /// @return An ExpressionNode representing the condition of this conditional statement.
    public ExpressionNode getCondition() { return condition; }
}
