package parser.ast.nodes.expression.literal;


/// Represents an identifier literal expression in the AST.
///
/// An identifier literal expression represents a reference to a variable, function, or other named entity in the source code.
/// It consists of a name that identifies the entity being referenced.
public class IdentifierLiteralExpression extends ExpressionNode {
    
    String name;

    /// Constructs a new IdentifierLiteralExpression with the specified line, column, and name.
    /// @param line The line number in the source code where this expression occurs.
    /// @param column The column number in the source code where this expression starts.
    /// @param name The name of the identifier being referenced.
    public IdentifierLiteralExpression(int line, int column, String name) {

        super(line, column);
        this.name = name;
    }

    /// Returns the name of the identifier being referenced by this expression.
    /// @return The name of the identifier.
    public String getName() { return name; }

    @Override
    public <T> T accept(NodeVisitor<T> visitor) { return visitor.visitIdentifierLiteral(this); }
}
