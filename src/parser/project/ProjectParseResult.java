package parser.project;

import parser.ast.SymbolTable;
import parser.ast.nodes.StatementNode;
import token.TypeRegistry;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/// Result of a multi-file parse run.
public record ProjectParseResult(Map<Path, List<StatementNode>> astByFile, SymbolTable globalSymbolTable, TypeRegistry typeRegistry) { }
