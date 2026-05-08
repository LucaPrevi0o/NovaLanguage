package lexer.token.family;

import lexer.token.TokenFamily;

/// Represents a generic parameter type in the programming language, allowing for the use of type parameters in generic classes and functions.
public class GenericParameterType implements TokenFamily {

    private final String parameterName;

    /// Constructs a new GenericParameterType with the specified parameter name.
    /// @param parameterName The name of the generic parameter (e.g., "T", "E").
    public GenericParameterType(String parameterName) { this.parameterName = parameterName; }

    @Override
    public String get() { return parameterName; }
}
