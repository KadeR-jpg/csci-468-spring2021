package edu.montana.csci.csci468.parser.expressions;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.ErrorType;
import edu.montana.csci.csci468.parser.ParseError;
import edu.montana.csci.csci468.parser.SymbolTable;
import org.objectweb.asm.Opcodes;

import static edu.montana.csci.csci468.bytecode.ByteCodeGenerator.internalNameFor;

public class IdentifierExpression extends Expression {
    private final String name;
    private CatscriptType type;

    public IdentifierExpression(String value) {
        this.name = value;
    }

    public String getName() {
        return name;
    }

    @Override
    public CatscriptType getType() {
        return type;
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        CatscriptType type = symbolTable.getSymbolType(getName());
        if (type == null) {
            addError(ErrorType.UNKNOWN_NAME);
        } else {
            this.type = type;
        }
    }

    // ==============================================================
    // Implementation
    // ==============================================================

    @Override
    public Object evaluate(CatscriptRuntime runtime) {
        return (Object) runtime.getValue(name);
    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        Integer localvar = code.resolveLocalStorageSlotFor(getName());
        if (localvar == null) {
            code.addVarInstruction(Opcodes.ALOAD, 0);
            String desc = "I";
            if (!getType().equals(CatscriptType.INT)) {
                desc = "L" + internalNameFor(getType().getJavaType()) + ";";
            }
            code.addFieldInstruction(Opcodes.GETFIELD, getName(), desc, code.getProgramInternalName());

        } else {
            if (getType().equals(CatscriptType.INT)) {
                code.addVarInstruction(Opcodes.ILOAD, localvar);
            } else {
                code.addVarInstruction(Opcodes.ALOAD, localvar);
            }
        }
    }
}
