package edu.montana.csci.csci468.parser.expressions;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.ErrorType;
import edu.montana.csci.csci468.parser.ParseError;
import edu.montana.csci.csci468.parser.SymbolTable;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenType;
import org.objectweb.asm.Opcodes;

public class AdditiveExpression extends Expression {

    private final Token operator;
    private final Expression leftHandSide;
    private final Expression rightHandSide;

    public AdditiveExpression(Token operator, Expression leftHandSide, Expression rightHandSide) {
        this.leftHandSide = addChild(leftHandSide);
        this.rightHandSide = addChild(rightHandSide);
        this.operator = operator;
    }

    public Expression getLeftHandSide() {
        return leftHandSide;
    }

    public Expression getRightHandSide() {
        return rightHandSide;
    }

    public boolean isAdd() {
        return operator.getType() == TokenType.PLUS;
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        leftHandSide.validate(symbolTable);
        rightHandSide.validate(symbolTable);
        if (getType().equals(CatscriptType.INT)) {
            if (!leftHandSide.getType().equals(CatscriptType.INT)) {
                leftHandSide.addError(ErrorType.INCOMPATIBLE_TYPES);
            }
            if (!rightHandSide.getType().equals(CatscriptType.INT)) {
                rightHandSide.addError(ErrorType.INCOMPATIBLE_TYPES);
            }
        }
    }

    @Override
    public CatscriptType getType() {
        if (leftHandSide.getType().equals(CatscriptType.STRING)
                || rightHandSide.getType().equals(CatscriptType.STRING)) {
            return CatscriptType.STRING;
        } else {
            return CatscriptType.INT;
        }
    }

    @Override
    public String toString() {
        return super.toString() + "[" + operator.getStringValue() + "]";
    }

    // ==============================================================
    // Implementation
    // ==============================================================

    @Override
    public Object evaluate(CatscriptRuntime runtime) {
        Object inputEval;
        String lhsStr, rhsStr;
        lhsStr = null;
        rhsStr = null;
        Integer lhsValue, rhsValue;


        if (getLeftHandSide().getType().equals(CatscriptType.STRING)
                || getRightHandSide().getType().equals(CatscriptType.STRING)) {
            if (!getLeftHandSide().getType().equals(CatscriptType.NULL)) {
                lhsStr = getLeftHandSide().evaluate(runtime).toString();
            } else {
                lhsStr = "null";
            }
            if (!getRightHandSide().getType().equals(CatscriptType.NULL)) {
                rhsStr = getRightHandSide().evaluate(runtime).toString();
            } else {
                rhsStr = "null";
            } if (isAdd()) {
                return lhsStr + rhsStr;
            }
        } else {
            lhsValue = (int) leftHandSide.evaluate(runtime);
            rhsValue = (int) rightHandSide.evaluate(runtime);

            if (isAdd()) {
                return lhsValue + rhsValue;
            } else {
                return lhsValue - rhsValue;
            }
        }
        return null;
    }

    @Override
    public void transpile(StringBuilder javascript) {
        getLeftHandSide().transpile(javascript);
        javascript.append(operator.getStringValue());
        getRightHandSide().transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        String strThing = "java/lang/String";
        if(!getRightHandSide().getType().equals(CatscriptType.STRING) && !getLeftHandSide().getType().equals(CatscriptType.STRING)) {
            getLeftHandSide().compile(code);
            getRightHandSide().compile(code);
            if(isAdd()) {
                code.addInstruction(Opcodes.IADD);
            } else {
                code.addInstruction(Opcodes.ISUB);
            }
        } else {
            if(getLeftHandSide().getType().equals(CatscriptType.STRING) && getRightHandSide().getType().equals(CatscriptType.STRING)) {
                getLeftHandSide().compile(code);
                getRightHandSide().compile(code);
            } else if (getRightHandSide().getType().equals(CatscriptType.STRING)) {
                getLeftHandSide().compile(code);
                code.addMethodInstruction(Opcodes.INVOKESTATIC, strThing, "valueOf", argType(getLeftHandSide().getType()));
                getRightHandSide().compile(code);
            } else if (getLeftHandSide().getType().equals(CatscriptType.STRING)) {
                getLeftHandSide().compile(code);
                getRightHandSide().compile(code);
                code.addMethodInstruction(Opcodes.INVOKESTATIC, strThing, "valueOf", argType(getRightHandSide().getType()));
            }
            code.addMethodInstruction(Opcodes.INVOKEVIRTUAL, strThing, "concat", "(Ljava/lang/String;)Ljava/lang/String;");
        }
    }

    private String argType(CatscriptType type) {
        if(type.equals(CatscriptType.INT)) {
            return "(I)Ljava/lang/String;";
        } else if (type.equals(CatscriptType.BOOLEAN)) {
            return "(Z)Ljava/lang/String;";
        } else {
            return "(Ljava/lang/Object;)Ljava/lang/String;";
        }

    }

}
