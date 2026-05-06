package parser.ast;

import parser.ast.visitor.NodeVisitor;

/// Represents a node in the abstract syntax tree (AST) of the source code.
/// Each node contains information about its position in the source code (line and column).
public abstract class AstNode {

    private final int line;
    private final int column;

    /// Constructs a new AstNode with the specified line and column numbers.
    /// @param line The line number in the source code where this node occurs.
    /// @param column The column number in the source code where this node starts.
    public AstNode(int line, int column) {

        this.line = line;
        this.column = column;
    }

    /// Returns the line number in the source code where this node occurs.
    /// @return The line number.
    public int getLine() { return line; }

    /// Returns the column number in the source code where this node starts.
    /// @return The column number.
    public int getColumn() { return column; }

    /// Accepts a visitor, calling the appropriate visit method for this node type.
    /// @param <T>     The return type of the visitor.
    /// @param visitor The visitor to accept.
    /// @return The result produced by the visitor.
    public abstract <T> T accept(NodeVisitor<T> visitor);

    @Override
    public String toString() { return this.getClass().getSimpleName() + " (line: " + line + ", column: " + column + ")"; }
}
