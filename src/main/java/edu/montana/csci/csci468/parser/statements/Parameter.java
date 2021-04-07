package edu.montana.csci.csci468.parser.statements;

//import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.SymbolTable;
import edu.montana.csci.csci468.parser.expressions.TypeLiteral;

public class Parameter extends Statement {
    private TypeLiteral type;
    private String idName;

    public void setType(TypeLiteral type) {
        this.type = type;
    }

    public void setIdName(String idName) {
        this.idName = idName;
    }

    public TypeLiteral getType() {
        return type;
    }

    public String getIdentifier() {
        return idName;
    }

    @Override
    public void validate(SymbolTable symTab) {
        int i = 0;
    }

}
