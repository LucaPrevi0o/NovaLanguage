package lexer.token;

import lexer.token.family.PrimitiveType;
import java.util.ArrayList;
import java.util.List;

/// Temporary per-parse-session adapter for parser-side ReturnType metadata.
///
/// Each {@link parser.Parser} creates its own instance, eliminating static shared state
/// and making concurrent parsing and testing safe.
///
/// This registry is not a semantic type table and should not be used for validation.
/// Declaration and class parsing still use it to preserve class/generic ReturnType metadata
/// until class/generic metadata is represented semantically.
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

}
