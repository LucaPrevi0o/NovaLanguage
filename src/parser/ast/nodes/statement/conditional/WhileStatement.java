package parser.ast.nodes.statement.conditional;

import parser.ast.nodes.*;
import parser.ast.nodes.statement.ConditionalStatement;

/// Represents a while loop statement in the AST.
///
/// A while loop consists of a condition expression and a body statement.
/// The condition is evaluated before each iteration, and if it is true, the body is executed.
/// This process repeats until the condition evaluates to false.
public class WhileStatement extends ConditionalStatement {

    private final StatementNode body;

    /// Constructs a new WhileStatement with the specified line, column, condition, and body.
    /// @param line The line number in the source code where this statement occurs.
    /// @param column The column number in the source code where this statement starts.
    /// @param condition The expression that is evaluated before each iteration to determine if the loop should continue.
    /// @param body The statement that represents the body of the loop, which is executed if the condition is true.
    public WhileStatement(int line, int column, ExpressionNode condition, StatementNode body) {

        super(line, column, condition);
        this.body = body;
    }

    /// Returns the statement that represents the body of the loop, which is executed if the condition is true.
    /// @return The body statement.
    public StatementNode getBody() { return body; }
}