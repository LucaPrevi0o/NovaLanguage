import src.lexer.Lexer;
import src.lexer.Token;
import src.parser.Parser;
import src.parser.ast.nodes.StatementNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        String sourceCode;
        if (args.length > 0) {

            String filePath = args[0];
            try {

                sourceCode = readFile(filePath);
                System.out.println("=== Reading from file: " + filePath + " ===\n");
            } catch (IOException e) {

                System.err.println("Error reading file: " + e.getMessage());
                return;
            }
        } else {

            sourceCode = """
                    int x = 10;
                    if (x > 5) {
                        for (int i = 0; i < x; i = i + 1) {
                            x = x + i;
                        }
                        return x * 2;
                    } else {
                        return 0;
                    }
                    """;
            System.out.println("=== Using default example code ===\n");
        }

        Lexer lexer = new Lexer(sourceCode);
        List<Token> tokens = lexer.tokenize();

        System.out.println("=== TOKENS ===\n");
        for (Token token : tokens) System.out.println(token);

        System.out.println("\n=== PARSING ===\n");
        try {

            Parser parser = new Parser(tokens);
            List<StatementNode> ast = parser.parse();
            System.out.println("Parsing completed successfully!");
            System.out.println("Total AST nodes: " + ast.size());

            System.out.println("\n=== AST STRUCTURE ===\n");
            for (StatementNode node : ast) AstPrinter.printASTNode(node, 0);

            System.out.println("\n=== SYMBOL TABLE ===\n");
            SymbolTablePrinter.printSymbolTableGrouped(parser.getSymbolTable(), ast);
        } catch (Exception e) {

            System.err.println("\n=== PARSING ERROR ===");
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String readFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return Files.readString(path);
    }

}
