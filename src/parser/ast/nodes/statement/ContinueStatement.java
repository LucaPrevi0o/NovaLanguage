package parser.ast.nodes.statement;

import parser.ast.Printable;
import parser.ast.nodes.StatementNode;
import parser.ast.visitor.NodeVisitor;

import java.util.List;

public class ContinueStatement extends StatementNode implements Printable {

    public ContinueStatement(int line, int column) {
        super(line, column);
    }

    @Override
    public String toPrintString() { return "ContinueStatement [line " + getLine() + "]"; }

    @Override
    public List<PrintEntry> getPrintEntries() { return List.of(new PrintEntry.Info("continue")); }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) { return visitor.visitContinue(this); }
}
