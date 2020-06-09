package bit.minisys.minicc.icgen;

import bit.minisys.minicc.parser.ast.*;
import bit.minisys.minicc.semantic.symbol.FuncSymbol;
import bit.minisys.minicc.semantic.symbol.Specifier;
import bit.minisys.minicc.semantic.symbol.Symbol;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class WZICBuilder implements ASTVisitor {

    // map of ASTNode and its result
    // each ASTNode may have no or only one result
    // result could be symbol with normal identifier or TmpValSymbol
    // identifier of TmpValSymbol starts with "#" followed by tmp value id
    private final Map<ASTNode, Symbol> resultMap = new HashMap<>();

    // function info
    private final LinkedList<FuncInfo> funcInfoList = new LinkedList<>();

    // save current expression visit result to this symbol if not null
    private Symbol symbolToSave = null;

    // label for current loop
    private LabelSymbol loopNextLabel = null;
    private LabelSymbol loopEndLabel = null;

    private final WZTmpValueGenerator tmpValueGenerator = new WZTmpValueGenerator();
    private final WZLabelGenerator labelGenerator = new WZLabelGenerator();

    private Symbol getSymbol(String name) {
        Symbol s;
        // check current func
        FuncInfo current = getCurrentFuncInfo();
        if (current != null) {
            s = current.getSymbol(name);
            if (s != null) {
                return s;
            }
        }
        // check global
        s = funcInfoList.get(0).getSymbol(name);
        return s;
    }

    private void addSymbol(Symbol symbol) {
        FuncInfo funcInfo = getCurrentFuncInfo();
        if (funcInfo == null) {
            return;
        }
        funcInfo.addSymbol(symbol);
    }

    private FuncInfo getCurrentFuncInfo() {
        if (funcInfoList.isEmpty()) {
            return null;
        }
        return funcInfoList.getLast();
    }

    private void addQuat(WZQuat quat) {
        FuncInfo current = getCurrentFuncInfo();
        if (current != null) {
            current.addQuat(quat);
        }
    }

    public String getOutput() {
        StringBuilder output = new StringBuilder();
        for (FuncInfo funcInfo : funcInfoList) {
            output.append(funcInfo);
        }
        return new String(output);
    }

    public LinkedList<FuncInfo> getFuncInfoList() {
        return funcInfoList;
    }

    @Override
    public void visit(ASTCompilationUnit program) throws Exception {
        FuncInfo globalInfo = new FuncInfo(program);
        funcInfoList.add(globalInfo);
        for (ASTNode child : program.items) {
            if (child instanceof ASTDeclaration) {
                visit((ASTDeclaration) child);
            }
        }
        for (ASTNode child : program.items) {
            if (child instanceof ASTFunctionDefine) {
                visit((ASTFunctionDefine) child);
            }
        }
    }

    @Override
    public void visit(ASTDeclaration declaration) throws Exception {
        for (ASTInitList initList : declaration.initLists) {
            visit(initList);
        }
    }

    @Override
    public void visit(ASTArrayDeclarator arrayDeclarator) throws Exception {
        // todo: array
    }

    @Override
    public void visit(ASTVariableDeclarator variableDeclarator) throws Exception {
        visit(variableDeclarator.identifier);
        Symbol symbol = resultMap.get(variableDeclarator.identifier);
        resultMap.put(variableDeclarator, symbol);
    }

    @Override
    public void visit(ASTFunctionDeclarator functionDeclarator) throws Exception {
        // nothing need to do
    }

    @Override
    public void visit(ASTParamsDeclarator paramsDeclarator) throws Exception {
        // nothing need to do
    }

    @Override
    public void visit(ASTArrayAccess arrayAccess) throws Exception {
        // todo: array
    }

    @Override
    public void visit(ASTBinaryExpression binaryExpression) throws Exception {
        // todo: more op
        WZQuat.WZ_QUAT_OP op = null;
        Symbol opnd1 = null, opnd2 = null, result = null;

        // intercept save information
        result = symbolToSave;
        symbolToSave = null;

        if (binaryExpression.op.value.equals("=")) {
            op = WZQuat.WZ_QUAT_OP.ASSIGN;
            visit(binaryExpression.expr1);
            Symbol opnd3 = resultMap.get(binaryExpression.expr1);
            symbolToSave = opnd3;
            visit(binaryExpression.expr2);
            opnd1 = resultMap.get(binaryExpression.expr2);
            if (symbolToSave != null) {
                // not intercepted
                symbolToSave = null;
                WZQuat quat = new WZQuat(op, opnd1, null, opnd3);
                addQuat(quat);
            }
            if (result != null) {
                WZQuat outerQuat = new WZQuat(op, opnd3, null, result);
                addQuat(outerQuat);
            }
            resultMap.put(binaryExpression, opnd3);
            return;
        } else if (binaryExpression.op.value.equals("+")) {
            op = WZQuat.WZ_QUAT_OP.ADD;
        } else if (binaryExpression.op.value.equals("*")) {
            op = WZQuat.WZ_QUAT_OP.MUL;
        } else if (binaryExpression.op.value.equals("-")) {
            op = WZQuat.WZ_QUAT_OP.SUB;
        } else if (binaryExpression.op.value.equals("/")) {
            op = WZQuat.WZ_QUAT_OP.DIV;
        } else if (binaryExpression.op.value.equals("%")) {
            op = WZQuat.WZ_QUAT_OP.MOD;
        } else if (binaryExpression.op.value.equals("==")) {
            op = WZQuat.WZ_QUAT_OP.EQ;
        } else if (binaryExpression.op.value.equals(">")) {
            op = WZQuat.WZ_QUAT_OP.MT;
        } else if (binaryExpression.op.value.equals("<")) {
            op = WZQuat.WZ_QUAT_OP.LT;
        } else if (binaryExpression.op.value.equals(">=")) {
            op = WZQuat.WZ_QUAT_OP.MET;
        } else if (binaryExpression.op.value.equals("<=")) {
            op = WZQuat.WZ_QUAT_OP.LET;
        } else if (binaryExpression.op.value.equals("&&")) {
            op = WZQuat.WZ_QUAT_OP.AND;
        } else if (binaryExpression.op.value.equals("||")) {
            op = WZQuat.WZ_QUAT_OP.OR;
        }
        visit(binaryExpression.expr1);
        opnd1 = resultMap.get(binaryExpression.expr1);
        visit(binaryExpression.expr2);
        opnd2 = resultMap.get(binaryExpression.expr2);
        if (result == null) {
            result = tmpValueGenerator.genNewTmpVal();
            if (opnd1 instanceof ConstantSymbol && opnd2 instanceof ConstantSymbol) {
                boolean isFloat = false;
                if (((ConstantSymbol) opnd1).getType() == ConstantSymbol.CONST_TYPE.FLOAT) {
                    isFloat = true;
                }
                if (((ConstantSymbol) opnd2).getType() == ConstantSymbol.CONST_TYPE.FLOAT) {
                    isFloat = true;
                }
                Specifier specifier = new Specifier();
                if (isFloat) {
                    specifier.addSpecifier("double");
                } else {
                    specifier.addSpecifier("int");
                }
                result.setSpecifier(specifier);
            } else if (opnd1 instanceof ConstantSymbol) {
                result.setSpecifier(opnd2.getSpecifier());
            } else {
                result.setSpecifier(opnd1.getSpecifier());
            }
            addSymbol(result);
        }
        WZQuat quat = new WZQuat(op, opnd1, opnd2, result);
        addQuat(quat);
        resultMap.put(binaryExpression, result);
    }

    @Override
    public void visit(ASTBreakStatement breakStat) throws Exception {
        WZQuat quat = new WZQuat(WZQuat.WZ_QUAT_OP.JMP, null, null, loopEndLabel);
        addQuat(quat);
    }

    @Override
    public void visit(ASTContinueStatement continueStatement) throws Exception {
        WZQuat quat = new WZQuat(WZQuat.WZ_QUAT_OP.JMP, null, null, loopNextLabel);
        addQuat(quat);
    }

    @Override
    public void visit(ASTCastExpression castExpression) throws Exception {
        visit(castExpression.typename);
        Symbol symbol = resultMap.get(castExpression.typename);
        visit(castExpression.expr);
        Symbol opnd = resultMap.get(castExpression.expr);
        TmpValSymbol tmpValSymbol = tmpValueGenerator.genNewTmpVal();
        tmpValSymbol.setSpecifier(symbol.getSpecifier());
        addSymbol(tmpValSymbol);
        WZQuat quat = new WZQuat(WZQuat.WZ_QUAT_OP.ASSIGN, opnd, null, tmpValSymbol);
        addQuat(quat);
        resultMap.put(castExpression, tmpValSymbol);
    }

    @Override
    public void visit(ASTCharConstant charConst) throws Exception {
        ConstantSymbol symbol = new ConstantSymbol(charConst);
        resultMap.put(charConst, symbol);
    }

    @Override
    public void visit(ASTCompoundStatement compoundStat) throws Exception {
        for (ASTNode child : compoundStat.blockItems) {
            if (child instanceof ASTStatement) {
                visit((ASTStatement) child);
            } else if (child instanceof  ASTDeclaration) {
                visit((ASTDeclaration) child);
            }
        }
    }

    @Override
    public void visit(ASTConditionExpression conditionExpression) throws Exception {
        // intercept save information
        Symbol result = symbolToSave;
        symbolToSave = null;

        if (result == null) {
            result = tmpValueGenerator.genNewTmpVal();
        }

        visit(conditionExpression.condExpr);
        Symbol condition = resultMap.get(conditionExpression.condExpr);

        LabelSymbol falseLabel = labelGenerator.genNewLabel();
        LabelSymbol nextLabel = labelGenerator.genNewLabel();

        addQuat(new WZQuat(WZQuat.WZ_QUAT_OP.JF, condition, null, falseLabel));

        for (ASTExpression expression : conditionExpression.trueExpr) {
            visit(expression);
        }
        Symbol trueSymbol = resultMap.get(conditionExpression.trueExpr.get(0));
        if (result instanceof TmpValSymbol) {
            result.setSpecifier(trueSymbol.getSpecifier());
            addSymbol(result);
        }
        addQuat(new WZQuat(WZQuat.WZ_QUAT_OP.ASSIGN, trueSymbol, null, result));
        addQuat(new WZQuat(WZQuat.WZ_QUAT_OP.JMP, null, null, nextLabel));

        addQuat(new WZQuat(WZQuat.WZ_QUAT_OP.LABEL, null, null, falseLabel));
        visit(conditionExpression.falseExpr);
        Symbol falseSymbol = resultMap.get(conditionExpression.falseExpr);
        addQuat(new WZQuat(WZQuat.WZ_QUAT_OP.ASSIGN, falseSymbol, null, result));

        addQuat(new WZQuat(WZQuat.WZ_QUAT_OP.LABEL, null, null, nextLabel));

        resultMap.put(conditionExpression, result);
    }

    @Override
    public void visit(ASTExpression expression) throws Exception {
        if(expression instanceof ASTArrayAccess) {
            visit((ASTArrayAccess)expression);
        }else if(expression instanceof ASTBinaryExpression) {
            visit((ASTBinaryExpression)expression);
        }else if(expression instanceof ASTCastExpression) {
            visit((ASTCastExpression)expression);
        }else if(expression instanceof ASTCharConstant) {
            visit((ASTCharConstant)expression);
        }else if(expression instanceof ASTConditionExpression) {
            visit((ASTConditionExpression)expression);
        }else if(expression instanceof ASTFloatConstant) {
            visit((ASTFloatConstant)expression);
        }else if(expression instanceof ASTFunctionCall) {
            visit((ASTFunctionCall)expression);
        }else if(expression instanceof ASTIdentifier) {
            visit((ASTIdentifier)expression);
        }else if(expression instanceof ASTIntegerConstant) {
            visit((ASTIntegerConstant)expression);
        }else if(expression instanceof ASTMemberAccess) {
            visit((ASTMemberAccess)expression);
        }else if(expression instanceof ASTPostfixExpression) {
            visit((ASTPostfixExpression)expression);
        }else if(expression instanceof ASTStringConstant) {
            visit((ASTStringConstant)expression);
        }else if(expression instanceof ASTUnaryExpression) {
            visit((ASTUnaryExpression)expression);
        }else if(expression instanceof ASTUnaryTypename){
            visit((ASTUnaryTypename)expression);
        }
    }

    @Override
    public void visit(ASTExpressionStatement expressionStat) throws Exception {
        for (ASTExpression expression : expressionStat.exprs) {
            visit(expression);
        }
        Symbol symbol = resultMap.get(expressionStat.exprs.get(0));
        resultMap.put(expressionStat, symbol);
    }

    @Override
    public void visit(ASTFloatConstant floatConst) throws Exception {
        ConstantSymbol symbol = new ConstantSymbol(floatConst);
        resultMap.put(floatConst, symbol);
    }

    @Override
    public void visit(ASTFunctionCall funcCall) throws Exception {
        visit(funcCall.funcname);
        Symbol funcSymbol = resultMap.get(funcCall.funcname);
        for (int i = funcCall.argList.size() - 1; i >= 0; i--) {
            ASTExpression expression = funcCall.argList.get(i);
            visit(expression);
            Symbol arg = resultMap.get(expression);
            addQuat(new WZQuat(WZQuat.WZ_QUAT_OP.ARG, null, null, arg));
        }
        addQuat(new WZQuat(WZQuat.WZ_QUAT_OP.CALL, null, null, funcSymbol));
        if (((FuncSymbol) funcSymbol).needReturn()) {
            // intercept save information
            Symbol result = symbolToSave;
            symbolToSave = null;
            if (result == null) {
                result = tmpValueGenerator.genNewTmpVal();
                result.setSpecifier(funcSymbol.getSpecifier());
                addSymbol(result);
            }
            addQuat(new WZQuat(WZQuat.WZ_QUAT_OP.GRV, null, null, result));
            resultMap.put(funcCall, result);
        }
    }

    @Override
    public void visit(ASTGotoStatement gotoStat) throws Exception {
        // todo: goto
    }

    @Override
    public void visit(ASTIdentifier identifier) throws Exception {
        Symbol symbol = getSymbol(identifier.value);
        resultMap.put(identifier, symbol);
    }

    @Override
    public void visit(ASTInitList initList) throws Exception {
        if (initList.exprs == null || initList.exprs.isEmpty()) {
            return;
        }
        if (initList.declarator instanceof ASTVariableDeclarator) {
            visit(initList.declarator);
            Symbol result = resultMap.get(initList.declarator);
            ASTExpression expression = initList.exprs.get(0);
            visit(expression);
            Symbol opnd = resultMap.get(expression);
            WZQuat quat = new WZQuat(WZQuat.WZ_QUAT_OP.ASSIGN, opnd, null, result);
            addQuat(quat);
        } else if (initList.declarator instanceof ASTArrayDeclarator) {
            // todo: array
        } else if (initList.declarator instanceof ASTFunctionDeclarator) {
            // nothing need to do
        }
    }

    @Override
    public void visit(ASTIntegerConstant intConst) throws Exception {
        ConstantSymbol symbol = new ConstantSymbol(intConst);
        resultMap.put(intConst, symbol);
    }

    @Override
    public void visit(ASTIterationDeclaredStatement iterationDeclaredStat) throws Exception {
        // todo: Iteration Declared Statement
    }

    @Override
    public void visit(ASTIterationStatement iterationStat) throws Exception {
        LabelSymbol oldNextLabel = loopNextLabel;
        LabelSymbol oldEndLabel = loopEndLabel;
        LabelSymbol loopCheckLabel = labelGenerator.genNewLabel();
        loopNextLabel = labelGenerator.genNewLabel();;
        loopEndLabel = labelGenerator.genNewLabel();;
        if (iterationStat.init != null) {
            for (ASTExpression expression : iterationStat.init) {
                visit(expression);
            }
        }
        addQuat(new WZQuat(WZQuat.WZ_QUAT_OP.LABEL, null, null, loopCheckLabel));
        if (iterationStat.cond != null && !iterationStat.cond.isEmpty()) {
            for (ASTExpression expression : iterationStat.cond) {
                visit(expression);
            }
            Symbol result = resultMap.get(iterationStat.cond.get(0));
            addQuat(new WZQuat(WZQuat.WZ_QUAT_OP.JF, result, null, loopEndLabel));
        }
        if (iterationStat.stat != null) {
            visit(iterationStat.stat);
        }
        addQuat(new WZQuat(WZQuat.WZ_QUAT_OP.LABEL, null, null, loopNextLabel));
        if (iterationStat.step != null) {
            for (ASTExpression expression : iterationStat.step) {
                visit(expression);
            }
        }
        addQuat(new WZQuat(WZQuat.WZ_QUAT_OP.JMP, null, null, loopCheckLabel));
        addQuat(new WZQuat(WZQuat.WZ_QUAT_OP.LABEL, null, null, loopEndLabel));
        loopNextLabel = oldNextLabel;
        loopEndLabel = oldEndLabel;
    }

    @Override
    public void visit(ASTLabeledStatement labeledStat) throws Exception {
        // todo: goto
    }

    @Override
    public void visit(ASTMemberAccess memberAccess) throws Exception {
        // todo: member access
    }

    @Override
    public void visit(ASTPostfixExpression postfixExpression) throws Exception {
        // todo: more op
        TmpValSymbol tmpValSymbol = tmpValueGenerator.genNewTmpVal();
        visit(postfixExpression.expr);
        Symbol opnd = resultMap.get(postfixExpression.expr);
        tmpValSymbol.setSpecifier(opnd.getSpecifier());
        addSymbol(tmpValSymbol);
        WZQuat quat1 = new WZQuat(WZQuat.WZ_QUAT_OP.ASSIGN, opnd, null, tmpValSymbol);
        addQuat(quat1);
        if (postfixExpression.op.value.equals("++")) {
            ConstantSymbol constantSymbol = new ConstantSymbol(1);
            WZQuat quat2 = new WZQuat(WZQuat.WZ_QUAT_OP.ADD, opnd, constantSymbol, opnd);
            addQuat(quat2);
        } else if (postfixExpression.op.value.equals("--")) {
            ConstantSymbol constantSymbol = new ConstantSymbol(1);
            WZQuat quat2 = new WZQuat(WZQuat.WZ_QUAT_OP.SUB, opnd, constantSymbol, opnd);
            addQuat(quat2);
        }
        resultMap.put(postfixExpression, tmpValSymbol);
    }

    @Override
    public void visit(ASTReturnStatement returnStat) throws Exception {
        if (returnStat.expr == null || returnStat.expr.isEmpty()) {
            addQuat(new WZQuat(WZQuat.WZ_QUAT_OP.RET, null, null, null));
            return;
        }
        for (ASTExpression expression : returnStat.expr) {
            visit(expression);
        }
        Symbol returnSymbol = resultMap.get(returnStat.expr.get(0));
        addQuat(new WZQuat(WZQuat.WZ_QUAT_OP.RET, null, null, returnSymbol));
    }

    @Override
    public void visit(ASTSelectionStatement selectionStat) throws Exception {
        for (ASTExpression expression : selectionStat.cond) {
            visit(expression);
        }
        Symbol cond = resultMap.get(selectionStat.cond.get(0));
        LabelSymbol notTrue = labelGenerator.genNewLabel();
        addQuat(new WZQuat(WZQuat.WZ_QUAT_OP.JF, cond, null, notTrue));
        visit(selectionStat.then);
        LabelSymbol endIf = null;
        if (selectionStat.otherwise != null) {
            endIf = labelGenerator.genNewLabel();
            addQuat(new WZQuat(WZQuat.WZ_QUAT_OP.JMP, null, null, endIf));
        }
        addQuat(new WZQuat(WZQuat.WZ_QUAT_OP.LABEL, null, null, notTrue));
        if (selectionStat.otherwise != null) {
            visit(selectionStat.otherwise);
            addQuat(new WZQuat(WZQuat.WZ_QUAT_OP.LABEL, null, null, endIf));
        }
    }

    @Override
    public void visit(ASTStringConstant stringConst) throws Exception {
        ConstantSymbol symbol = new ConstantSymbol(stringConst);
        resultMap.put(stringConst, symbol);
    }

    @Override
    public void visit(ASTTypename typename) throws Exception {
        Specifier specifier = new Specifier();
        for (ASTToken token : typename.specfiers) {
            specifier.addSpecifier(token.value);
        }
        Symbol symbol = new Symbol();
        symbol.setSpecifier(specifier);
        resultMap.put(typename, symbol);
    }

    @Override
    public void visit(ASTUnaryExpression unaryExpression) throws Exception {
        visit(unaryExpression.expr);
        Symbol opnd = resultMap.get(unaryExpression.expr);
        if (unaryExpression.op.value.equals("++")) {
            ConstantSymbol constant1 = new ConstantSymbol(1);
            addQuat(new WZQuat(WZQuat.WZ_QUAT_OP.ADD, opnd, constant1, opnd));
        } else if (unaryExpression.op.value.equals("--")) {
            ConstantSymbol constant1 = new ConstantSymbol(1);
            addQuat(new WZQuat(WZQuat.WZ_QUAT_OP.SUB, opnd, constant1, opnd));
        }
        resultMap.put(unaryExpression, opnd);
    }

    @Override
    public void visit(ASTUnaryTypename unaryTypename) throws Exception {
        // todo: unary typename
    }

    @Override
    public void visit(ASTFunctionDefine functionDefine) throws Exception {
        FuncInfo funcInfo = new FuncInfo(functionDefine);
        funcInfoList.add(funcInfo);
        visit(functionDefine.body);
    }

    @Override
    public void visit(ASTDeclarator declarator) throws Exception {
        if (declarator instanceof ASTVariableDeclarator) {
            visit((ASTVariableDeclarator) declarator);
        } else if (declarator instanceof ASTArrayDeclarator) {
            visit((ASTArrayDeclarator) declarator);
        } else if (declarator instanceof ASTFunctionDeclarator) {
            visit((ASTFunctionDeclarator) declarator);
        }
    }

    @Override
    public void visit(ASTStatement statement) throws Exception {
        if(statement instanceof ASTIterationDeclaredStatement) {
            visit((ASTIterationDeclaredStatement)statement);
        }else if(statement instanceof ASTIterationStatement) {
            visit((ASTIterationStatement)statement);
        }else if(statement instanceof ASTCompoundStatement) {
            visit((ASTCompoundStatement)statement);
        }else if(statement instanceof ASTSelectionStatement) {
            visit((ASTSelectionStatement)statement);
        }else if(statement instanceof ASTExpressionStatement) {
            visit((ASTExpressionStatement)statement);
        }else if(statement instanceof ASTBreakStatement) {
            visit((ASTBreakStatement)statement);
        }else if(statement instanceof ASTContinueStatement) {
            visit((ASTContinueStatement)statement);
        }else if(statement instanceof ASTReturnStatement) {
            visit((ASTReturnStatement)statement);
        }else if(statement instanceof ASTGotoStatement) {
            visit((ASTGotoStatement)statement);
        }else if(statement instanceof ASTLabeledStatement) {
            visit((ASTLabeledStatement)statement);
        }
    }

    @Override
    public void visit(ASTToken token) throws Exception {
        // nothing need to do
    }
}
