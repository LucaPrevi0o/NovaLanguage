/// # Semantic analysis
///
/// Semantic-analysis passes for syntactically valid Nova ASTs.
///
/// This package answers the question the parser deliberately avoids:
///
/// > Does this syntactically valid program make sense according to Nova's language rules?
///
/// ## Responsibilities
///
/// - Resolve names against semantic scopes.
/// - Validate duplicate declarations.
/// - Check initializer and assignment compatibility.
/// - Validate function, method, member, and array-access expressions where currently supported.
/// - Check l-value validity for assignments.
/// - Check return statements against function and method context.
/// - Check `break` and `continue` placement.
///
/// ## Compiler boundary
///
/// Semantic passes should consume AST nodes, semantic declarations, semantic scopes, and
/// resolved type symbols. They should not push meaning back into parser code or require the
/// parser to reject semantically invalid but syntactically valid constructs.
///
/// ## Current evolution
///
/// Type checking now compares semantic type symbols internally. Declarations still expose the
/// transitional `ReturnType` adapter, but the checker resolves those adapters into
/// `semantic.type` symbols before checking assignability, member access, calls, arrays, and
/// condition expressions.
package semantic.analysis;
