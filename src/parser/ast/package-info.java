/**
 * Core AST contracts for Nova source representation.
 *
 * <p>This package defines the base abstractions shared by syntax-tree nodes produced by the
 * parser. The AST represents source-level Nova syntax before semantic lowering, type
 * resolution, optimization, or backend-specific transformation.</p>
 *
 * <p>AST classes should preserve what the user wrote closely enough for diagnostics and
 * semantic passes. They are not intended to be the final intermediate representation used by
 * the backend.</p>
 */
package parser.ast;
