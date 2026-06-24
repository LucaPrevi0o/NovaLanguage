package parser.ast.nodes.type;

/// Source-level generic parameter syntax declared by a class header, such as `T` in `class Box[T]`.
public final class GenericTypeSyntax extends TypeSyntax {

    private final String name;

    /// Constructs a generic parameter type syntax node.
    /// @param line The line where the generic parameter name begins.
    /// @param column The column where the generic parameter name begins.
    /// @param name The parsed generic parameter name.
    public GenericTypeSyntax(int line, int column, String name) {

        super(line, column);
        this.name = name;
    }

    @Override
    public String getName() { return name; }
}
