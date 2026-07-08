package parser.ast.nodes.statement.conditional;

import parser.ast.Printable;
import parser.ast.nodes.ExpressionNode;
import parser.ast.nodes.StatementNode;
import parser.ast.nodes.statement.ConditionalStatement;
import parser.ast.nodes.type.TypeSyntax;

import java.util.List;

import static printer.AstPrinter.buildTypeStringWithSizes;

/// Represents a for-each loop statement: {@code for (Type name : iterable) body}.
public class ForEachStatement extends ConditionalStatement implements Printable {

    private final TypeSyntax elementTypeSyntax;
    private final String elementName;
    private final StatementNode body;

    /// Constructs a new ForEachStatement.
    /// @param line        The line number where this statement occurs.
    /// @param column      The column number where this statement starts.
    /// @param elementType The declared type of the loop variable.
    /// @param elementName The name of the loop variable.
    /// @param iterable    The expression that produces the collection to iterate over.
    /// @param body        The loop body.
    public ForEachStatement(int line, int column, TypeSyntax elementType, String elementName,
                            ExpressionNode iterable, StatementNode body) {

        super(line, column, iterable);
        this.elementTypeSyntax = elementType;
        this.elementName = elementName;
        this.body = body;
    }

    /// Returns the parsed source type syntax for the loop variable.
    /// @return The parsed element type syntax.
    public TypeSyntax getElementTypeSyntax() { return elementTypeSyntax; }

    /// Returns the name of the loop variable.
    /// @return The element name.
    public String getElementName() { return elementName; }

    /// Returns the iterable expression (stored as the {@code condition} of the base class).
    /// @return The iterable expression.
    public ExpressionNode getIterable() { return getCondition(); }

    /// Returns the loop body.
    /// @return The body statement.
    public StatementNode getBody() { return body; }

    @Override
    public String toPrintString() { return "ForEachStatement [line " + getLine() + "]"; }

    @Override
    public List<PrintEntry> getPrintEntries() {
        return List.of(
            new PrintEntry.Info("Element Type: " + buildTypeStringWithSizes(getElementTypeSyntax())),
            new PrintEntry.Info("Element Name: " + elementName),
            new PrintEntry.Child("Iterable", getIterable()),
            new PrintEntry.Child("Body", body)
        );
    }
}
