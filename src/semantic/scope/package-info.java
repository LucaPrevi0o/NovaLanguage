/// # Semantic scopes
///
/// Semantic scope representation and construction for Nova programs.
///
/// This package models lexical visibility after parsing. Semantic scopes organize declarations
/// into a tree that later passes can use for name lookup and contextual validation.
///
/// ## Scope categories
///
/// - Global scope
/// - Class scope
/// - Function scope
/// - Constructor scope
/// - Block scope
/// - Loop scope
/// - Switch scope
/// - Branch scope
///
/// ## Responsibilities
///
/// - Attach declarations to the scope where they are introduced.
/// - Preserve parent/child visibility relationships.
/// - Support semantic name resolution.
/// - Provide the foundation for duplicate validation and contextual checks.
///
/// ## Design direction
///
/// The semantic scope tree should be the long-term source of truth for name visibility. Parser
/// code should avoid rebuilding or validating semantic scope meaning once this model is
/// available.
package semantic.scope;
