/**
 * Semantic declaration models and collection utilities.
 *
 * <p>This package records named Nova program entities after parsing, independently from
 * parser helper state. Declarations describe what a name introduces, where it is owned, and
 * which source node produced it.</p>
 *
 * <p>Declaration collection is one of the first semantic steps after parsing. Later passes
 * use these declarations to build scopes, resolve names, validate duplicates, and prepare
 * for type checking.</p>
 */
package semantic.declaration;
