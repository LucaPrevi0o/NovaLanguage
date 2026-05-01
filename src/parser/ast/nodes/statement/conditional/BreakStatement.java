package src.parser.ast.nodes.statement.conditional;

import src.parser.ast.nodes.StatementNode;

/// Represents a break statement, used to exit from a loop prematurely.
public class BreakStatement extends StatementNode {

    /// Constructs a new BreakStatement with the specified line and column.
    /// @param line The line number where the break statement appears.
    /// @param column The column number where the break statement starts.
    public BreakStatement(int line, int column) {

        super(line, column);
    }
}

