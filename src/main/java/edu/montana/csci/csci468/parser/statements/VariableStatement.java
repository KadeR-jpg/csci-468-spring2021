package edu.montana.csci.csci468.parser.statements;

import org.objectweb.asm.Opcodes;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.ErrorType;
import edu.montana.csci.csci468.parser.ParseError;
import edu.montana.csci.csci468.parser.SymbolTable;
import edu.montana.csci.csci468.parser.expressions.Expression;

import static edu.montana.csci.csci468.bytecode.ByteCodeGenerator.internalNameFor;

public class VariableStatement extends Statement {
    private Expression expression;
    private String variableName;
    private CatscriptType explicitType;
    private CatscriptType type;

    public Expression getExpression() {
        return expression;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public void setExpression(Expression parseExpression) {
        this.expression = addChild(parseExpression);
    }

    public void setExplicitType(CatscriptType type) {
        this.explicitType = type;
    }

    public CatscriptType getExplicitType() {
        return explicitType;
    }

    public boolean isGlobal() {
        return getParent() instanceof CatScriptProgram;
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        expression.validate(symbolTable);
        if (symbolTable.hasSymbol(variableName)) {
            addError(ErrorType.DUPLICATE_NAME);
        } else {
            if (type == null) {
                type = expression.getType();
                symbolTable.registerSymbol(variableName, expression.getType());
            } else if (explicitType.equals(type)) {
                symbolTable.registerSymbol(variableName, type);
            }
            if (explicitType != null) {
                if (!explicitType.equals(CatscriptType.OBJECT)) {
                    if (!explicitType.equals(type)) {
                        addError(ErrorType.INCOMPATIBLE_TYPES);
                    }
                }
            }
        }
    }

    public CatscriptType getType() {
        return type;
    }

    // ==============================================================
    // Implementation
    // ==============================================================
    @Override
    public void execute(CatscriptRuntime runtime) {
        runtime.setValue(variableName, expression.evaluate(runtime));
    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        if (isGlobal()) {
            code.addVarInstruction(Opcodes.ALOAD, 0);
            getExpression().compile(code);
            String desc = "I";
            if (!getType().equals(CatscriptType.INT)) {
                desc = "L" + internalNameFor(getType().getJavaType()) + ";";
            }
            code.addField(getVariableName(), desc);
            code.addFieldInstruction(Opcodes.PUTFIELD, getVariableName(), desc, code.getProgramInternalName());
        } else {
            Integer localStorageSlot = code.createLocalStorageSlotFor(getVariableName());
            getExpression().compile(code);
            if (getExpression().getType().equals(CatscriptType.INT)) {
                code.addVarInstruction(Opcodes.ISTORE, localStorageSlot);
            } else {
                code.addVarInstruction(Opcodes.ASTORE, localStorageSlot);

            }
        }
    }
}
