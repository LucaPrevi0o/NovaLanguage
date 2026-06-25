package semantic.type.symbol;

import parser.ast.nodes.ExpressionNode;
import semantic.type.TypeKind;

import java.util.Arrays;

/// Resolved array type symbol.
/// @param elementType The resolved element type.
/// @param sizes The parsed dimension sizes, with {@code null} for unspecified dimensions.
public record ArrayTypeSymbol(TypeSymbol elementType, ExpressionNode[] sizes) implements TypeSymbol {

    /// Creates a resolved array type symbol.
    ///
    /// @param elementType The resolved element type.
    /// @param sizes       The parsed dimension sizes, with {@code null} for unspecified dimensions.
    public ArrayTypeSymbol(TypeSymbol elementType, ExpressionNode[] sizes) {

        this.elementType = elementType;
        this.sizes = sizes != null ? Arrays.copyOf(sizes, sizes.length) : new ExpressionNode[0];
    }

    @Override
    public String getName() {

        var name = new StringBuilder(elementType != null ? elementType.getName() : "<unknown>");
        for (var ignored : sizes) name.append("[]");
        return name.toString();
    }

    @Override
    public TypeKind getKind() { return TypeKind.ARRAY; }

    @Override
    public TypeSymbol elementType() { return elementType; }

    /// Returns the number of array dimensions.
    ///
    /// @return The array dimension count.
    public int getDimensions() { return sizes.length; }

    @Override
    public ExpressionNode[] sizes() { return Arrays.copyOf(sizes, sizes.length); }

    @Override
    public boolean isResolved() { return elementType != null && elementType.isResolved(); }
}
