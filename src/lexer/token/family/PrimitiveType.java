package lexer.token.family;

import lexer.token.TokenFamily;

/// Represents primitive types in the programming language, such as int, long, byte, float, double, char, bool, and void.
public enum PrimitiveType implements TokenFamily {

    /// The int primitive type, representing integer values.
    INT("int"),

    /// The long primitive type, representing long integer values.
    LONG("long"),

    /// The byte primitive type, representing byte values.
    BYTE("byte"),

    /// The float primitive type, representing single-precision floating-point values.
    FLOAT("float"),

    /// The double primitive type, representing double-precision floating-point values.
    DOUBLE("double"),

    /// The char primitive type, representing single character values.
    CHAR("char"),

    /// The bool primitive type, representing boolean values (true or false).
    BOOL("bool"),

    /// The void primitive type, representing the absence of a value or return type.
    VOID("void"),

    /// The string primitive type, representing a sequence of characters.
    STRING("string");

    private final String name;

    /// Constructs a new PrimitiveType with the specified string representation.
    /// @param name The string representation of the primitive type (e.g., "int", "long", "float").
    PrimitiveType(String name) { this.name = name; }

    @Override
    public String get() {  return name; }
}
