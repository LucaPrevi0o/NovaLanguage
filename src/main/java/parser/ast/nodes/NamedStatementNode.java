package parser.ast.nodes;

/// Represents a named statement node in the abstract syntax tree (AST).
///
/// Named statement nodes are usually source declarations, such as variables, functions,
/// parameters, or classes. This node only preserves the source-level name; semantic
/// declaration identity, duplicate detection, and visibility belong to the semantic layer.
public abstract class NamedStatementNode extends StatementNode {
    
    private final String name;

    /// Constructs a new NamedStatementNode with the specified line, column, and name.
    /// @param line The line number in the source code where this named statement begins.
    /// @param column The column number in the source code where this named statement begins.
    /// @param name The source-level name.
    public NamedStatementNode(int line, int column, String name) {

        super(line, column);
        this.name = name;
    }

    /// Returns the source-level name.
    /// @return The source-level name.
    public String getName() { return name; }
}
