package parser.ast.nodes;

/// Represents a named StatementNode in the abstract syntax tree (AST), which can be a variable, function, or object name.
///
/// Named statement nodes are usually declarations, in which a logical name is defined to hold/reference a specific value.
/// The actual value can be different types, such as a variable, function, or object. The name is used to reference the value in the program.
public abstract class NamedStatementNode extends StatementNode {
    
    private final String name;

    /// Constructs a new NamedStatementNode with the specified line, column, and name.
    /// @param line The line number in the source code where this symbol is defined.
    /// @param column The column number in the source code where this symbol starts.
    /// @param name The name of the symbol.
    public NamedStatementNode(int line, int column, String name) {

        super(line, column);
        this.name = name;
    }

    /// Returns the name of this symbol.
    /// @return The name of this symbol.
    public String getName() { return name; }
}
