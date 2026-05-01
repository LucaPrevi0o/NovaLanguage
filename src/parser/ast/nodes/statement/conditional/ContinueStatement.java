package src.parser.ast.nodes.statement.conditional;

import src.parser.ast.nodes.StatementNode;

/// Represents a continue statement, used to skip the current iteration of a loop and continue with the next one.
public class ContinueStatement extends StatementNode {

    /// Constructs a new ContinueStatement with the specified line and column.
    /// @param line The line number where the continue statement appears.
    /// @param column The column number where the continue statement starts.
    public ContinueStatement(int line, int column) {

        super(line, column);
    }
}

