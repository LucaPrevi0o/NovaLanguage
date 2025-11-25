package src.token;

import src.parser.ast.nodes.ExpressionNode;

/**
 * Represents a complete type specification including base type and array dimensions.
 * For example: int, int[], Animal, Animal[][]
 */
public class ReturnType {
    
    private final TokenFamily baseType;
    private final ExpressionNode[] arraySizes;
    
    /**
     * Create a return type with a TokenFamily base and optional array dimensions.
     */
    public ReturnType(TokenFamily baseType, ExpressionNode[] arraySizes) {

        this.baseType = baseType;
        this.arraySizes = arraySizes != null ? arraySizes : new ExpressionNode[0];
    }
    
    /**
     * Create a simple return type without arrays.
     */
    public ReturnType(TokenFamily baseType) { this(baseType, new ExpressionNode[0]); }
    
    /**
     * Get the base type (primitive or custom class).
     */
    public TokenFamily getBaseType() { return baseType; }
    
    /**
     * Get array dimension sizes.
     */
    public ExpressionNode[] getSizes() { return arraySizes; }
    
    /**
     * Check if this is an array type.
     */
    public boolean isArray() {
        return arraySizes.length > 0;
    }
    
    /**
     * Get the number of array dimensions.
     */
    public int getArrayDimensions() {
        return arraySizes.length;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(baseType.get());
        for (int i = 0; i < arraySizes.length; i++) {
            sb.append("[]");
        }
        return sb.toString();
    }
}
