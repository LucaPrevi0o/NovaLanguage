package token.family;

import token.TokenFamily;

/// Represents access modifiers in the programming language, such as public, private, and protected.
public enum AccessModifier implements TokenFamily { 

    /// The public access modifier, allowing access from any other code.
    PUBLIC("public"),

        /// The private access modifier, restricting access to within the same class.
    PRIVATE("private"),

    /// The protected access modifier, allowing access from subclasses and classes in the same package.
    PROTECTED("protected");

    private final String keyword;

    /// Constructs a new AccessModifier with the specified keyword.
    /// @param keyword The string representation of the access modifier (e.g., "public", "private").
    AccessModifier(String keyword) { this.keyword = keyword; }

    @Override
    public String get() { return keyword; }
}
