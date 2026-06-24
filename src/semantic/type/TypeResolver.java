package semantic.type;

import error.diagnostic.Diagnostic;
import error.diagnostic.DiagnosticPhase;
import lexer.token.ReturnType;
import lexer.token.family.GenericParameterType;
import lexer.token.family.NonPrimitiveType;
import lexer.token.family.PrimitiveType;
import parser.ast.AstNode;
import parser.ast.nodes.type.ArrayTypeSyntax;
import parser.ast.nodes.type.GenericTypeSyntax;
import parser.ast.nodes.type.NamedTypeSyntax;
import parser.ast.nodes.type.TypeSyntax;
import semantic.declaration.DeclarationKind;
import semantic.declaration.SemanticDeclaration;
import semantic.scope.SemanticScope;

import java.util.List;

/// Resolves parsed type syntax and temporary ReturnType adapters into semantic type symbols.
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
            case NamedTypeSyntax namedType when namedType.isPrimitive() -> resolved(new PrimitiveTypeSymbol(namedType.getName()));
            case NamedTypeSyntax namedType -> resolveClassName(namedType.getName(), owner, scope, unresolvedLabel);
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

    private TypeResolution resolveReturnTypeBase(ReturnType type, AstNode owner, SemanticScope scope, String unresolvedLabel) {

        var tokenClass = type.getTokenClass();
        if (tokenClass instanceof PrimitiveType primitiveType) return resolved(new PrimitiveTypeSymbol(primitiveType.token()));
        if (tokenClass instanceof GenericParameterType genericParameter) return resolved(new GenericParameterSymbol(genericParameter.typeName()));
        if (tokenClass instanceof NonPrimitiveType(String typeName)) return resolveClassName(typeName, owner, scope, unresolvedLabel);

        if (type.getSyntax() != null) return resolve(type.getSyntax(), owner, scope, unresolvedLabel);
        return resolved(new UnknownTypeSymbol("<unknown>"));
    }

    private SemanticDeclaration visibleClass(SemanticScope scope, String name) {

        var current = scope;
        while (current != null) {

            for (var declaration : current.findLocal(name))
                if (declaration.getKind() == DeclarationKind.CLASS) return declaration;
            current = current.getParent();
        }
        return null;
    }

    private TypeResolution resolved(TypeSymbol symbol) { return new TypeResolution(symbol, List.of()); }
}
