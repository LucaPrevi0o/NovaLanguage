package parser.ast.nodes.statement.conditional;

import parser.ast.Printable;
import parser.ast.nodes.ExpressionNode;
import parser.ast.nodes.statement.ConditionalStatement;
import parser.ast.visitor.NodeVisitor;

import java.util.List;

/// Represents a switch statement: {@code switch (expr) { case v -> body; default -> body; }}.
///
/// The "condition" (inherited from {@link ConditionalStatement}) is the switched-on expression.
/// Each arm is represented by a {@link SwitchCase}.
public class SwitchStatement extends ConditionalStatement implements Printable {

    private final SwitchCase[] cases;

    /// Constructs a new SwitchStatement.
    /// @param line      Line number of the {@code switch} keyword.
    /// @param column    Column number of the {@code switch} keyword.
    /// @param subject   The expression being switched on.
    /// @param cases     The list of case / default arms.
    public SwitchStatement(int line, int column, ExpressionNode subject, SwitchCase[] cases) {

        super(line, column, subject);
        this.cases = cases;
    }

    /// Returns the expression being switched on.
    /// @return The switch subject expression.
    public ExpressionNode getSubject() { return getCondition(); }

    /// Returns all case arms (including a possible default arm).
    /// @return An array of {@link SwitchCase} objects.
    public SwitchCase[] getCases() { return cases; }

    @Override
    public String toPrintString() { return "SwitchStatement [line " + getLine() + "]"; }

    @Override
    public List<PrintEntry> getPrintEntries() {
        return List.of(
            new PrintEntry.Child("Subject", getSubject()),
            new PrintEntry.Children("Cases", cases)
        );
    }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) { return visitor.visitSwitch(this); }
}
