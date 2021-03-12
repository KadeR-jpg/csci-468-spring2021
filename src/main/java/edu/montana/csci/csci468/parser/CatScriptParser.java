package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;
//import jdk.incubator.jpackage.main.CommandLine.Tokenizer;

import static edu.montana.csci.csci468.tokenizer.TokenType.*;

import java.util.function.Function;
import java.rmi.server.ExportException;
import java.util.ArrayList;
import java.util.List;

public class CatScriptParser {

    private TokenList tokens;
    private FunctionDefinitionStatement currentFunctionDefinition;

    public CatScriptProgram parse(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();

        // first parse an expression
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = parseExpression();
        if (tokens.hasMoreTokens()) {
            tokens.reset();
            while (tokens.hasMoreTokens()) {
                program.addStatement(parseProgramStatement());
            }
        } else {
            program.setExpression(expression);
        }

        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    public CatScriptProgram parseAsExpression(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = parseExpression();
        program.setExpression(expression);
        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    // ============================================================
    // Statements
    // ============================================================

    private Statement parseProgramStatement() {
        Statement stmt = parseStatement();
        if (stmt != null) {
            return stmt;
        }
        return new SyntaxErrorStatement(tokens.consumeToken());
    }

    private Statement parsePrintStatement() {
        if (tokens.match(PRINT)) {

            PrintStatement printStatement = new PrintStatement();
            printStatement.setStart(tokens.consumeToken());

            require(LEFT_PAREN, printStatement);
            printStatement.setExpression(parseExpression());
            printStatement.setEnd(require(RIGHT_PAREN, printStatement));

            return printStatement;
        } else {
            return null;
        }
    }

    private Statement parseStatement() {
        if (tokens.match(FUNCTION)) {
            return parseFunctionDeclaration();
        } else if (tokens.match(FOR)) {
            return parseForStatement();
        } else if (tokens.match(PRINT)) {
            return parsePrintStatement();
        } else if (tokens.match(VAR)) {
            return parseVarStatement();
        } else if (tokens.match(IF)) {
            return parseIfStatement();
        } else if (tokens.match(IDENTIFIER)) {
            Token token = tokens.consumeToken();
            if (tokens.matchAndConsume(LEFT_PAREN)) {
                FunctionCallExpression exp = (FunctionCallExpression) parseFunctionCall(token);
                return new FunctionCallStatement(exp);
            }
            return parseAssignmentStatement(token);
        } else {
            return new SyntaxErrorStatement(tokens.consumeToken());
        }
    }

    private Statement parseVarStatement() {
        return null;
    }

    private Statement parseFunctionDeclaration() {
        FunctionDefinitionStatement funcDefinition = new FunctionDefinitionStatement();
        funcDefinition.setStart(tokens.consumeToken());
        Token function = tokens.consumeToken();
        funcDefinition.setName(function.getStringValue());
        require(LEFT_PAREN, funcDefinition);
        // Add stuff here, add the statements to the list
        require(RIGHT_PAREN, funcDefinition);
        if (tokens.match(COLON)) {
            funcDefinition.setType(parseTypeExpression());
        } else {
            TypeLiteral voidType = new TypeLiteral();
            funcDefinition.setType(voidType);
        }
        require(LEFT_BRACE, funcDefinition);
        currentFunctionDefinition = funcDefinition;
        funcDefinition.setBody(parseFunctionBody());
        require(RIGHT_BRACE, funcDefinition);
        funcDefinition.setEnd(tokens.lastToken());

        currentFunctionDefinition = funcDefinition;
        return funcDefinition;
    }

    private List<Statement> parseFunctionBody() {
        List<Statement> stmtList = new ArrayList<>();
        while (!tokens.match(RIGHT_BRACE)) {
            if (tokens.match(EOF)) {
                return stmtList;
            } else if (tokens.match(RETURN)) {
                stmtList.add(parseReturnStatement());
                return stmtList;
            }
            stmtList.add(parseStatement());
        }
        return stmtList;
    }

    private Statement parseIfStatement() {
        return null;
    }

    private Statement parseAssignmentStatement(Token identifier) {
        AssignmentStatement assignStmt = new AssignmentStatement();
        Token identity = identifier;
        assignStmt.setStart(identity);
        assignStmt.setVariableName(identity.getStringValue());
        require(EQUAL, assignStmt);
        assignStmt.setExpression(parseExpression());
        assignStmt.setEnd(tokens.lastToken());
        return assignStmt;
    }

    private Statement parseVariablStatement() {
        return null;
    }

    private Statement parseForStatement() {
        ForStatement forStmt = new ForStatement();
        ArrayList<Statement> stmt = new ArrayList<>();
        forStmt.setStart(tokens.consumeToken());
        require(LEFT_PAREN, forStmt);
//        Token ideToken = new tokens.consumeToken();
//        forStmt.setVariableName(ideToken.getStringValue());
        require(IN, forStmt);
        forStmt.setExpression(parseExpression());
        require(RIGHT_PAREN, forStmt);
        require(LEFT_PAREN, forStmt);
        while (!tokens.match(RIGHT_BRACE)) {
            if (tokens.match(RIGHT_BRACE)) {
                forStmt.addError(ErrorType.UNEXPECTED_TOKEN);
                return forStmt;
            }
            stmt.add(parseStatement());
        }
        forStmt.setBody(stmt);
        forStmt.setEnd(require(RIGHT_BRACE, forStmt));
        return forStmt;
    }

    private Statement parseReturnStatement() {
        ReturnStatement retStmt = new ReturnStatement();
        retStmt.setStart(tokens.consumeToken());
        if (!tokens.match(RIGHT_BRACE)) {
            Expression exp = parseExpression();
            retStmt.setExpression(exp);
        }
        retStmt.setEnd(tokens.lastToken());
        retStmt.setFunctionDefinition(currentFunctionDefinition);
        return retStmt;
    }

    // ============================================================
    // Expressions
    // ============================================================

    private Expression parseExpression() {
        return parseEqualityExpression();
    }

    private Expression parseEqualityExpression() {
        Expression equalityLhs = parseComparisionExpression();
        if (tokens.match(EQUAL_EQUAL, BANG_EQUAL)) {
            Token equalityTOKEN = tokens.consumeToken();
            Expression equalityRhs = parseComparisionExpression();
            EqualityExpression equalityExp = new EqualityExpression(equalityTOKEN, equalityLhs, equalityRhs);
            equalityExp.setStart(equalityLhs.getStart());
            equalityExp.setEnd(equalityRhs.getEnd());
            equalityLhs = equalityExp;
        }
        return equalityLhs;
    }

    private Expression parseComparisionExpression() {
        Expression compLhs = parseAdditiveExpression();
        if (tokens.match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token compTOKEN = tokens.consumeToken();
            Expression compRhs = parseAdditiveExpression();
            ComparisonExpression compExp = new ComparisonExpression(compTOKEN, compLhs, compRhs);
            compExp.setStart(compLhs.getStart());
            compExp.setEnd(compRhs.getEnd());
            compLhs = compExp;
        }
        return compLhs;
    }

    private Expression parseAdditiveExpression() {
        Expression expression = parseFactorExpression();
        if (tokens.match(PLUS, MINUS)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseFactorExpression();
            AdditiveExpression additiveExpression = new AdditiveExpression(operator, expression, rightHandSide);
            additiveExpression.setStart(expression.getStart());
            additiveExpression.setEnd(rightHandSide.getEnd());
            expression = additiveExpression;
        }
        return expression;
    }

    private Expression parseFactorExpression() {
        Expression unaryLhs = parseUnaryExpression();
        while (tokens.match(STAR, SLASH)) {
            Token factorTOKEN = tokens.consumeToken();
            Expression unaryRhs = parseUnaryExpression();
            FactorExpression factEXP = new FactorExpression(factorTOKEN, unaryLhs, unaryRhs);
            factEXP.setStart(unaryLhs.getStart());
            factEXP.setEnd(unaryRhs.getEnd());
            unaryLhs = factEXP;
        }
        return unaryLhs;
    }

    private Expression parseUnaryExpression() {
        if (tokens.match(MINUS, NOT)) {
            Token token = tokens.consumeToken();
            final Expression rhs = parseUnaryExpression();
            UnaryExpression unaryExpression = new UnaryExpression(token, rhs);
            unaryExpression.setStart(token);
            unaryExpression.setEnd(rhs.getEnd());
            return unaryExpression;
        } else {
            return parsePrimaryExpression();
        }
    }

    private Expression parsePrimaryExpression() {
        if (tokens.match(INTEGER)) {
            Token integerToken = tokens.consumeToken();
            IntegerLiteralExpression integerExpression = new IntegerLiteralExpression(integerToken.getStringValue());
            integerExpression.setToken(integerToken);
            return integerExpression;
        } else if (tokens.match(STRING)) {
            Token strTOKEN = tokens.consumeToken();
            StringLiteralExpression strExpression = new StringLiteralExpression(strTOKEN.getStringValue());
            strExpression.setToken(strTOKEN);
            return strExpression;
        } else if (tokens.match(NULL)) {
            Token nullTOKEN = tokens.consumeToken();
            NullLiteralExpression nullEXP = new NullLiteralExpression();
            nullEXP.setToken(nullTOKEN);
            return nullEXP;
        } else if (tokens.match(TRUE) || tokens.match(FALSE)) { // Boolean
            Token boolTOKEN = tokens.consumeToken();
            BooleanLiteralExpression boolEXP;
            if (boolTOKEN.getType() == TRUE) {
                boolEXP = new BooleanLiteralExpression(true);
            } else {
                boolEXP = new BooleanLiteralExpression(false);
            }
            boolEXP.setToken(boolTOKEN);
            return boolEXP;
        } else if (tokens.match(LEFT_BRACKET)) {
            return parseListLiteral();
        } else if (tokens.match(LEFT_PAREN)) { // Paren Expressions
            tokens.consumeToken();
            ParenthesizedExpression parenExp = null;
            while (!tokens.match(RIGHT_PAREN)) {
                parenExp = new ParenthesizedExpression(parseExpression());
            }
            tokens.consumeToken();
            return parenExp;
        } else if (tokens.match(IDENTIFIER)) {
            Token token = tokens.consumeToken();
            if (tokens.match(LEFT_PAREN)) {
                tokens.consumeToken();
                return parseFunctionCall(token);
            } else {
                IdentifierExpression iExp = new IdentifierExpression(token.getStringValue());
                iExp.setToken(token);
                return iExp;
            }
        } else {
            return new SyntaxErrorExpression(tokens.consumeToken());
        }
    }

    private Expression parseFunctionCall(Token token) {
        List<Expression> expList = new ArrayList<>();
        while (!tokens.match(RIGHT_PAREN)) {
            if (!tokens.hasMoreTokens()) {
                FunctionCallExpression errList = new FunctionCallExpression(token.getStringValue(), expList);
                errList.addError(ErrorType.UNTERMINATED_ARG_LIST);
                return errList;
            } else if (tokens.match(COMMA)) {
                tokens.consumeToken();
            } else {
                Expression exp = parseExpression();
                expList.add(exp);
            }

        }
        tokens.consumeToken();
        return new FunctionCallExpression(token.getStringValue(), expList);
    }

    private Expression parseListLiteral() {
        tokens.consumeToken();
        List<Expression> expList = new ArrayList<>();
        while (!tokens.match(RIGHT_BRACKET)) {
            if (!tokens.hasMoreTokens()) {
                ListLiteralExpression errList = new ListLiteralExpression(expList);
                errList.addError(ErrorType.UNTERMINATED_LIST);
                return errList;
            } else if (tokens.match(COMMA)) {
                tokens.consumeToken();
            } else {
                Expression exp = parseExpression();
                expList.add(exp);
            }
        }
        tokens.consumeToken();
        return new ListLiteralExpression(expList);
    }

    private TypeLiteral parseTypeExpression() {
        String token = tokens.consumeToken().getStringValue();
        TypeLiteral typeLIT = new TypeLiteral();
        if (token == "int") {
            typeLIT.setType(CatscriptType.INT);
            return typeLIT;
        } else if (token == "bool") {
            typeLIT.setType(CatscriptType.BOOLEAN);
            return typeLIT;
        } else if (token == "string") {
            typeLIT.setType(CatscriptType.STRING);
            return typeLIT;
        } else if (token == "object") {
            typeLIT.setType(CatscriptType.OBJECT);
            return typeLIT;
        } else if (token == "void") {
            typeLIT.setType(CatscriptType.VOID);
            return typeLIT;
        } else if (token == "list") {
            TypeLiteral listTypeLit = null;
            String lhs = tokens.consumeToken().getStringValue();
            if (lhs.equals("<")) {
                listTypeLit = parseTypeExpression();
            }
            if (tokens.consumeToken().getStringValue().equals(">")) {
                assert listTypeLit != null;
                CatscriptType.ListType listTYPE = new CatscriptType.ListType(listTypeLit.getType());
                typeLIT.setType(listTYPE);
                return typeLIT;
            }
        } else {
            typeLIT.setType(CatscriptType.NULL);
            return typeLIT;
        }
        return typeLIT;
    }

    // ============================================================
    // Parse Helpers
    // ============================================================
    private Token require(TokenType type, ParseElement elt) {
        return require(type, elt, ErrorType.UNEXPECTED_TOKEN);
    }

    private Token require(TokenType type, ParseElement elt, ErrorType msg) {
        if (tokens.match(type)) {
            return tokens.consumeToken();
        } else {
            elt.addError(msg, tokens.getCurrentToken());
            return tokens.getCurrentToken();
        }
    }

}
