/**
 * Recursive-descent grammar parsers for Nova source syntax.
 *
 * <p>This package contains focused parser components for declarations, classes,
 * expressions, and other grammar areas. Each parser consumes tokens through the shared
 * parser support layer and builds AST nodes that preserve the source-level structure of the
 * program.</p>
 *
 * <p>Grammar parsers should keep token movement local and explicit. When invalid syntax is
 * encountered, they should report structured diagnostics and recover at well-defined
 * boundaries instead of performing semantic validation.</p>
 */
package parser.grammar;
