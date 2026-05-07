package backend;

import backend.lowering.AstLowerer;
import backend.opt.Optimizer;
import backend.target.PseudoAssemblyEmitter;
import parser.ast.nodes.StatementNode;

import java.util.List;

/// End-to-end backend foundation pipeline.
public class BackendPipeline {

    private final AstLowerer lowerer = new AstLowerer();
    private final Optimizer optimizer = new Optimizer();
    private final PseudoAssemblyEmitter emitter = new PseudoAssemblyEmitter();

    public String compileToPseudoAssembly(List<StatementNode> ast) {

        var ir = lowerer.lower(ast);
        var optimized = optimizer.optimize(ir);
        return emitter.emit(optimized);
    }
}
