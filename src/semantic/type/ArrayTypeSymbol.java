package semantic.type;

import parser.ast.nodes.ExpressionNode;
import java.util.Arrays;

/// Resolved array type symbol.
public final class ArrayTypeSymbol implements TypeSymbol {

    private final TypeSymbol elementType;
    private final ExpressionNode[] sizes;

    /// Creates a resolved array type symbol.
    /// @param elementType The resolved element type.
    /// @param sizes The parsed dimension sizes, with {@code null} for unspecified dimensions.
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

    /// Returns the array element type.
    /// @return The element type.
    public TypeSymbol getElementType() { return elementType; }

    /// Returns the number of array dimensions.
    /// @return The array dimension count.
    public int getDimensions() { return sizes.length; }

    /// Returns the parsed array dimension sizes.
    /// @return A copy of the parsed dimension sizes.
    public ExpressionNode[] getSizes() { return Arrays.copyOf(sizes, sizes.length); }

    @Override
    public boolean isResolved() { return elementType != null && elementType.isResolved(); }
}
