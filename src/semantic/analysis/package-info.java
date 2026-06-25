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
/// - Validate duplicate declarations, including overload signatures for functions, methods, and constructors.
/// - Check initializer and assignment compatibility, including class subtype compatibility through superclass chains.
/// - Validate function and method calls, direct or inherited member access, and array-access expressions where currently supported.
/// - Select basic function and method overloads by argument count and semantic argument types.
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
/// transitional `ReturnType` adapter, but syntaxless adapter metadata is converted through
/// `semantic.type.ReturnTypeSyntaxBridge` before the checker works with `semantic.type` symbols
/// for assignability, subtype compatibility, inherited member access, calls, arrays, overload
/// selection, and condition expressions.
package semantic.analysis;
