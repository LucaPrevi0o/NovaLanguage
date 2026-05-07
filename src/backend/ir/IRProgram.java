package backend.ir;

import java.util.ArrayList;
import java.util.List;

/// Minimal IR program container.
public class IRProgram {

    private final List<IRInstruction> instructions = new ArrayList<>();

    public void add(IRInstruction instruction) { instructions.add(instruction); }

    public List<IRInstruction> instructions() { return List.copyOf(instructions); }
}
