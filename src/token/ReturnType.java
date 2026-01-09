package src.token;

import src.parser.ast.nodes.ExpressionNode;

/**
 * Represents a complete type specification including base type and array dimensions.
 * For example: int, int[], Animal, Animal[][]
 */
public class ReturnType {
    
    private final TokenFamily baseType;
    private final ExpressionNode[] arraySizes;
    private final ReturnType[] superTypes;
    private final ReturnType genericParameterType;

    public ReturnType(TokenFamily baseType, ExpressionNode[] arraySizes, ReturnType[] superTypes, ReturnType genericParameterType) {

        this.baseType = baseType;
        this.arraySizes = arraySizes;
        this.superTypes = superTypes;
        this.genericParameterType = genericParameterType;
    }
    
    /**
     * Create a simple return type without arrays.
     */
    public ReturnType(TokenFamily baseType) { this(baseType, new ExpressionNode[0], null, null); }
    
    /**
     * Get the base type (primitive or custom class).
     */
    public TokenFamily getBaseType() { return baseType; }
    
    /**
     * Get array dimension sizes.
     */
    public ExpressionNode[] getSizes() { return arraySizes; }

    /**
     * Get the super type if any (for classes).
     */
    public ReturnType[] getSuperTypes() { return superTypes; }

    /**
     * Get the generic parameter type if any.
     */
    public ReturnType getGenericParameterType() { return genericParameterType; }
    
    /**
     * Check if this is an array type.
     */
    public boolean isArray() { return arraySizes.length > 0; }
    
    /**
     * Get the number of array dimensions.
     */
    public int getArrayDimensions() { return arraySizes.length; }
    
    @Override
    public String toString() {

        var sb = new StringBuilder(baseType.get());
        for (var i = 0; i < arraySizes.length; i++) sb.append("[" + arraySizes[i] + "]");
        return sb.toString();
    }
}
