package edu.montana.csci.csci468.eval;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import edu.montana.csci.csci468.parser.statements.FunctionDefinitionStatement;

public class CatscriptRuntime {
    LinkedList<Map<String, Object>> scopes = new LinkedList<>();

    public void setFunc(String name, FunctionDefinitionStatement func) {
        if (scopes.peek() != null) {
            scopes.peek().put(name, func);
        }
    }

    public FunctionDefinitionStatement getFunc(String name) {
        Object obj = getValue(name);
        if (obj instanceof FunctionDefinitionStatement) {
            return (FunctionDefinitionStatement) obj;
        } else {
            return null;
        }
    }

    public CatscriptRuntime() {
        HashMap<String, Object> globalScope = new HashMap<>();
        scopes.push(globalScope);
    }

    public Object getValue(String name) {
        Iterator<Map<String, Object>> mapIterator = scopes.descendingIterator();
        while (mapIterator.hasNext()) {
            Map<String, Object> scope = mapIterator.next();
            Object getScope = scope.get(name);
            if (getScope != null) {
                return getScope;
            }
        }
        return null;
    }

    public void setValue(String variableName, Object val) {
        scopes.peek().put(variableName, val);
    }

    public void pushScope() {
        scopes.push(new HashMap<>());
    }

    public void popScope() {
        scopes.pop();
    }

}
