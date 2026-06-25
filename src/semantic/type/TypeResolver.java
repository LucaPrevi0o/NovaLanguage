package semantic.type;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticPhase;
import lexer.token.ReturnType;
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
/// Parsed TypeSyntax is the preferred input. ReturnType adapters are accepted as compatibility
/// fallbacks, and syntaxless adapter metadata is converted through `ReturnTypeSyntaxBridge`.
public final class TypeResolver {

    /// Resolves a ReturnType adapter as a normal declared type.
    /// @param type The ReturnType adapter to resolve.
    /// @param owner The AST node that owns the type.
    /// @param scope The semantic scope used for class lookup.
    /// @return The resolved semantic type and any diagnostics.
    public TypeResolution resolve(ReturnType type, AstNode owner, SemanticScope scope) {

        return resolve(type, owner, scope, "type");
    }

    /// Resolves source type syntax with a ReturnType adapter as a compatibility fallback.
    /// @param syntax The parsed type syntax, or {@code null} when unavailable.
    /// @param fallbackType The ReturnType adapter to use only when syntax is unavailable.
    /// @param owner The AST node that owns the type.
    /// @param scope The semantic scope used for class lookup.
    /// @return The resolved semantic type and any diagnostics.
    public TypeResolution resolve(TypeSyntax syntax, ReturnType fallbackType, AstNode owner, SemanticScope scope) {

        return resolve(syntax, fallbackType, owner, scope, "type");
    }

    /// Resolves source type syntax with a ReturnType adapter as a compatibility fallback.
    /// @param syntax The parsed type syntax, or {@code null} when unavailable.
    /// @param fallbackType The ReturnType adapter to use only when syntax is unavailable.
    /// @param owner The AST node that owns the type.
    /// @param scope The semantic scope used for class lookup.
    /// @param unresolvedLabel The diagnostic label to use when a name cannot be resolved.
    /// @return The resolved semantic type and any diagnostics.
    public TypeResolution resolve(TypeSyntax syntax, ReturnType fallbackType, AstNode owner, SemanticScope scope, String unresolvedLabel) {

        return syntax != null
                ? resolve(syntax, owner, scope, unresolvedLabel)
                : resolve(fallbackType, owner, scope, unresolvedLabel);
    }

    /// Resolves a ReturnType adapter with a custom unresolved-name label.
    /// @param type The ReturnType adapter to resolve.
    /// @param owner The AST node that owns the type.
    /// @param scope The semantic scope used for class lookup.
    /// @param unresolvedLabel The diagnostic label to use when a name cannot be resolved.
    /// @return The resolved semantic type and any diagnostics.
    public TypeResolution resolve(ReturnType type, AstNode owner, SemanticScope scope, String unresolvedLabel) {

        if (type == null) return new TypeResolution(null, List.of());
        var syntax = ReturnTypeSyntaxBridge.toTypeSyntax(type);
        return syntax != null ? resolve(syntax, owner, scope, unresolvedLabel) : resolved(new UnknownTypeSymbol("<unknown>"));
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
        var genericParameterSyntax = classDeclaration.getGenericClassParameterSyntax();
        if (genericParameterSyntax != null) return genericParameterSyntax.getName();
        return ReturnTypeSyntaxBridge.genericParameterName(genericParameter);
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
