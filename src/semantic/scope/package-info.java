/**
 * Semantic scope representation and construction for Nova programs.
 *
 * <p>This package models lexical visibility after parsing. Semantic scopes organize
 * declarations into global, class, function, constructor, block, loop, switch, and branch
 * contexts so analysis passes can resolve names according to source structure.</p>
 *
 * <p>The semantic scope tree is the long-term source of truth for name visibility. Parser
 * code should avoid rebuilding semantic meaning once declarations and scopes are available
 * here.</p>
 */
package semantic.scope;
