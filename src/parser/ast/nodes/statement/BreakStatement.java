package parser.ast.nodes.statement;

import parser.ast.Printable;
import parser.ast.nodes.StatementNode;
import parser.ast.visitor.NodeVisitor;

import java.util.List;

public class BreakStatement extends StatementNode implements Printable {

    public BreakStatement(int line, int column) {
        super(line, column);
    }

    @Override
    public String toPrintString() { return "BreakStatement [line " + getLine() + "]"; }

    @Override
    public List<PrintEntry> getPrintEntries() { return List.of(new PrintEntry.Info("break")); }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) { return visitor.visitBreak(this); }
}
