package parser.ast.nodes.type;

import parser.ast.nodes.ExpressionNode;

/// Source-level array type syntax, such as `int[]` or `User[10]`.
public final class ArrayTypeSyntax extends TypeSyntax {

    private final TypeSyntax elementType;
    private final ExpressionNode[] sizes;

    /// Constructs an array type syntax node.
    /// @param line The line where the array type begins.
    /// @param column The column where the array type begins.
    /// @param elementType The parsed element type syntax.
    /// @param sizes The parsed array dimension sizes, with {@code null} for unspecified dimensions.
    public ArrayTypeSyntax(int line, int column, TypeSyntax elementType, ExpressionNode[] sizes) {

        super(line, column);
        this.elementType = elementType;
        this.sizes = sizes;
    }

    @Override
    public String getName() { return elementType.getName(); }

    /// Returns the parsed element type syntax.
    /// @return The element type.
    public TypeSyntax getElementType() { return elementType; }

    /// Returns parsed array dimension sizes.
    /// @return The array dimension sizes.
    public ExpressionNode[] getSizes() { return sizes; }
}
