package semantic.declaration;

/// Kinds of declarations discovered during semantic declaration collection.
public enum DeclarationKind {

    /// A class declaration.
    CLASS,

    /// A constructor declaration.
    CONSTRUCTOR,

    /// A field declaration (e.g., a member variable of a class).
    FIELD,

    /// A method declaration (e.g., a member function of a class).
    METHOD,

    /// A function declaration.
    FUNCTION,

    /// A parameter declaration (e.g., a parameter of a function or method).
    PARAMETER,

    /// A variable declaration (e.g., a local variable in a function or method).
    VARIABLE,

    /// A variable declaration in a foreach statement.
    FOREACH_VARIABLE
}
