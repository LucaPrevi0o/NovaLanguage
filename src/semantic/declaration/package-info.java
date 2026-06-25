/// # Semantic declarations
///
/// Semantic declaration models and collection utilities.
///
/// This package records named Nova program entities after parsing. It is the semantic layer's
/// declaration inventory, independent from parser helper state.
/// Semantic declarations preserve parsed declaration `TypeSyntax` directly while retaining the
/// temporary `ReturnType` adapter for compatibility during the migration.
///
/// ## Declaration examples
///
/// - Classes
/// - Fields
/// - Methods
/// - Constructors
/// - Functions
/// - Parameters
/// - Local variables
/// - For-each variables
///
/// ## Pipeline role
///
/// ```text
/// AST -> semantic declarations -> semantic scopes -> analysis passes
/// ```
///
/// ## Design note
///
/// Declaration collection should preserve enough information for later validation without
/// deciding every language rule immediately. Duplicate validation, type resolution, and access
/// checks are separate semantic responsibilities. New semantic code should prefer the parsed
/// type-syntax field over the compatibility adapter.
package semantic.declaration;
