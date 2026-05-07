package backend.ir;

/// Minimal backend IR instruction.
public record IRInstruction(String opcode, String[] operands) {
    @Override
    public String toString() { return opcode + (operands.length == 0 ? "" : " " + String.join(", ", operands)); }
}
