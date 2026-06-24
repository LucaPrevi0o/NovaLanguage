/// # Semantic declarations
///
/// Semantic declaration models and collection utilities.
///
/// This package records named Nova program entities after parsing. It is the semantic layer's
/// declaration inventory, independent from parser helper state.
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
/// checks are separate semantic responsibilities.
package semantic.declaration;
