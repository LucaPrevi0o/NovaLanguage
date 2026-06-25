package semantic.type;

import error.diagnostic.Diagnostic;
import semantic.type.symbol.TypeSymbol;

import java.util.List;

/// Result of resolving type syntax or a temporary ReturnType adapter.
/// @param type The resolved type symbol, or an unknown placeholder when resolution failed.
/// @param diagnostics Diagnostics produced during type resolution.
public record TypeResolution(TypeSymbol type, List<Diagnostic> diagnostics) {

    /// Creates an immutable type-resolution result.
    public TypeResolution { diagnostics = diagnostics != null ? List.copyOf(diagnostics) : List.of(); }
}
