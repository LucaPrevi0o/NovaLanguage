package parser.ast.nodes.type;

import parser.ast.AstNode;

/// Base AST node for source-level type syntax.
///
/// Type syntax preserves what the parser saw before semantic analysis resolves
/// the name to a concrete type symbol.
public abstract class TypeSyntax extends AstNode {

    /// Constructs a type syntax node at the given source position.
    /// @param line The line where the type syntax begins.
    /// @param column The column where the type syntax begins.
    protected TypeSyntax(int line, int column) { super(line, column); }

    /// Returns the source-level type name.
    /// @return The parsed type name.
    public abstract String getName();
}
