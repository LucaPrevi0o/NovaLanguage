package token;

import parser.ast.nodes.statement.ClassDeclarationStatement;
import parser.ast.nodes.statement.declaration.object.ClassConstructorDeclaration;
import parser.ast.nodes.statement.declaration.object.ClassFieldDeclaration;
import parser.ast.nodes.statement.declaration.object.ClassMethodDeclaration;
import token.family.AccessModifier;
import token.family.NonPrimitiveType;
import token.family.PrimitiveType;
import java.util.ArrayList;
import java.util.List;

/// A per-parse-session registry of all known types (primitives + user-declared classes).
///
/// Each {@link parser.Parser} creates its own instance, eliminating static shared state
/// and making concurrent parsing and testing safe.
public class TypeRegistry {

    private final List<ReturnType> types = new ArrayList<>();

    /// Creates a new TypeRegistry pre-populated with all primitive types.
    public TypeRegistry() { for (var primitive : PrimitiveType.values()) types.add(new ReturnType(primitive)); }

    /// Registers a new class declaration as a return type in the registry.
    /// @param classDecl The ClassDeclarationStatement representing the class to register.
    public void registerClass(ClassDeclarationStatement classDecl) { types.add(new ReturnType(new NonPrimitiveType(classDecl))); }

    /// Registers a new ReturnType in the registry.
    /// @param returnType The ReturnType to register.
    public void registerType(ReturnType returnType) { types.add(returnType); }

    /// Retrieves a ReturnType from the registry based on its string representation.
    /// @param typeName The string representation of the return type (e.g., "int", "MyClass").
    /// @return The matching ReturnType, or {@code null} if not found.
    public ReturnType getReturnType(String typeName) {
        return types.stream().filter(t -> t.getBaseType().get().equals(typeName)).findFirst().orElse(null);
    }

    /// Retrieves a ReturnType for a registered class by its class name.
    /// @param className The class name.
    /// @return The ReturnType wrapping the class declaration, or {@code null} if not found.
    public ReturnType getClassDeclaration(String className) {

        for (var type : types) if (type.getBaseType() instanceof NonPrimitiveType npt)
            if (npt.getClassDeclaration().getName().equals(className)) return type;
        return null;
    }

    /// Determines whether the given name corresponds to a registered custom class.
    /// @param typeName The class name to check.
    /// @return {@code true} if the name is a registered custom class.
    public boolean isCustomClass(String typeName) { return getClassDeclaration(typeName) != null; }

    /// Determines whether the given name corresponds to a registered primitive type.
    /// @param typeName The type name to check.
    /// @return {@code true} if the name is a registered primitive type.
    public boolean isGenericType(String typeName) {
        return types.stream().filter(t -> t.getBaseType().get().equals(typeName)).anyMatch(ReturnType::isGeneric);
    }

    /// Registers a class name placeholder for multi-file forward references.
    /// If already registered, this is a no-op.
    /// @param className The name of the class to register as a placeholder.
    public void registerClassName(String className) {

        if (isCustomClass(className)) return;
        var placeholder = new ClassDeclarationStatement(
            0, 0, className,
            new ClassMethodDeclaration[0],
            new ClassFieldDeclaration[0],
            new ReturnType[0],
            null,
            new ClassDeclarationStatement[0],
            AccessModifier.PUBLIC,
            new ClassConstructorDeclaration[0]
        );
        registerClass(placeholder);
    }

    /// No-op method kept for test API compatibility.
    /// With per-instance TypeRegistry, each {@link parser.Parser} creates a fresh registry,
    /// so no global reset is needed.
    public static void reset() { }
}
