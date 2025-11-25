package src.token;

/**
 * Interface for all token type families.
 * Can be implemented by enums (primitive types) or classes (custom class types).
 */
public interface TokenFamily {
    
    /**
     * Get the name of this type family.
     */
    String get();
}
