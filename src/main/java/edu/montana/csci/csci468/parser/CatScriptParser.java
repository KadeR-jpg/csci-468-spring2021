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

    /**
     * @param source
     * @return CatScriptProgram
     */
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

    /**
     * @param source
     * @return CatScriptProgram
     */
    public CatScriptProgram parseAsExpression(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = parseExpression();
        program.setExpression(expression);
        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    /**
     * @return Statement
     */
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

    /**
     * @return Statement
     */
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

    /**
     * @return Statement
     */
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

    /**
     * @return Statement
     */
    private Statement parseVarStatement() {
        return null;
    }

    /**
     * @return Statement
     */
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

    /**
     * @return List<Statement>
     */
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

    /**
     * @return Statement
     */
    private Statement parseIfStatement() {
        return null;
    }

    /**
     * @param identifier
     * @return Statement
     */
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

    /**
     * @return Statement
     */
    private Statement parseVariablStatement() {
        return null;
    }

    /**
     * @return Statement
     */
    private Statement parseForStatement() {
        ForStatement forStmt = new ForStatement();
        ArrayList<Statement> stmt = new ArrayList<>();
        forStmt.setStart(tokens.consumeToken());
        require(LEFT_PAREN, forStmt);
        // Token ideToken = new tokens.consumeToken();
        // forStmt.setVariableName(ideToken.getStringValue());
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

    /**
     * @return Statement
     */
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
    /**
     * parseExpression() is our "top level" Expression that will be called. This is
     * the start of our recursive descent.
     * 
     * @return parseEqualityExpression()
     */
    private Expression parseExpression() {
        return parseEqualityExpression();
    }

    /**
     * parseEqualityExpression() is the next in line for recursive descent. Here we
     * set the LHS of the expression to a comparison expression and this LHS "falls
     * down" the class list until it hits a token that matches and then it gets
     * returned to this class and the expression solving continues in a similar
     * fashion After the program finds a token match we know that the LHS of the
     * expression has been "solved" so then we consume the equality token and the
     * RHS of the equation does the same descent down Once that side has returned we
     * can then send both sides to the EqualityExpression class to be evaluated then
     * we make sure that we are capturing the whole expression and setting that
     * equal to the LHS of the expression incase there is more expression to
     * evaluate and return that to either end the parsing or to start it all over
     * again.
     * 
     * @return EqualityExpression
     */
    private Expression parseEqualityExpression() {
        Expression equalityLhs = parseComparisonExpression();
        if (tokens.match(EQUAL_EQUAL, BANG_EQUAL)) {
            Token equalityTOKEN = tokens.consumeToken();
            Expression equalityRhs = parseComparisonExpression();
            EqualityExpression equalityExp = new EqualityExpression(equalityTOKEN, equalityLhs, equalityRhs);
            equalityExp.setStart(equalityLhs.getStart());
            equalityExp.setEnd(equalityRhs.getEnd());
            equalityLhs = equalityExp;
        }
        return equalityLhs;
    }

    /**
     * parseComparisonExpression() behaves the same as parseEqualityExpression()
     * just returns a different type of expression.
     * 
     * @return ComparisonExpression()
     */
    private Expression parseComparisonExpression() {
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

    /**
     * parseAdditiveExpression() also behaves pretty similar to the other two above
     * the only difference here is that we can have a while() loop for readability.
     * 
     * @return AdditiveExpression()
     */
    private Expression parseAdditiveExpression() {
        Expression expression = parseFactorExpression();
        while (tokens.match(PLUS, MINUS)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseFactorExpression();
            AdditiveExpression additiveExpression = new AdditiveExpression(operator, expression, rightHandSide);
            additiveExpression.setStart(expression.getStart());
            additiveExpression.setEnd(rightHandSide.getEnd());
            expression = additiveExpression;
        }
        return expression;
    }

    /**
     * parseFactorExpression() works the same as the class above just returns a
     * different type of expression
     * 
     * @return FactorExpression()
     */
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

    /**
     * parseUnaryExpression() is our base level expression the smallest kind we deal
     * with. here we have no LHS side to set it equal to because if it reaches this
     * portion of the code then the only operations that it can have are are a "-"
     * that would return us a negative number and the "!" bool which is just our
     * bool operator. But besides that the class is pretty similar to the rest.
     * Shout out recursive descent. If it is not a MINUS or a NOT then it gets
     * returned to the parsePrimaryExpression() which handles the several different
     * variations of a primary expression.
     * 
     * @return unaryExpression()
     * @return parsePrimaryExpression()
     */
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

    /**parsePrimaryExpression() is where we handle types
     * Here we can return the token type.
     * So when we encounter the literal expressions 
     * such as 1+1+1. That expression would be evaluated to \
     * an IntegerLiteral expression. So this class handles all 
     * similar occurences.
     * @return Expression
     */
    private Expression parsePrimaryExpression() {
        if (tokens.match(INTEGER)) {
            Token integerTOKEN = tokens.consumeToken();
            IntegerLiteralExpression integerExpression = new IntegerLiteralExpression(integerTOKEN.getStringValue());
            integerExpression.setToken(integerTOKEN);
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
        } else if (tokens.match(TRUE) || tokens.match(FALSE)) {
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
        } else if (tokens.match(LEFT_PAREN)) {
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

    /**
     * @param token
     * @return Expression
     */
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

    /**
     * 
     */
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

    /**
     * @return TypeLiteral
     */
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

    /**
     * @param type
     * @param elt
     * @return Token
     */
    // ============================================================
    // Parse Helpers
    // ============================================================
    private Token require(TokenType type, ParseElement elt) {
        return require(type, elt, ErrorType.UNEXPECTED_TOKEN);
    }

    /**
     * @param type
     * @param elt
     * @param msg
     * @return Token
     */
    private Token require(TokenType type, ParseElement elt, ErrorType msg) {
        if (tokens.match(type)) {
            return tokens.consumeToken();
        } else {
            elt.addError(msg, tokens.getCurrentToken());
            return tokens.getCurrentToken();
        }
    }

}
