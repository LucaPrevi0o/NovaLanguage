package semantic.declaration;

import lexer.token.ReturnType;
import parser.ast.AstNode;

/// A declaration discovered from the AST before name resolution or duplicate validation.
/// @param kind The kind of declaration (e.g., class, method, variable).
/// @param name The name of the declaration.
/// @param declaredType The return type of the declaration, or null if not applicable.
/// @param node The AST node that represents the declaration.
/// @param owner The AST node that owns the declaration, or `null` if it has no owner.
public record SemanticDeclaration(DeclarationKind kind, String name, ReturnType declaredType, AstNode node, AstNode owner) {

    /// Returns the line number of the declaration in the source code, or `-1` if not available.
    ///
    /// @return The line number of the declaration in the source code, or `-1` if not available.
    public int getLine() { return node != null ? node.getLine() : -1; }

    /// Returns the column number of the declaration in the source code, or `-1` if not available.
    ///
    /// @return The column number of the declaration in the source code, or `-1` if not available.
    public int getColumn() { return node != null ? node.getColumn() : -1; }
}
