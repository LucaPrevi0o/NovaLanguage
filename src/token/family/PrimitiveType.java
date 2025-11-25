package src.token.family;

import src.token.TokenFamily;

/**
 * Enum for primitive types (int, float, double, etc.).
 * Implements TokenFamily to be treated uniformly with custom class types.
 */
public enum PrimitiveType implements TokenFamily {

    INT("int"),
    LONG("long"),
    BYTE("byte"),
    FLOAT("float"), 
    DOUBLE("double"), 
    CHAR("char"),
    BOOL("bool"),
    VOID("void");

    private final String name;

    PrimitiveType(String name) { this.name = name; }

    @Override
    public String get() {  return name; }
}
