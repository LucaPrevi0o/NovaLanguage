package lexer.token;

import lexer.token.family.GenericParameterType;
import lexer.token.family.NonPrimitiveType;
import lexer.token.family.PrimitiveType;
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

    /// Registers a new ReturnType in the registry.
    /// @param returnType The ReturnType to register.
    public void registerType(ReturnType returnType) { types.add(returnType); }

    /// Retrieves a ReturnType from the registry based on its string representation.
    /// @param typeName The string representation of the return type (e.g., "int", "MyClass").
    /// @return The matching ReturnType, or {@code null} if not found.
    public ReturnType getReturnType(String typeName) {
        return types.stream().filter(t -> t.getTokenClass().token().equals(typeName)).findFirst().orElse(null);
    }

    /// Determines whether the given name corresponds to a registered custom class.
    /// @param typeName The class name to check.
    /// @return {@code true} if the name is a registered type;
    public boolean isCustomType(String typeName) {

        var returnType = getReturnType(typeName).getTokenClass();
        return returnType instanceof NonPrimitiveType || returnType instanceof GenericParameterType;
    }
}
