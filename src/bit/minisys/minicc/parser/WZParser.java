package bit.minisys.minicc.parser;

import bit.minisys.minicc.MiniCCCfg;
import bit.minisys.minicc.internal.util.MiniCCUtil;
import bit.minisys.minicc.parser.ast.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.antlr.v4.gui.TreeViewer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("DuplicatedCode")
public class WZParser implements IMiniCCParser {

    private ArrayList<ScannerToken> tknList;
    private int tokenIndex;
    private ScannerToken nextToken;

    @Override
    public String run(String iFile) throws Exception {
        System.out.println("Parsing...");

        String oFile = MiniCCUtil.removeAllExt(iFile) + MiniCCCfg.MINICC_PARSER_OUTPUT_EXT;
        String tFile = MiniCCUtil.removeAllExt(iFile) + MiniCCCfg.MINICC_SCANNER_OUTPUT_EXT;

        tknList = loadTokens(tFile);
        tokenIndex = 0;

        ASTNode root = program();


        String[] dummyStrs = new String[16];
        TreeViewer viewr = new TreeViewer(Arrays.asList(dummyStrs), root);
        viewr.open();

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(oFile), root);

        return oFile;
    }

    private ArrayList<ScannerToken> loadTokens(String tFile) {
        tknList = new ArrayList<ScannerToken>();

        ArrayList<String> tknStr = MiniCCUtil.readFile(tFile);

        for(String str: tknStr) {
            if(str.trim().length() <= 0) {
                continue;
            }

            ScannerToken st = new ScannerToken();
            //[@0,0:2='int',<'int'>,1:0]
            String[] segs;
            if(str.indexOf("<','>") > 0) {
                str = str.replace("','", "'DOT'");

                segs = str.split(",");
                segs[1] = "=','";
                segs[2] = "<','>";

            }else {
                segs = str.split(",");
            }
            st.lexme = segs[1].substring(segs[1].indexOf("=") + 2, segs[1].length() - 1);
            st.type  = segs[2].substring(segs[2].indexOf("<") + 1, segs[2].length() - 1);
            String[] lc = segs[3].split(":");
            st.line = Integer.parseInt(lc[0]);
            st.column = Integer.parseInt(lc[1].replace("]", ""));

            tknList.add(st);
        }

        return tknList;
    }

    public void matchToken(String type) {
        if(tokenIndex < tknList.size()) {
            ScannerToken next = tknList.get(tokenIndex);
            if(!next.type.equals(type)) {
                System.out.println("[ERROR]Parser: unmatched token, expected = " + type + ", "
                        + "input = " + next.type);
            }
            else {
                tokenIndex++;
            }
        }
    }

    private boolean tokenIsType(String type) {
        return tknList.get(tokenIndex).type.equals(type);
    }

    private ASTToken getNextToken() {
        ScannerToken scannerToken = tknList.get(tokenIndex);
        ASTToken token = new ASTToken(scannerToken.lexme, tokenIndex);
        tokenIndex ++;
        return token;
    }

    //program
    //	external-declaration-list
    private ASTNode program() {
        ASTCompilationUnit p = new ASTCompilationUnit();
        ArrayList<ASTNode> fl = externalDeclarationList();
        if(fl != null) {
            p.items.addAll(fl);
        }
        p.children.addAll(p.items);
        return p;
    }

    //external-declaration-list
    //	function-definition external-declaration-list
    //	ε
    private ArrayList<ASTNode> externalDeclarationList() {
        nextToken = tknList.get(tokenIndex);
        if (nextToken.type.equals("EOF")) {
            return null;
        } else {
            ArrayList<ASTNode> list = new ArrayList<>();
            ASTNode node = functionDefinition();
            list.add(node);
            ArrayList<ASTNode> next = externalDeclarationList();
            if (next != null) {
                list.addAll(next);
            }
            return list;
        }
    }

    //function-definition
    //	specifier declarator ( parameter-type-list ) compound-statement
    private ASTNode functionDefinition() {
        ArrayList<ASTToken> tokens = new ArrayList<>();

        ASTToken mSpecifier = specifier();
        tokens.add(mSpecifier);

        ASTDeclarator astDeclarator = declarator();

        ASTFunctionDeclarator functionDeclarator = new ASTFunctionDeclarator();
        functionDeclarator.declarator = astDeclarator;
        functionDeclarator.children.add(astDeclarator);

        matchToken("'('");
        if (!tokenIsType("')'")) {
            List<ASTParamsDeclarator> paramsDeclarators = parameterTypeList();
            functionDeclarator.params = paramsDeclarators;
            functionDeclarator.children.addAll(paramsDeclarators);
        } else {
            functionDeclarator.params = null;
        }
        matchToken("')'");

        ASTCompoundStatement mCompoundStatement = compoundStatement();

        ASTFunctionDefine functionDefine = new ASTFunctionDefine(tokens, functionDeclarator, mCompoundStatement);
        functionDefine.children.addAll(tokens);
        functionDefine.children.add(functionDeclarator);
        functionDefine.children.add(mCompoundStatement);

        return functionDefine;
    }

    //specifier
    //	void
    //	char
    //	short
    //	int
    //	long
    //	float
    //	double
    //	signed
    //	unsigned
    private ASTToken specifier() {
        ScannerToken scannerToken = tknList.get(tokenIndex);
        if (scannerToken.isSpecifier()) {
            ASTToken token = new ASTToken(scannerToken.lexme, tokenIndex);
            tokenIndex ++;
            return token;
        }
        return new ASTToken();
    }

    //declaration
    //	specifier init-declarator-list ;
    //	specifier declaration
    private ASTDeclaration declaration() {
        ArrayList<ASTToken> mSpecifiers = new ArrayList<>();
        ASTToken mToken = specifier();
        mSpecifiers.add(mToken);
        while (tknList.get(tokenIndex).isSpecifier()) {
            mToken = specifier();
            mSpecifiers.add(mToken);
        }
        List<ASTInitList> mInitList = initDeclaratorList();

        matchToken("';'");

        ASTDeclaration astDeclaration = new ASTDeclaration(mSpecifiers, mInitList);
        astDeclaration.children.addAll(mSpecifiers);
        astDeclaration.children.addAll(mInitList);

        return astDeclaration;
    }

    //init-declarator-list
    //	init-declarator , init-declarator-list
    //	init-declarator
    private List<ASTInitList> initDeclaratorList() {
        ArrayList<ASTInitList> mList = new ArrayList<>();
        ASTInitList mInitList = initDeclarator();
        mList.add(mInitList);
        while (tokenIsType("','")) {
            matchToken("','");
            mInitList = initDeclarator();
            mList.add(mInitList);
        }
        return mList;
    }

    //init-declarator
    //	declarator
    //	declarator = initializer
    private ASTInitList initDeclarator() {
        ASTInitList astInitList = new ASTInitList();
        astInitList.declarator = declarator();
        astInitList.children.add(astInitList.declarator);
        if (tokenIsType("'='")) {
            matchToken("'='");
            List<ASTExpression> mExprs = initializer();
            astInitList.exprs = mExprs;
            astInitList.children.addAll(mExprs);
        }
        return astInitList;
    }

    //initializer
    //	assignment-expression
    //	{ initializer-list }
    //	{ initializer-list , }
    //initializer-list
    //	initializer
    //	initializer , initializer-list
    private List<ASTExpression> initializer() {
        ArrayList<ASTExpression> astExpressions = new ArrayList<>();
        if (tokenIsType("'{'")) {
            matchToken("'{'");
            ASTExpression astExpression = assignmentExpression();
            astExpressions.add(astExpression);
            while (tokenIsType("','")) {
                matchToken("','");
                if (tokenIsType("'}'")) {
                    break;
                }
                astExpression = assignmentExpression();
                astExpressions.add(astExpression);
            }
            matchToken("'}'");
        } else {
            astExpressions.add(assignmentExpression());
        }
        return astExpressions;
    }

    //declarator
    //	identifier
    //	identifier [ ]
    //	identifier [ assignment-expression ]
    private ASTDeclarator declarator() {
        ASTIdentifier astIdentifier = new ASTIdentifier();
        astIdentifier.tokenId = tokenIndex;
        astIdentifier.value = tknList.get(tokenIndex).lexme;
        matchToken("Identifier");

        ASTVariableDeclarator astVariableDeclarator = new ASTVariableDeclarator();
        astVariableDeclarator.identifier = astIdentifier;
        astVariableDeclarator.children.add(astIdentifier);

        if (tokenIsType("'['")) {
            matchToken("'['");
            ASTExpression astExpression = assignmentExpression();
            ASTArrayDeclarator astArrayDeclarator = new ASTArrayDeclarator();
            astArrayDeclarator.declarator = astVariableDeclarator;
            astArrayDeclarator.expr = astExpression;
            astArrayDeclarator.children.add(astVariableDeclarator);
            astArrayDeclarator.children.add(astExpression);
            matchToken("']'");
            return astArrayDeclarator;
        } else {
            return astVariableDeclarator;
        }
    }

    //parameter-type-list
    //	parameter-list
    //	parameter-list , ...
    //parameter-list
    //	parameter-declaration
    //	parameter-declaration , parameter-list
    private ArrayList<ASTParamsDeclarator> parameterTypeList() {
        ArrayList<ASTParamsDeclarator> astParamsDeclaratorList = new ArrayList<>();
        ASTParamsDeclarator astParamsDeclarator = parameterDeclaration();
        astParamsDeclaratorList.add(astParamsDeclarator);
        while (tokenIsType("','")) {
            matchToken("','");
            if (tokenIsType("'...'")) {
                matchToken("'...'");
                break;
            }
            astParamsDeclarator = parameterDeclaration();
            astParamsDeclaratorList.add(astParamsDeclarator);
        }
        return astParamsDeclaratorList;
    }

    //parameter-declaration
    //	specifier
    //	specifier declarator
    //	specifier parameter-declaration
    private ASTParamsDeclarator parameterDeclaration() {
        ArrayList<ASTToken> specifiers = new ArrayList<>();
        specifiers.add(specifier());
        while (tknList.get(tokenIndex).isSpecifier()) {
            specifiers.add(specifier());
        }

        ASTDeclarator astDeclarator = null;
        if (tknList.get(tokenIndex).type.equals("Identifier")) {
            astDeclarator = declarator();
        }

        ASTParamsDeclarator astParamsDeclarator = new ASTParamsDeclarator(specifiers, astDeclarator);
        astParamsDeclarator.children.addAll(specifiers);
        if (astDeclarator != null) {
            astParamsDeclarator.children.add(astDeclarator);
        }

        return astParamsDeclarator;
    }

    //compound-statement
    //	{ statement-list }
    //statement-list
    //	ε
    //	declaration statement-list
    //	statement statement-list
    private ASTCompoundStatement compoundStatement() {
        ASTCompoundStatement astCompoundStatement = new ASTCompoundStatement();
        ArrayList<ASTNode> list = new ArrayList<>();
        matchToken("'{'");
        while (!tokenIsType("'}'")) {
            if (tknList.get(tokenIndex).isSpecifier()) {
                list.add(declaration());
            } else {
                list.add(statement());
            }
        }
        matchToken("'}'");
        astCompoundStatement.blockItems.addAll(list);
        astCompoundStatement.children.addAll(list);
        return astCompoundStatement;
    }

    //statement
    //	expression-statement
    //	compound-statement
    //	selection-statement
    //	iteration-statement
    //	jump-statement
    private ASTStatement statement() {
        if (tokenIsType("'{'")) {
            return compoundStatement();
        } else if (tokenIsType("'if'")) {
            return selectionStatement();
        } else if (tokenIsType("'for'")) {
            return iterationStatement();
        } else if (tokenIsType("'return'")) {
            return jumpStatement();
        } else if (tokenIsType("'break'")) {
            return breakStatement();
        }
        else {
            return expressionStatement();
        }
    }

    //expression-statement
    //	;
    //	expression ;
    private ASTExpressionStatement expressionStatement() {
        ASTExpressionStatement astExpressionStatement = new ASTExpressionStatement();
        if (!tokenIsType("';'")) {
            ArrayList<ASTExpression> list = expression();
            astExpressionStatement.exprs = list;
            astExpressionStatement.children.addAll(list);
        }
        matchToken("';'");
        return astExpressionStatement;
    }

    //expression
    //	assignment-expression
    //	assignment-expression , expression
    private ArrayList<ASTExpression> expression() {
        ArrayList<ASTExpression> list = new ArrayList<>();
        ASTExpression astExpression = assignmentExpression();
        list.add(astExpression);
        while (tokenIsType("','")) {
            matchToken("','");
            astExpression = assignmentExpression();
            list.add(astExpression);
        }
        return list;
    }

    //assignment-expression
    //	conditional-expression
    //	unary-expression assignment-operator assignment-expression
    private ASTExpression assignmentExpression() {
        int numOfParentheses = 0;
        int numOfBrackets = 0;
        for (int i = 0;
             !tknList.get(tokenIndex + i).type.equals("','") &&
                     !tknList.get(tokenIndex + i).type.equals("';'");
             i++) {

            if (tknList.get(tokenIndex + i).type.equals("'('")) {
                numOfParentheses++;
                continue;
            }

            if (tknList.get(tokenIndex + i).type.equals("'['")) {
                numOfBrackets++;
                continue;
            }

            if (tknList.get(tokenIndex + i).type.equals("')'")) {
                numOfParentheses--;
                if (numOfParentheses < 0) {
                    break;
                }
            }

            if (tknList.get(tokenIndex + i).type.equals("']'")) {
                numOfBrackets--;
                if (numOfBrackets < 0) {
                    break;
                }
            }

            if (tknList.get(tokenIndex + i).isAssignmentOperator()) {
                ASTBinaryExpression astBinaryExpression = new ASTBinaryExpression();
                astBinaryExpression.expr1 = unaryExpression();
                astBinaryExpression.op = assignmentOperator();
                astBinaryExpression.expr2 = assignmentExpression();
                astBinaryExpression.children.add(astBinaryExpression.expr1);
                astBinaryExpression.children.add(astBinaryExpression.op);
                astBinaryExpression.children.add(astBinaryExpression.expr2);
                return astBinaryExpression;
            }
        }
        return conditionalExpression();
    }

    //assignment-operator
    //	=
    //	*=
    //	/=
    //	%=
    //	+=
    //	-=
    //	<<=
    //	>>=
    //	&=
    //	^=
    //	|=
    private ASTToken assignmentOperator() {
        ScannerToken scannerToken = tknList.get(tokenIndex);
        if (scannerToken.isAssignmentOperator()) {
            ASTToken token = new ASTToken(scannerToken.lexme, tokenIndex);
            tokenIndex ++;
            return token;
        }
        return new ASTToken();
    }

    //conditional-expression
    //	logical-or-expression
    //	logical-or-expression ? expression : conditional-expression
    private ASTExpression conditionalExpression() {
        ASTExpression expr1 = logicalOrExpression();
        if (tokenIsType("'?'")) {
            ASTConditionExpression astConditionExpression = new ASTConditionExpression();
            matchToken("'?'");
            LinkedList<ASTExpression> expr2 = new LinkedList<>(expression());
            matchToken("':'");
            ASTExpression expr3 = conditionalExpression();
            astConditionExpression.condExpr = expr1;
            astConditionExpression.trueExpr = expr2;
            astConditionExpression.falseExpr = expr3;
            astConditionExpression.children.add(expr1);
            astConditionExpression.children.addAll(expr2);
            astConditionExpression.children.add(expr3);
            return astConditionExpression;
        } else {
            return expr1;
        }
    }

    //logical-or-expression
    //	logical-and-expression
    //	logical-and-expression || logical-or-expression
    private ASTExpression logicalOrExpression() {
        ASTExpression expr1 = null;
        ASTToken op = null;
        ASTExpression expr2 = logicalAndExpression();
        while (tokenIsType("'||'")) {
            if (expr1 != null) {
                ASTBinaryExpression astBinaryExpression = new ASTBinaryExpression(op, expr1, expr2);
                astBinaryExpression.children.add(expr1);
                astBinaryExpression.children.add(expr2);
                astBinaryExpression.children.add(op);
                expr1 = astBinaryExpression;
            } else {
                expr1 = expr2;
            }
            op = getNextToken();
            expr2 = logicalAndExpression();
        }
        if (expr1 == null) {
            return expr2;
        } else {
            ASTBinaryExpression astBinaryExpression = new ASTBinaryExpression(op, expr1, expr2);
            astBinaryExpression.children.add(expr1);
            astBinaryExpression.children.add(expr2);
            astBinaryExpression.children.add(op);
            return astBinaryExpression;
        }
    }

    //logical-and-expression
    //	inclusive-or-expression
    //	inclusive-or-expression && logical-and-expression
    private ASTExpression logicalAndExpression() {
        ASTExpression expr1 = null;
        ASTToken op = null;
        ASTExpression expr2 = inclusiveOrExpression();
        while (tokenIsType("'&&'")) {
            if (expr1 != null) {
                ASTBinaryExpression astBinaryExpression = new ASTBinaryExpression(op, expr1, expr2);
                astBinaryExpression.children.add(expr1);
                astBinaryExpression.children.add(expr2);
                astBinaryExpression.children.add(op);
                expr1 = astBinaryExpression;
            } else {
                expr1 = expr2;
            }
            op = getNextToken();
            expr2 = inclusiveOrExpression();
        }
        if (expr1 == null) {
            return expr2;
        } else {
            ASTBinaryExpression astBinaryExpression = new ASTBinaryExpression(op, expr1, expr2);
            astBinaryExpression.children.add(expr1);
            astBinaryExpression.children.add(expr2);
            astBinaryExpression.children.add(op);
            return astBinaryExpression;
        }
    }

    //inclusive-or-expression
    //	exclusive-or-expression
    //	exclusive-or-expression | inclusive-or-expression
    private ASTExpression inclusiveOrExpression() {
        ASTExpression expr1 = null;
        ASTToken op = null;
        ASTExpression expr2 = exclusiveOrExpression();
        while (tokenIsType("'|'")) {
            if (expr1 != null) {
                ASTBinaryExpression astBinaryExpression = new ASTBinaryExpression(op, expr1, expr2);
                astBinaryExpression.children.add(expr1);
                astBinaryExpression.children.add(expr2);
                astBinaryExpression.children.add(op);
                expr1 = astBinaryExpression;
            } else {
                expr1 = expr2;
            }
            op = getNextToken();
            expr2 = exclusiveOrExpression();
        }
        if (expr1 == null) {
            return expr2;
        } else {
            ASTBinaryExpression astBinaryExpression = new ASTBinaryExpression(op, expr1, expr2);
            astBinaryExpression.children.add(expr1);
            astBinaryExpression.children.add(expr2);
            astBinaryExpression.children.add(op);
            return astBinaryExpression;
        }
    }

    //exclusive-or-expression
    //	and-expression
    //	and-expression ^ exclusive-or-expression
    private ASTExpression exclusiveOrExpression() {
        ASTExpression expr1 = null;
        ASTToken op = null;
        ASTExpression expr2 = andExpression();
        while (tokenIsType("'^'")) {
            if (expr1 != null) {
                ASTBinaryExpression astBinaryExpression = new ASTBinaryExpression(op, expr1, expr2);
                astBinaryExpression.children.add(expr1);
                astBinaryExpression.children.add(expr2);
                astBinaryExpression.children.add(op);
                expr1 = astBinaryExpression;
            } else {
                expr1 = expr2;
            }
            op = getNextToken();
            expr2 = andExpression();
        }
        if (expr1 == null) {
            return expr2;
        } else {
            ASTBinaryExpression astBinaryExpression = new ASTBinaryExpression(op, expr1, expr2);
            astBinaryExpression.children.add(expr1);
            astBinaryExpression.children.add(expr2);
            astBinaryExpression.children.add(op);
            return astBinaryExpression;
        }
    }

    //and-expression
    //	equality-expression
    //	equality-expression & and-expression
    private ASTExpression andExpression() {
        ASTExpression expr1 = null;
        ASTToken op = null;
        ASTExpression expr2 = equalityExpression();
        while (tokenIsType("'&'")) {
            if (expr1 != null) {
                ASTBinaryExpression astBinaryExpression = new ASTBinaryExpression(op, expr1, expr2);
                astBinaryExpression.children.add(expr1);
                astBinaryExpression.children.add(expr2);
                astBinaryExpression.children.add(op);
                expr1 = astBinaryExpression;
            } else {
                expr1 = expr2;
            }
            op = getNextToken();
            expr2 = equalityExpression();
        }
        if (expr1 == null) {
            return expr2;
        } else {
            ASTBinaryExpression astBinaryExpression = new ASTBinaryExpression(op, expr1, expr2);
            astBinaryExpression.children.add(expr1);
            astBinaryExpression.children.add(expr2);
            astBinaryExpression.children.add(op);
            return astBinaryExpression;
        }
    }

    //equality-expression
    //	relational-expression
    //	relational-expression == equality-expression
    //	relational-expression != equality-expression
    private ASTExpression equalityExpression() {
        ASTExpression expr1 = null;
        ASTToken op = null;
        ASTExpression expr2 = relationalExpression();
        while (tokenIsType("'=='") || tokenIsType("'!='")) {
            if (expr1 != null) {
                ASTBinaryExpression astBinaryExpression = new ASTBinaryExpression(op, expr1, expr2);
                astBinaryExpression.children.add(expr1);
                astBinaryExpression.children.add(expr2);
                astBinaryExpression.children.add(op);
                expr1 = astBinaryExpression;
            } else {
                expr1 = expr2;
            }
            op = getNextToken();
            expr2 = relationalExpression();
        }
        if (expr1 == null) {
            return expr2;
        } else {
            ASTBinaryExpression astBinaryExpression = new ASTBinaryExpression(op, expr1, expr2);
            astBinaryExpression.children.add(expr1);
            astBinaryExpression.children.add(expr2);
            astBinaryExpression.children.add(op);
            return astBinaryExpression;
        }
    }

    //relational-expression
    //	shift-expression
    //	shift-expression < relational-expression
    //	shift-expression > relational-expression
    //	shift-expression <= relational-expression
    //	shift-expression >= relational-expression
    private ASTExpression relationalExpression() {
        ASTExpression expr1 = null;
        ASTToken op = null;
        ASTExpression expr2 = shiftExpression();
        while (tokenIsType("'<'") || tokenIsType("'>'") || tokenIsType("'<='") || tokenIsType("'>='")) {
            if (expr1 != null) {
                ASTBinaryExpression astBinaryExpression = new ASTBinaryExpression(op, expr1, expr2);
                astBinaryExpression.children.add(expr1);
                astBinaryExpression.children.add(expr2);
                astBinaryExpression.children.add(op);
                expr1 = astBinaryExpression;
            } else {
                expr1 = expr2;
            }
            op = getNextToken();
            expr2 = shiftExpression();
        }
        if (expr1 == null) {
            return expr2;
        } else {
            ASTBinaryExpression astBinaryExpression = new ASTBinaryExpression(op, expr1, expr2);
            astBinaryExpression.children.add(expr1);
            astBinaryExpression.children.add(expr2);
            astBinaryExpression.children.add(op);
            return astBinaryExpression;
        }
    }

    //shift-expression
    //	additive-expression
    //	additive-expression << shift-expression
    //	additive-expression >> shift-expression
    private ASTExpression shiftExpression() {
        ASTExpression expr1 = null;
        ASTToken op = null;
        ASTExpression expr2 = additiveExpression();
        while (tokenIsType("'<<'") || tokenIsType("'>>'")) {
            if (expr1 != null) {
                ASTBinaryExpression astBinaryExpression = new ASTBinaryExpression(op, expr1, expr2);
                astBinaryExpression.children.add(expr1);
                astBinaryExpression.children.add(expr2);
                astBinaryExpression.children.add(op);
                expr1 = astBinaryExpression;
            } else {
                expr1 = expr2;
            }
            op = getNextToken();
            expr2 = additiveExpression();
        }
        if (expr1 == null) {
            return expr2;
        } else {
            ASTBinaryExpression astBinaryExpression = new ASTBinaryExpression(op, expr1, expr2);
            astBinaryExpression.children.add(expr1);
            astBinaryExpression.children.add(expr2);
            astBinaryExpression.children.add(op);
            return astBinaryExpression;
        }
    }

    //additive-expression
    //	multiplicative-expression
    //	multiplicative-expression + additive-expression
    //	multiplicative-expression - additive-expression
    private ASTExpression additiveExpression() {
        ASTExpression expr1 = null;
        ASTToken op = null;
        ASTExpression expr2 = multiplicativeExpression();
        while (tokenIsType("'+'") || tokenIsType("'-'")) {
            if (expr1 != null) {
                ASTBinaryExpression astBinaryExpression = new ASTBinaryExpression(op, expr1, expr2);
                astBinaryExpression.children.add(expr1);
                astBinaryExpression.children.add(expr2);
                astBinaryExpression.children.add(op);
                expr1 = astBinaryExpression;
            } else {
                expr1 = expr2;
            }
            op = getNextToken();
            expr2 = multiplicativeExpression();
        }
        if (expr1 == null) {
            return expr2;
        } else {
            ASTBinaryExpression astBinaryExpression = new ASTBinaryExpression(op, expr1, expr2);
            astBinaryExpression.children.add(expr1);
            astBinaryExpression.children.add(expr2);
            astBinaryExpression.children.add(op);
            return astBinaryExpression;
        }
    }

    //multiplicative-expression
    //	cast-expression
    //	cast-expression * multiplicative-expression
    //	cast-expression / multiplicative-expression
    //	cast-expression % multiplicative-expression
    private ASTExpression multiplicativeExpression() {
        ASTExpression expr1 = null;
        ASTToken op = null;
        ASTExpression expr2 = castExpression();
        while (tokenIsType("'*'") || tokenIsType("'/'") || tokenIsType("'%'")) {
            if (expr1 != null) {
                ASTBinaryExpression astBinaryExpression = new ASTBinaryExpression(op, expr1, expr2);
                astBinaryExpression.children.add(expr1);
                astBinaryExpression.children.add(expr2);
                astBinaryExpression.children.add(op);
                expr1 = astBinaryExpression;
            } else {
                expr1 = expr2;
            }
            op = getNextToken();
            expr2 = castExpression();
        }
        if (expr1 == null) {
            return expr2;
        } else {
            ASTBinaryExpression astBinaryExpression = new ASTBinaryExpression(op, expr1, expr2);
            astBinaryExpression.children.add(expr1);
            astBinaryExpression.children.add(expr2);
            astBinaryExpression.children.add(op);
            return astBinaryExpression;
        }
    }

    //cast-expression
    //	unary-expression
    //	( specifier ) cast-expression
    private ASTExpression castExpression() {
        if (tokenIsType("'('") && tknList.get(tokenIndex + 1).isSpecifier()) {
            matchToken("'('");
            ASTToken mSpecifier = specifier();
            matchToken("')'");
            ASTExpression expr = castExpression();
            ASTCastExpression astCastExpression = new ASTCastExpression();

            ASTTypename astTypename = new ASTTypename();
            astTypename.specfiers = new ArrayList<>();
            astTypename.specfiers.add(mSpecifier);
            astTypename.children.add(mSpecifier);

            astCastExpression.typename = astTypename;
            astCastExpression.expr = expr;
            astCastExpression.children.add(astTypename);
            astCastExpression.children.add(expr);

            return astCastExpression;
        }
        return unaryExpression();
    }

    //unary-expression
    //	postfix-expression
    //	++ unary-expression
    //	-- unary-expression
    //	unary-operator cast-expression
    //	sizeof unary-expression
    //	sizeof specifier
    private ASTExpression unaryExpression() {
        if (tokenIsType("'++'") || tokenIsType("'--'")) {
            ASTToken op = getNextToken();
            ASTExpression expr = unaryExpression();
            ASTUnaryExpression astUnaryExpression = new ASTUnaryExpression(op, expr);
            astUnaryExpression.children.add(op);
            astUnaryExpression.children.add(expr);
            return astUnaryExpression;
        } else if (tknList.get(tokenIndex).isUnaryOperator()) {
            ASTToken op = unaryOperator();
            ASTExpression expr = castExpression();
            ASTUnaryExpression astUnaryExpression = new ASTUnaryExpression(op, expr);
            astUnaryExpression.children.add(op);
            astUnaryExpression.children.add(expr);
            return astUnaryExpression;
        } else if (tokenIsType("'sizeof'")) {
            ASTToken op = getNextToken();
            if (tknList.get(tokenIndex).isSpecifier()) {
                ASTToken mSpecifier = specifier();

                ASTTypename astTypename = new ASTTypename();
                astTypename.specfiers = new ArrayList<>();
                astTypename.specfiers.add(mSpecifier);
                astTypename.children.add(mSpecifier);

                ASTUnaryTypename astUnaryTypename = new ASTUnaryTypename();
                astUnaryTypename.op = op;
                astUnaryTypename.typename = astTypename;
                astUnaryTypename.children.add(op);
                astUnaryTypename.children.add(astTypename);

                return astUnaryTypename;
            } else {
                ASTExpression expr = unaryExpression();
                ASTUnaryExpression astUnaryExpression = new ASTUnaryExpression(op, expr);
                astUnaryExpression.children.add(op);
                astUnaryExpression.children.add(expr);
                return astUnaryExpression;
            }
        } else {
            return postfixExpression();
        }
    }

    //unary-operator
    //	&
    //	*
    //	+
    //	-
    //	~
    //	!
    private ASTToken unaryOperator() {
        ScannerToken scannerToken = tknList.get(tokenIndex);
        if (scannerToken.isUnaryOperator()) {
            ASTToken token = new ASTToken(scannerToken.lexme, tokenIndex);
            tokenIndex ++;
            return token;
        }
        return new ASTToken();
    }

    //postfix-expression
    //	primary-expression postfix-expression-post
    //postfix-expression-post
    //	[ expression ] postfix-expression-post
    //	( expression ) postfix-expression-post
    //	. identifier postfix-expression-post
    //	-> identifier postfix-expression-post
    //	++ postfix-expression-post
    //	-- postfix-expression-post
    //	ε
    private ASTExpression postfixExpression() {
        ASTExpression expr = primaryExpression();
        while (true) {
            if (tokenIsType("'['")) {
                ASTArrayAccess astArrayAccess = new ASTArrayAccess();

                matchToken("'['");
                ArrayList<ASTExpression> mExpression = expression();
                matchToken("']'");

                astArrayAccess.arrayName = expr;
                astArrayAccess.elements = mExpression;
                astArrayAccess.children.add(expr);
                astArrayAccess.children.addAll(mExpression);

                expr = astArrayAccess;

            } else if (tokenIsType("'('")) {
                ASTFunctionCall astFunctionCall = new ASTFunctionCall();
                astFunctionCall.funcname = expr;
                astFunctionCall.children.add(expr);
                matchToken("'('");
                if (!tokenIsType("')'")) {
                    ArrayList<ASTExpression> mExpression = expression();
                    astFunctionCall.argList = mExpression;
                    astFunctionCall.children.addAll(mExpression);
                }
                matchToken("')'");

                expr = astFunctionCall;

            } else if (tokenIsType("'.'") || tokenIsType("'->'")) {
                ASTMemberAccess astMemberAccess = new ASTMemberAccess();

                ASTToken op = getNextToken();

                ASTIdentifier astIdentifier = new ASTIdentifier();
                astIdentifier.tokenId = tokenIndex;
                astIdentifier.value = tknList.get(tokenIndex).lexme;
                matchToken("Identifier");

                astMemberAccess.master = expr;
                astMemberAccess.op = op;
                astMemberAccess.member = astIdentifier;

                astMemberAccess.children.add(expr);
                astMemberAccess.children.add(op);
                astMemberAccess.children.add(astIdentifier);

                expr = astMemberAccess;

            } else if (tokenIsType("'++'") || tokenIsType("'--'")) {
                ASTPostfixExpression astPostfixExpression = new ASTPostfixExpression();

                ASTToken op = getNextToken();

                astPostfixExpression.expr = expr;
                astPostfixExpression.op = op;

                astPostfixExpression.children.add(expr);
                astPostfixExpression.children.add(op);

                expr = astPostfixExpression;

            } else {
                break;
            }
        }
        return expr;
    }

    //primary-expression
    //	identifier
    //	constant
    //	string
    //	( assignment-expression )
    private ASTExpression primaryExpression() {
        if (tokenIsType("IntegerConstant")) {
            ASTIntegerConstant astIntegerConstant = new ASTIntegerConstant();
            astIntegerConstant.tokenId = tokenIndex;
            String lexme = tknList.get(tokenIndex).lexme;
            astIntegerConstant.value = Integer.parseInt(lexme);
            matchToken("IntegerConstant");
            return astIntegerConstant;
        } else if (tokenIsType("CharConstant")) {
            ASTCharConstant astCharConstant = new ASTCharConstant();
            astCharConstant.tokenId = tokenIndex;
            astCharConstant.value = tknList.get(tokenIndex).lexme;
            matchToken("CharConstant");
            return astCharConstant;
        } else if (tokenIsType("FloatingConstant")) {
            ASTFloatConstant astFloatConstant = new ASTFloatConstant();
            astFloatConstant.tokenId = tokenIndex;
            String lexme = tknList.get(tokenIndex).lexme;
            astFloatConstant.value = Double.parseDouble(lexme.substring(1, lexme.length() - 1));
            matchToken("FloatingConstant");
            return astFloatConstant;
        } else if (tokenIsType("StringLiteral")) {
            ASTStringConstant astStringConstant = new ASTStringConstant();
            astStringConstant.tokenId = tokenIndex;
            astStringConstant.value = tknList.get(tokenIndex).lexme;
            matchToken("StringLiteral");
            return astStringConstant;
        } else if (tokenIsType("'('")) {
            matchToken("'('");
            ASTExpression astExpression = assignmentExpression();
            matchToken("')'");
            return assignmentExpression();
        } else {
            ASTIdentifier astIdentifier = new ASTIdentifier();
            astIdentifier.tokenId = tokenIndex;
            astIdentifier.value = tknList.get(tokenIndex).lexme;
            matchToken("Identifier");
            return astIdentifier;
        }
    }

    //selection-statement
    //	if ( expression ) statement
    //	if ( expression ) statement else statement
    private ASTStatement selectionStatement() {
        matchToken("'if'");
        matchToken("'('");

        ASTSelectionStatement astSelectionStatement = new ASTSelectionStatement();
        LinkedList<ASTExpression> cond = new LinkedList<>(expression());
        astSelectionStatement.cond = cond;
        astSelectionStatement.children.addAll(cond);

        matchToken("')'");

        ASTStatement then = statement();
        astSelectionStatement.then = then;
        astSelectionStatement.children.add(then);

        if (tokenIsType("'else'")) {
            matchToken("'else'");
            ASTStatement otherwise = statement();
            astSelectionStatement.otherwise = otherwise;
            astSelectionStatement.children.add(otherwise);
        }

        return astSelectionStatement;
    }

    //iteration-statement
    //	for ( optional-expression ; optional-expression ; optional-expression ) statement
    //  for ( declaration optional-expression ; optional-expression ) statement
    //optional-expression
    //	ε
    //	expression
    private ASTStatement iterationStatement() {
        matchToken("'for'");
        matchToken("'('");

        if (tknList.get(tokenIndex).isSpecifier()) {
            ASTIterationDeclaredStatement astIterationDeclaredStatement = new ASTIterationDeclaredStatement();
            ASTDeclaration init = declaration();

            astIterationDeclaredStatement.init = init;
            astIterationDeclaredStatement.children.add(init);

            LinkedList<ASTExpression> mExpression;
            if (!tokenIsType("';'")) {
                mExpression = new LinkedList<>(expression());
                astIterationDeclaredStatement.cond = mExpression;
                astIterationDeclaredStatement.children.addAll(mExpression);
            }
            matchToken("';'");
            if (!tokenIsType("')'")) {
                mExpression = new LinkedList<>(expression());
                astIterationDeclaredStatement.step = mExpression;
                astIterationDeclaredStatement.children.addAll(mExpression);
            }
            matchToken("')'");
            ASTStatement mStatement = statement();
            astIterationDeclaredStatement.stat = mStatement;
            astIterationDeclaredStatement.children.add(mStatement);

            return astIterationDeclaredStatement;

        } else {
            ASTIterationStatement astIterationStatement = new ASTIterationStatement();
            LinkedList<ASTExpression> mExpression;
            if (!tokenIsType("';'")) {
                mExpression = new LinkedList<>(expression());
                astIterationStatement.init = mExpression;
                astIterationStatement.children.addAll(mExpression);
            }
            matchToken("';'");
            if (!tokenIsType("';'")) {
                mExpression = new LinkedList<>(expression());
                astIterationStatement.cond = mExpression;
                astIterationStatement.children.addAll(mExpression);
            }
            matchToken("';'");
            if (!tokenIsType("')'")) {
                mExpression = new LinkedList<>(expression());
                astIterationStatement.step = mExpression;
                astIterationStatement.children.addAll(mExpression);
            }
            matchToken("')'");
            ASTStatement mStatement = statement();
            astIterationStatement.stat = mStatement;
            astIterationStatement.children.add(mStatement);

            return astIterationStatement;
        }
    }

    //jump-statement
    //	return optional-expression ;
    private ASTStatement jumpStatement() {
        matchToken("'return'");
        ASTReturnStatement astReturnStatement = new ASTReturnStatement();
        if (!tokenIsType("';'")) {
            LinkedList<ASTExpression> mExpression = new LinkedList<>(expression());
            astReturnStatement.expr = mExpression;
            astReturnStatement.children.addAll(mExpression);
        }
        matchToken("';'");
        return astReturnStatement;
    }

    private ASTStatement breakStatement() {
        matchToken("'break'");
        ASTBreakStatement breakStatement = new ASTBreakStatement();
        matchToken("';'");
        return breakStatement;
    }

}
