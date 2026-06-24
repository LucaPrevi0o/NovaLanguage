package parser.ast.nodes.statement;

import parser.ast.Printable;
import parser.ast.nodes.StatementNode;

import java.util.List;

/// Represents a break statement in the abstract syntax tree (AST).
public class BreakStatement extends StatementNode implements Printable {

    /// Constructs a new BreakStatement node with the specified source position.
    /// @param line The line number in the source code where the break statement appears.
    /// @param column The column number in the source code where the break statement appears.
    public BreakStatement(int line, int column) {
        super(line, column);
    }

    @Override
    public String toPrintString() { return "BreakStatement [line " + getLine() + "]"; }

    @Override
    public List<PrintEntry> getPrintEntries() { return List.of(new PrintEntry.Info("break")); }
}
