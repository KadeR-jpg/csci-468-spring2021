package edu.montana.csci.csci468.parser.expressions;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.ErrorType;
import edu.montana.csci.csci468.parser.ParseError;
import edu.montana.csci.csci468.parser.SymbolTable;
import edu.montana.csci.csci468.parser.statements.CatScriptProgram;
import edu.montana.csci.csci468.parser.statements.FunctionDefinitionStatement;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.Opcodes;

import static edu.montana.csci.csci468.bytecode.ByteCodeGenerator.internalNameFor;

public class FunctionCallExpression extends Expression {
    private final String name;
    List<Expression> arguments;
    private CatscriptType type;

    public FunctionCallExpression(String functionName, List<Expression> arguments) {
        this.arguments = new LinkedList<>();
        for (Expression value : arguments) {
            this.arguments.add(addChild(value));
        }
        this.name = functionName;
    }

    public List<Expression> getArguments() {
        return arguments;
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
        FunctionDefinitionStatement function = symbolTable.getFunction(getName());
        if (function == null) {
            addError(ErrorType.UNKNOWN_NAME);
            type = CatscriptType.OBJECT;
        } else {
            type = function.getType();
            if (arguments.size() != function.getParameterCount()) {
                addError(ErrorType.ARG_MISMATCH);
            } else {
                for (int i = 0; i < arguments.size(); i++) {
                    Expression argument = arguments.get(i);
                    argument.validate(symbolTable);
                    CatscriptType parameterType = function.getParameterType(i);
                    if (!parameterType.isAssignableFrom(argument.getType())) {
                        argument.addError(ErrorType.INCOMPATIBLE_TYPES);
                    }
                }
            }
        }
    }

    public String getDescriptor() {
        StringBuilder sb = new StringBuilder("(");
        for (Expression expr : getArguments()) {
            CatscriptType argType = expr.getType();
            if (argType.equals(CatscriptType.BOOLEAN) || argType.equals(CatscriptType.INT)) {
                sb.append("I");
            } else {
                sb.append("L").append(internalNameFor(getType().getJavaType())).append(";");
            }
        }
        sb.append(")");
        if (type.equals(CatscriptType.VOID)) {
            sb.append("V");
        } else if (type.equals(CatscriptType.BOOLEAN) || type.equals(CatscriptType.INT)) {
            sb.append("I");
        } else {
            sb.append("L").append(internalNameFor(getType().getJavaType())).append(";");
        }
        return sb.toString();
    }

    // ==============================================================
    // Implementation
    // ==============================================================

    @Override
    public Object evaluate(CatscriptRuntime runtime) {
        FunctionDefinitionStatement funcDef = (FunctionDefinitionStatement) runtime.getValue(getName());
        List<Object> eval = new LinkedList<>();
        for (Expression exp : getArguments()) {
            eval.add(exp.evaluate(runtime));
        }
        return funcDef.invoke(runtime, eval);
    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        for (Expression arg : getArguments()) {
            arg.compile(code);
        }
        code.addMethodInstruction(Opcodes.INVOKEVIRTUAL, code.getProgramInternalName(), getName(), getDescriptor());
    }

}
