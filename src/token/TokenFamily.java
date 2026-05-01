package src.token;

/// Represents a family of tokens in the programming language, providing a common interface for different types of tokens.
public interface TokenFamily {
    
    /// Returns the string representation of the token family, which can be used for debugging, error messages, or other purposes.
    /// @return The string representation of the token family.
    String get();
}
