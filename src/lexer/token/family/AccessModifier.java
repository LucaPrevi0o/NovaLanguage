package lexer.token.family;

import lexer.token.TokenClass;

/// Represents access modifiers in the programming language, such as public, private, and protected.
public enum AccessModifier implements TokenClass {

    /// The public access modifier, allowing access from any other code.
    PUBLIC("public"),

        /// The private access modifier, restricting access to within the same class.
    PRIVATE("private"),

    /// The protected access modifier, allowing access from subclasses and classes in the same package.
    PROTECTED("protected");

    private final String accessModifier;

    /// Constructs a new AccessModifier with the specified accessModifier.
    /// @param accessModifier The string representation of the access modifier (e.g., "public", "private").
    AccessModifier(String accessModifier) { this.accessModifier = accessModifier; }

    @Override
    public String token() { return accessModifier; }
}
