package semantic.type;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticPhase;
import lexer.token.ReturnType;
import lexer.token.family.GenericParameterType;
import lexer.token.family.NonPrimitiveType;
import lexer.token.family.PrimitiveType;
import parser.ast.AstNode;
import parser.ast.nodes.statement.ClassDeclarationStatement;
import parser.ast.nodes.type.ArrayTypeSyntax;
import parser.ast.nodes.type.GenericTypeSyntax;
import parser.ast.nodes.type.NamedTypeSyntax;
import parser.ast.nodes.type.TypeSyntax;
import semantic.declaration.DeclarationKind;
import semantic.declaration.SemanticDeclaration;
import semantic.scope.SemanticScope;
import semantic.type.symbol.ArrayTypeSymbol;
import semantic.type.symbol.GenericParameterSymbol;
import semantic.type.symbol.TypeSymbol;
import semantic.type.symbol.ValueTypeSymbol;
import semantic.type.symbol.UnknownTypeSymbol;
import semantic.type.symbol.ClassTypeSymbol;

import java.util.List;

/// Resolves parsed type syntax and temporary ReturnType adapters into semantic type symbols.
///
/// Parser-created ReturnType adapters are source-syntax-first: when a ReturnType carries
/// TypeSyntax, the syntax is resolved before legacy token-class metadata is considered.
public final class TypeResolver {

    /// Resolves a ReturnType adapter as a normal declared type.
    /// @param type The ReturnType adapter to resolve.
    /// @param owner The AST node that owns the type.
    /// @param scope The semantic scope used for class lookup.
    /// @return The resolved semantic type and any diagnostics.
    public TypeResolution resolve(ReturnType type, AstNode owner, SemanticScope scope) {

        return resolve(type, owner, scope, "type");
    }

    /// Resolves a ReturnType adapter with a custom unresolved-name label.
    /// @param type The ReturnType adapter to resolve.
    /// @param owner The AST node that owns the type.
    /// @param scope The semantic scope used for class lookup.
    /// @param unresolvedLabel The diagnostic label to use when a name cannot be resolved.
    /// @return The resolved semantic type and any diagnostics.
    public TypeResolution resolve(ReturnType type, AstNode owner, SemanticScope scope, String unresolvedLabel) {

        if (type == null) return new TypeResolution(null, List.of());
        if (type.getSyntax() != null) return resolve(type.getSyntax(), owner, scope, unresolvedLabel);

        var baseResolution = resolveReturnTypeBase(type, owner, scope, unresolvedLabel);
        var symbol = baseResolution.type();
        if (type.getSizes().length > 0)
            symbol = new ArrayTypeSymbol(symbol, type.getSizes());
        return new TypeResolution(symbol, baseResolution.diagnostics());
    }

    /// Resolves source-level type syntax as a normal declared type.
    /// @param syntax The parsed type syntax to resolve.
    /// @param owner The AST node that owns the type syntax.
    /// @param scope The semantic scope used for class lookup.
    /// @return The resolved semantic type and any diagnostics.
    public TypeResolution resolve(TypeSyntax syntax, AstNode owner, SemanticScope scope) {

        return resolve(syntax, owner, scope, "type");
    }

    /// Resolves source-level type syntax with a custom unresolved-name label.
    /// @param syntax The parsed type syntax to resolve.
    /// @param owner The AST node that owns the type syntax.
    /// @param scope The semantic scope used for class lookup.
    /// @param unresolvedLabel The diagnostic label to use when a name cannot be resolved.
    /// @return The resolved semantic type and any diagnostics.
    public TypeResolution resolve(TypeSyntax syntax, AstNode owner, SemanticScope scope, String unresolvedLabel) {

        return switch (syntax) {

            case ArrayTypeSyntax arrayType -> {

                var elementResolution = resolve(arrayType.getElementType(), owner, scope, unresolvedLabel);
                yield new TypeResolution(
                    new ArrayTypeSymbol(elementResolution.type(), arrayType.getSizes()),
                    elementResolution.diagnostics()
                );
            }
            case GenericTypeSyntax genericType -> resolved(new GenericParameterSymbol(genericType.getName()));
            case NamedTypeSyntax namedType when namedType.isPrimitive() -> resolved(ValueTypeSymbol.builtin(namedType.getName()));
            case NamedTypeSyntax namedType -> resolveNamedType(namedType, owner, scope, unresolvedLabel);
            case null -> new TypeResolution(null, List.of());
            default -> throw new IllegalArgumentException("Unsupported type syntax: " + syntax.getClass().getName());
        };
    }

