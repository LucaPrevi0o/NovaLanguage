/**
 * AST nodes for class-owned Nova declarations.
 *
 * <p>This package contains declaration nodes for class fields, methods, and constructors.
 * These nodes describe members as they appear in source code, including their declared
 * types, parameters, access modifiers, and bodies where applicable.</p>
 *
 * <p>Member lookup, access control, overload resolution, inheritance validation, and method
 * compatibility are semantic responsibilities and should not be encoded as parser-time
 * assumptions in these node classes.</p>
 */
package parser.ast.nodes.statement.declaration.object;
