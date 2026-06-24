package parser.ast.nodes.statement;

import parser.ast.Printable;
import parser.ast.nodes.StatementNode;

import java.util.List;

/// Represents a continue statement in the abstract syntax tree (AST).
public class ContinueStatement extends StatementNode implements Printable {

    /// Constructs a new ContinueStatement node with the specified source position.
    /// @param line The line number in the source code where the continue statement appears.
    /// @param column The column number in the source code where the continue statement appears.
    public ContinueStatement(int line, int column) { super(line, column); }

    @Override
    public String toPrintString() { return "ContinueStatement [line " + getLine() + "]"; }

    @Override
    public List<PrintEntry> getPrintEntries() { return List.of(new PrintEntry.Info("continue")); }
}
