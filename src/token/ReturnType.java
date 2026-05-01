package src.token;

import src.parser.ast.nodes.ExpressionNode;

/// Represents the return type of a declaration, which can be a primitive type, a non-primitive type, an array type, or a generic type.
public class ReturnType {
    
    private final TokenFamily baseType;
    private final ExpressionNode[] arraySizes;
    private final ReturnType[] superTypes;
    private final ReturnType genericParameterType;

    /// Create a return type with all details specified.
    /// @param baseType The base type (primitive or custom class).
    /// @param arraySizes The sizes of each array dimension (empty if not an array).
    /// @param superTypes The super types if this is a class type (null if not a class).
    /// @param genericParameterType The generic parameter type if this is a generic type (null if not generic).
    public ReturnType(TokenFamily baseType, ExpressionNode[] arraySizes, ReturnType[] superTypes, ReturnType genericParameterType) {

        this.baseType = baseType;
        this.arraySizes = arraySizes;
        this.superTypes = superTypes;
        this.genericParameterType = genericParameterType;
    }

    /// Create a simple return type with just a base type (no arrays, no generics).
    /// @param baseType The base type (primitive or custom class).
    public ReturnType(TokenFamily baseType) { this(baseType, new ExpressionNode[0], null, null); }

    /// Get the base type (primitive or custom class).
    /// @return The base type of this return type.
    public TokenFamily getBaseType() { return baseType; }

    /// Get the array sizes if this is an array type.
    /// @return An array of ExpressionNodes representing the sizes of each array dimension, or an empty array if this is not an array type.
    public ExpressionNode[] getSizes() { return arraySizes; }

    /// Get the super types if this is a class type.
    /// @return An array of ReturnTypes representing the super types of this class type, or null if this is not a class type.
    public ReturnType[] getSuperTypes() { return superTypes; }

    /// Get the generic parameter type if this is a generic type.
    /// @return The ReturnType representing the generic parameter type, or null if this is not a generic type.
    public ReturnType getGenericParameterType() { return genericParameterType; }

    /// Check if this return type represents an array type.
    /// @return true if this return type is an array type (i.e., has one or more array dimensions), false otherwise.
    public boolean isArray() { return arraySizes.length > 0; }

    @Override
    public String toString() {

        var sb = new StringBuilder(baseType.get());
        for (ExpressionNode arraySize : arraySizes) sb.append("[").append(arraySize).append("]");
        return sb.toString();
    }
}