    /// Resolves a class name against visible semantic class declarations.
    /// @param className The class name to resolve.
    /// @param owner The AST node that owns the class reference.
    /// @param scope The semantic scope used for class lookup.
    /// @param unresolvedLabel The diagnostic label to use when the class cannot be resolved.
    /// @return The resolved class type or an unknown placeholder with diagnostics.
    public TypeResolution resolveClassName(String className, AstNode owner, SemanticScope scope, String unresolvedLabel) {

        var declaration = visibleClass(scope, className);
        if (declaration != null) return resolved(new ClassTypeSymbol(className, declaration));

        return new TypeResolution(
            new UnknownTypeSymbol(className),
            List.of(Diagnostic.error(
                DiagnosticPhase.SEMANTIC,
                "Undefined " + unresolvedLabel + " '" + className + "'",
                owner != null ? owner.getLine() : -1,
                owner != null ? owner.getColumn() : -1
            ))
        );
    }

    /// Resolves the base type of a ReturnType adapter, ignoring array dimensions.
    /// @param type The ReturnType adapter to resolve.
    /// @param owner The AST node that owns the type.
    /// @param scope The semantic scope used for class lookup.
    /// @param unresolvedLabel The diagnostic label to use when a name cannot be resolved.
    /// @return The resolved semantic type and any diagnostics.
    private TypeResolution resolveReturnTypeBase(ReturnType type, AstNode owner, SemanticScope scope, String unresolvedLabel) {

        var tokenClass = type.getTokenClass();
        if (tokenClass instanceof PrimitiveType primitiveType) return resolved(ValueTypeSymbol.builtin(primitiveType.token()));
        if (tokenClass instanceof GenericParameterType(String name)) return resolved(new GenericParameterSymbol(name));
        if (tokenClass instanceof NonPrimitiveType(String typeName)) return resolveClassName(typeName, owner, scope, unresolvedLabel);

        return resolved(new UnknownTypeSymbol("<unknown>"));
    }

    /// Resolves a named type syntax, checking for visible generic parameters before class declarations.
    /// @param namedType The named type syntax to resolve.
    /// @param owner The AST node that owns the named type.
    /// @param scope The semantic scope used for class lookup.
    /// @param unresolvedLabel The diagnostic label to use when a name cannot be resolved.
    /// @return The resolved semantic type and any diagnostics.
    private TypeResolution resolveNamedType(NamedTypeSyntax namedType, AstNode owner, SemanticScope scope, String unresolvedLabel) {

        var typeName = namedType.getName();
        if (isVisibleGenericParameter(scope, typeName))
            return resolved(new GenericParameterSymbol(typeName));
        return resolveClassName(typeName, owner, scope, unresolvedLabel);
    }

    /// Checks if a generic parameter with the given name is visible in the current semantic scope.
    /// @param scope The semantic scope to check.
    /// @param name The generic parameter name to check for visibility.
    /// @return `true` if the generic parameter is visible, `false` otherwise.
    private boolean isVisibleGenericParameter(SemanticScope scope, String name) {

        var current = scope;
        while (current != null) {

            if (current.getOwner() instanceof ClassDeclarationStatement classDeclaration &&
                genericParameterName(classDeclaration).equals(name))
                return true;
            current = current.getParent();
        }
        return false;
    }

    /// Retrieves the name of the generic parameter declared in a class, if any.
    /// @param classDeclaration The class declaration to inspect.
    /// @return The generic parameter name, or an empty string if none is declared.
    private String genericParameterName(ClassDeclarationStatement classDeclaration) {

        var genericParameter = classDeclaration.getGenericClassParameter();
        if (genericParameter == null) return "";
        if (genericParameter.getSyntax() != null) return genericParameter.getSyntax().getName();

        var tokenClass = genericParameter.getTokenClass();
        if (tokenClass instanceof GenericParameterType(String typeName)) return typeName;
        return tokenClass != null ? tokenClass.token() : "";
    }

    /// Finds a visible class declaration with the given name in the current semantic scope or its ancestors.
    /// @param scope The semantic scope to search.
    /// @param name The class name to look for.
    /// @return The visible class declaration, or `null` if not found.
    private SemanticDeclaration visibleClass(SemanticScope scope, String name) {

        var current = scope;
        while (current != null) {

            for (var declaration : current.findLocal(name))
                if (declaration.kind() == DeclarationKind.CLASS) return declaration;
            current = current.getParent();
        }
        return null;
    }

    /// Creates a TypeResolution for a successfully resolved type symbol with no diagnostics.
    /// @param symbol The resolved type symbol.
    /// @return A TypeResolution containing the symbol and an empty diagnostics list.
    private TypeResolution resolved(TypeSymbol symbol) { return new TypeResolution(symbol, List.of()); }
}
