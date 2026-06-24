/// # Parser grammar
///
/// Recursive-descent grammar parsers for Nova source syntax.
///
/// This package contains focused parser components for major grammar areas such as
/// declarations, classes, and expressions. Each component consumes tokens through the shared
/// parser support layer and builds AST nodes that preserve source-level structure.
///
/// ## Responsibilities
///
/// - Implement grammar rules in small, auditable methods.
/// - Keep parser cursor movement local and explicit.
/// - Build AST nodes without performing semantic validation.
/// - Report structured parser diagnostics when required syntax is missing.
/// - Recover at well-defined boundaries such as statements, blocks, or class members.
///
/// ## Recovery rule
///
/// Invalid syntax should not unnecessarily discard later valid syntax. When a recovery path is
/// added or changed, it should usually be covered by a regression test.
package parser.grammar;
