package lexer.token.family.literal;

import lexer.token.family.Literal;

/// Represents an identifier literal in the programming language, which is a name used to identify variables,
/// functions, classes, and other entities (e.g., myVariable, myFunction).
public final class IdentifierLiteral extends Literal {

    /// Constructs a new IdentifierLiteral with the specified identifier name.
    /// @param identifier The name of the identifier (e.g., "myVariable", "myFunction").
    public IdentifierLiteral(String identifier) { super(identifier); }

    /// Constructs a new IdentifierLiteral with a null value, which can be used as a placeholder or default value.
    public IdentifierLiteral() { super(null); }
}