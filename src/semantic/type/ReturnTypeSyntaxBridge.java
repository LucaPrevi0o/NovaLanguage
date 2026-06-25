package semantic.type;

import lexer.token.ReturnType;
import lexer.token.TokenClass;
import lexer.token.family.GenericParameterType;
import lexer.token.family.NonPrimitiveType;
import lexer.token.family.PrimitiveType;
import parser.ast.nodes.ExpressionNode;
import parser.ast.nodes.type.ArrayTypeSyntax;
import parser.ast.nodes.type.GenericTypeSyntax;
import parser.ast.nodes.type.NamedTypeSyntax;
import parser.ast.nodes.type.TypeSyntax;

/// Converts syntaxless `ReturnType` compatibility adapters into source-level `TypeSyntax`.
///
/// This is the semantic layer's only intended fallback boundary for reading legacy lexer-token
/// metadata from `ReturnType`. Semantic passes should resolve the returned `TypeSyntax` into
/// `TypeSymbol` objects instead of making type decisions from token classes directly.
public final class ReturnTypeSyntaxBridge {

    private static final int SYNTHETIC_POSITION = -1;

    private ReturnTypeSyntaxBridge() {}

    /// Returns source type syntax for a `ReturnType` adapter.
    /// @param type The compatibility adapter to convert.
    /// @return The adapter's parsed syntax when present, synthesized syntax from legacy metadata
    /// otherwise, or {@code null} when the adapter has no usable type metadata.
    public static TypeSyntax toTypeSyntax(ReturnType type) {

        if (type == null) return null;
        if (type.getSyntax() != null) return type.getSyntax();

        var baseSyntax = toBaseTypeSyntax(type.getTokenClass());
        if (baseSyntax == null) return null;

        var sizes = sizes(type);
        return sizes.length > 0
                ? new ArrayTypeSyntax(SYNTHETIC_POSITION, SYNTHETIC_POSITION, baseSyntax, sizes)
                : baseSyntax;
    }

    /// Returns whether a source type syntax node represents primitive `void`.
    /// @param syntax The source type syntax to inspect.
    /// @return `true` for primitive `void` syntax.
    public static boolean isVoid(TypeSyntax syntax) {

        return syntax instanceof NamedTypeSyntax namedType &&
                namedType.isPrimitive() &&
                "void".equals(namedType.getName());
    }

    /// Returns whether a compatibility adapter represents primitive `void`.
    /// @param type The compatibility adapter to inspect.
    /// @return `true` for adapters that convert to primitive `void` syntax.
    public static boolean isVoid(ReturnType type) { return isVoid(toTypeSyntax(type)); }

    /// Returns the generic parameter name represented by a compatibility adapter.
    /// @param type The compatibility adapter to inspect.
    /// @return The generic parameter name, or an empty string when the adapter does not represent one.
    public static String genericParameterName(ReturnType type) {

        return toTypeSyntax(type) instanceof GenericTypeSyntax genericType ? genericType.getName() : "";
    }

    private static TypeSyntax toBaseTypeSyntax(TokenClass tokenClass) {

        return switch (tokenClass) {

            case PrimitiveType primitiveType ->
                    new NamedTypeSyntax(SYNTHETIC_POSITION, SYNTHETIC_POSITION, primitiveType.token(), true);
            case NonPrimitiveType(String typeName) ->
                    new NamedTypeSyntax(SYNTHETIC_POSITION, SYNTHETIC_POSITION, typeName, false);
            case GenericParameterType(String typeName) ->
                    new GenericTypeSyntax(SYNTHETIC_POSITION, SYNTHETIC_POSITION, typeName);
            case null, default -> null;
        };
    }

    private static ExpressionNode[] sizes(ReturnType type) {

        var sizes = type.getSizes();
        return sizes != null ? sizes : new ExpressionNode[0];
    }
}
