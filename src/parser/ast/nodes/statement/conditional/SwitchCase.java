package parser.ast.nodes.statement.conditional;

import parser.ast.nodes.ExpressionNode;
import parser.ast.nodes.StatementNode;

/// Represents a single case (or the default arm) inside a switch statement.
///
/// A case with a non-null {@code value} matches that specific value.
/// A case with a {@code null} {@code value} is the default arm.
public class SwitchCase {

    private final ExpressionNode value;   // null for default
    private final StatementNode body;
    private final int line;
    private final int column;

    /// Constructs a new SwitchCase.
    /// @param line   Line number of the case keyword.
    /// @param column Column number of the case keyword.
    /// @param value  The value expression to match, or {@code null} for the default arm.
    /// @param body   The statement body to execute when this case matches.
    public SwitchCase(int line, int column, ExpressionNode value, StatementNode body) {

        this.line = line;
        this.column = column;
        this.value = value;
        this.body = body;
    }

    /// Returns the match value, or {@code null} if this is the default arm.
    /// @return The case value expression.
    public ExpressionNode getValue() { return value; }

    /// Returns the body of this case.
    /// @return The body statement.
    public StatementNode getBody() { return body; }

    /// Returns the line number of this case.
    /// @return The line number.
    public int getLine() { return line; }

    /// Returns the column number of this case.
    /// @return The column number.
    public int getColumn() { return column; }

    /// Returns whether this is the default arm.
    /// @return {@code true} if this is the default arm.
    public boolean isDefault() { return value == null; }
}
