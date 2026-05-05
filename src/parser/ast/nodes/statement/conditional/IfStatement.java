package parser.ast.nodes.statement.conditional;

import parser.ast.nodes.*;
import parser.ast.nodes.statement.ConditionalStatement;
import parser.ast.visitor.NodeVisitor;

/// Represents an if statement in the AST.
///
/// An if statement consists of a condition expression, a then block (the code to execute if the condition is true),
/// and an optional else block (the code to execute if the condition is false).
public class IfStatement extends ConditionalStatement {

    private final StatementNode thenBlock;
    private final StatementNode elseBlock;

    /// Constructs a new IfStatement with the specified line, column, condition, then block, and else block.
    /// @param line The line number in the source code where this statement occurs.
    /// @param column The column number in the source code where this statement starts.
    /// @param condition The expression that is evaluated to determine if the then block or else block should be executed.
    /// @param thenBlock The statement that represents the code to execute if the condition is true.
    /// @param elseBlock The statement that represents the code to execute if the condition is false (optional).
    public IfStatement(int line, int column, ExpressionNode condition, StatementNode thenBlock, StatementNode elseBlock) {

        super(line, column, condition);
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }

    /// Returns the statement that represents the code to execute if the condition is true.
    /// @return The then block statement.
    public StatementNode getThenBlock() { return thenBlock; }

    /// Returns the statement that represents the code to execute if the condition is false (optional).
    /// @return The else block statement, or null if there is no else block.
    public StatementNode getElseBlock() { return elseBlock; }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) { return visitor.visitIf(this); }
}
