package parser.ast.nodes.type;

/// Source-level named type syntax, such as `int`, `string`, or `User`.
public final class NamedTypeSyntax extends TypeSyntax {

    private final String name;
    private final boolean primitive;

    /// Constructs a named type syntax node.
    /// @param line The line where the type name begins.
    /// @param column The column where the type name begins.
    /// @param name The parsed type name.
    /// @param primitive Whether the parsed name is a primitive type keyword.
    public NamedTypeSyntax(int line, int column, String name, boolean primitive) {

        super(line, column);
        this.name = name;
        this.primitive = primitive;
    }

    @Override
    public String getName() { return name; }

    /// Returns whether this type name came from a primitive type token.
    /// @return `true` for primitive type syntax, `false` for identifier type syntax.
    public boolean isPrimitive() { return primitive; }
}
