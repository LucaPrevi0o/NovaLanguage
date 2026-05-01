package src.token;

import src.parser.ast.nodes.statement.ClassDeclarationStatement;
import src.token.family.NonPrimitiveType;
import src.token.family.PrimitiveType;
import java.util.ArrayList;
import java.util.List;

//// A registry for managing return types in the language, including both primitive and non-primitive types.
public class TypeRegistry {

    private static final List<ReturnType> types = new ArrayList<>();

    static { for (var primitive : PrimitiveType.values()) types.add(new ReturnType(primitive)); }

    /// Registers a new class declaration as a return type in the registry.
    /// @param classDecl The ClassDeclarationStatement representing the class to be registered as a return type.
    public static void registerClass(ClassDeclarationStatement classDecl) { types.add(new ReturnType(new NonPrimitiveType(classDecl))); }

    /// Retrieves a ReturnType from the registry based on its string representation.
    /// @param typeName The string representation of the return type to retrieve (e.g., "int", "MyClass").
    /// @return The ReturnType object corresponding to the specified type name, or null if no matching type is found in the registry.
    public static ReturnType getReturnType(String typeName) { return types.stream().filter(t -> t.getBaseType().get().equals(typeName)).findFirst().orElse(null); }

    /// Retrieves a ReturnType from the registry based on the name of a class declaration.
    /// @param className The name of the class declaration to retrieve the ReturnType for.
    /// @return The ReturnType object corresponding to the specified class name, or null if no matching class declaration is found in the registry.
    public static ReturnType getClassDeclaration(String className) {

        for (var type : types) if (type.getBaseType() instanceof NonPrimitiveType npt)
            if (npt.getClassDeclaration().getName().equals(className)) return type;
        return null;
    }

    /// Checks if a given type name corresponds to a custom class type registered in the registry.
    /// @param typeName The string representation of the type to check (e.g., "MyClass").
    /// @return true if the specified type name corresponds to a custom class type registered in the registry, false otherwise.
    public static boolean isCustomClass(String typeName) { return getClassDeclaration(typeName) != null; }
}
