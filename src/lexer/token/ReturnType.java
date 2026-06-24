package lexer.token;

import parser.ast.nodes.ExpressionNode;
import parser.ast.nodes.type.TypeSyntax;

/// Temporary compatibility adapter for declaration type syntax.
///
/// Parser-created instances should carry the source {@link TypeSyntax} that produced them.
/// Legacy token-class metadata remains only for older AST/printer callers and syntaxless
/// adapters; semantic type resolution treats the parsed syntax as authoritative when present.
public class ReturnType {
    
    private final TokenClass tokenClass;
    private final ExpressionNode[] arraySizes;
    private final ReturnType[] superTypes;
    private final ReturnType genericParameterType;
    private final TypeSyntax syntax;

    /// Create a return type with all details specified.
    /// @param tokenClass The base type (primitive or custom class).
    /// @param arraySizes The sizes of each array dimension (empty if not an array).
    /// @param superTypes The super types if this is a class type (null if not a class).
    /// @param genericParameterType The generic parameter type if this is a generic type (null if not generic).
    public ReturnType(TokenClass tokenClass, ExpressionNode[] arraySizes, ReturnType[] superTypes, ReturnType genericParameterType) {

        this(tokenClass, arraySizes, superTypes, genericParameterType, null);
    }

    /// Create a return type with all details specified and a parsed source type syntax node.
    /// @param tokenClass The base type (primitive or custom class).
    /// @param arraySizes The sizes of each array dimension (empty if not an array).
    /// @param superTypes The super types if this is a class type (null if not a class).
    /// @param genericParameterType The generic parameter type if this is a generic type (null if not generic).
    /// @param syntax The source-level parsed type syntax, or {@code null} for inferred/semantic-only types.
    public ReturnType(TokenClass tokenClass, ExpressionNode[] arraySizes, ReturnType[] superTypes, ReturnType genericParameterType, TypeSyntax syntax) {

        this.tokenClass = tokenClass;
        this.arraySizes = arraySizes;
        this.superTypes = superTypes;
        this.genericParameterType = genericParameterType;
        this.syntax = syntax;
    }

    /// Create a simple return type with just a base type (no arrays, no generics).
    /// @param tokenClass The base type (primitive or custom class).
    public ReturnType(TokenClass tokenClass) { this(tokenClass, new ExpressionNode[0], null, null); }

    /// Get the base type (primitive or custom class).
    /// @return The base type of this return type.
    public TokenClass getTokenClass() { return tokenClass; }

    /// Get the array sizes if this is an array type.
    /// @return An array of ExpressionNodes representing the sizes of each array dimension, or an empty array if this is not an array type.
    public ExpressionNode[] getSizes() { return arraySizes; }

    /// Get the super types if this is a class type.
    /// @return An array of ReturnTypes representing the super types of this class type, or null if this is not a class type.
    public ReturnType[] getSuperTypes() { return superTypes; }

    /// Get the generic parameter type if this is a generic type.
    /// @return The ReturnType representing the generic parameter type, or null if this is not a generic type.
    public ReturnType getGenericParameterType() { return genericParameterType; }

    /// Get the parsed source type syntax that produced this ReturnType, when available.
    /// @return The parsed type syntax, or {@code null} for inferred/semantic-only types.
    public TypeSyntax getSyntax() { return syntax; }

    /// Check if this return type represents an array type.
    /// @return true if this return type is an array type (i.e., has one or more array dimensions), false otherwise.
    public boolean isArray() { return arraySizes.length > 0; }

    @Override
    public String toString() {

        var sb = new StringBuilder(tokenClass.token());
        for (var arraySize : arraySizes) sb.append("[").append(arraySize).append("]");
        return sb.toString();
    }
}
