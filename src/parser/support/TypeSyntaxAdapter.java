package parser.support;

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

/// Converts parsed type syntax to the temporary ReturnType representation used by existing AST nodes.
public final class TypeSyntaxAdapter {

    private TypeSyntaxAdapter() { }

    /// Converts a parsed type syntax node to a ReturnType.
    /// @param syntax The parsed type syntax.
    /// @return A ReturnType adapter for the parsed type syntax.
    public static ReturnType toReturnType(TypeSyntax syntax) {

        return switch (syntax) {

            case ArrayTypeSyntax arrayType -> toArrayReturnType(arrayType);
            case GenericTypeSyntax genericType -> new ReturnType(
                new GenericParameterType(genericType.getName()),
                new ExpressionNode[0],
                null,
                null,
                genericType
            );
            case NamedTypeSyntax namedType -> toNamedReturnType(namedType, new ExpressionNode[0], namedType);
            case null -> null;
            default -> throw new IllegalArgumentException("Unsupported type syntax: " + syntax.getClass().getName());
        };
    }

    private static ReturnType toArrayReturnType(ArrayTypeSyntax arrayType) {

        return toNamedReturnType(arrayType.getElementType(), arrayType.getSizes(), arrayType);
    }

    private static ReturnType toNamedReturnType(TypeSyntax syntax, ExpressionNode[] sizes, TypeSyntax sourceSyntax) {

        if (syntax instanceof GenericTypeSyntax genericType)
            return new ReturnType(new GenericParameterType(genericType.getName()), sizes, null, null, sourceSyntax);

        if (!(syntax instanceof NamedTypeSyntax namedType))
            throw new IllegalArgumentException("Array element type must be named type syntax");

        if (namedType.isPrimitive())
            return new ReturnType(primitiveType(namedType.getName()), sizes, null, null, sourceSyntax);

        return new ReturnType(new NonPrimitiveType(namedType.getName()), sizes, null, null, sourceSyntax);
    }

    private static TokenClass primitiveType(String name) {

        for (var primitive : PrimitiveType.values())
            if (primitive.token().equals(name)) return primitive;
        throw new IllegalArgumentException("Unknown primitive type syntax: " + name);
    }
}
