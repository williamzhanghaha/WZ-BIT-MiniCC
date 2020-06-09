package bit.minisys.minicc.semantic;

import bit.minisys.minicc.parser.ast.*;
import bit.minisys.minicc.semantic.symbol.*;

import java.util.*;

public class NodeInfo {

    // corresponding ASTNode
    private ASTNode astNode = null;

    // scope flag
    private boolean inIteration = false;
    private boolean isGlobal = false;

    // return check flag
    // true if this is a return statement
    //         or have return in all selection
    private boolean hasReturn = false;

    // parent NodeInfo
    private NodeInfo parent = null;

    // symbol table
    private final SymbolTable symbolTable = new SymbolTable();

    // error handler
    private final ErrorHandler errorHandler;

    private NodeInfo(ASTNode astNode, NodeInfo parent) {
        this.astNode = astNode;
        astNode.info = this;
        this.parent = parent;
        this.errorHandler = parent.errorHandler;
    }

    // constructor for Compilation Unit
    public NodeInfo(ASTCompilationUnit program, ErrorHandler errorHandler) {
        this.astNode = program;
        astNode.info = this;
        this.parent = null;
        this.errorHandler = errorHandler;
        this.isGlobal = true;
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    // if current node is a program, visit and return true
    public boolean visitProgram() {
        if (this.astNode instanceof ASTCompilationUnit && this.parent == null) {
            visitAsCompilationUnit();
            return true;
        }
        return false;
    }

    // return new child NoteInfo
    private NodeInfo createChild(ASTNode node) {
        NodeInfo child =  new NodeInfo(node, this);
        return child;
    }

    // add symbol to current symbol table
    // error when identifier has already been in current symbol table
    private void addSymbol(Symbol symbol) {
        if (!this.symbolTable.addSymbol(symbol)) {
            // duplicate declaration
            errorHandler.duplicateDeclaration(symbol.getIdentifier());
        }
    }

    // look for identifier in current and ancestors' symbol table
    // return null if not found
    private Symbol findSymbol(String identifier) {
        NodeInfo nodeInfo = this;
        while (nodeInfo != null) {
            Symbol symbol = nodeInfo.symbolTable.getSymbol(identifier);
            if (symbol != null) {
                // symbol found in current layer
                return symbol;
            }
            nodeInfo = nodeInfo.parent;
        }
        return null;
    }

    private void mergeChildSymbolTable(NodeInfo child) {
        for (Symbol s : child.symbolTable.getSymbols()) {
            if (!s.isUpNeed()) continue;
            if (this.symbolTable.getSymbol(s.getIdentifier()) != null) {
                errorHandler.duplicateDeclaration(s.getIdentifier());
                s.setUp(false);
            }
        }
        this.symbolTable.mergeChildTable(child.symbolTable, false);
    }

    // visit as [type]

    private void visitAsCompilationUnit() {
        if (!(astNode instanceof ASTCompilationUnit)) return;
        ASTCompilationUnit program = (ASTCompilationUnit) astNode;
        for (ASTNode childAstNode : program.items) {
            // for each item in program
            // check type
            if (childAstNode instanceof ASTDeclaration) {
                // is declaration
                // create child node
                NodeInfo child = createChild(childAstNode);
                child.isGlobal = this.isGlobal;
                // visit as declaration
                child.visitAsDeclaration();
                // check and add child's table content to parent's table
                mergeChildSymbolTable(child);
            } else if (childAstNode instanceof ASTFunctionDefine) {
                NodeInfo child = createChild(childAstNode);
                child.isGlobal = this.isGlobal;
                child.visitAsFunctionDefine();
                mergeChildSymbolTable(child);
            } else {
                errorHandler.handleErrorMsg("CompilationUnit's content must consist of declaration or function define");
            }
        }
    }

    private void visitAsDeclaration() {
        if (!(astNode instanceof ASTDeclaration)) return;
        ASTDeclaration declaration = (ASTDeclaration) astNode;
        // specifier
        Specifier specifier = new Specifier();
        for (ASTToken specifierToken : declaration.specifiers) {
            specifier.addSpecifier(specifierToken.value);
        }
        // init list
        for (ASTInitList initList : declaration.initLists) {
            NodeInfo child = createChild(initList);
            child.isGlobal = this.isGlobal;
            child.visitAsInitList();
            // for each init list, check if its child already declared
            for (Symbol s : child.symbolTable.getSymbols()) {
                if (!s.isUpNeed()) continue;
                if (this.symbolTable.getSymbol(s.getIdentifier()) != null ) {
                    errorHandler.duplicateDeclaration(s.getIdentifier());
                    s.setUp(false);
                }
            }
            this.symbolTable.mergeChildTableWithSpecifier(specifier, child.symbolTable, true);
        }
    }

    private void visitAsArrayDeclarator() {
        // symbol table should have only one array symbol or nothing after call this method
        if (!(astNode instanceof ASTArrayDeclarator)) return;
        ASTArrayDeclarator arrayDeclarator = (ASTArrayDeclarator) astNode;
        // expr
        // -1 means not concerned
        int size = -1;
        if (arrayDeclarator.expr instanceof ASTIntegerConstant) {
            size = ((ASTIntegerConstant) arrayDeclarator.expr).value;
        } else {
            // the size of current dimension not concerned
            NodeInfo child = createChild(arrayDeclarator.expr);
            // just check, do not merge symbol table
            child.visitAsExpression();
        }
        // declarator
        ArraySymbol arraySymbol;
        if (arrayDeclarator.declarator instanceof ASTVariableDeclarator) {
            // first dimension
            arraySymbol = new ArraySymbol(((ASTVariableDeclarator) arrayDeclarator.declarator).identifier.value, size);
        } else if (arrayDeclarator.declarator instanceof ASTArrayDeclarator) {
            // multiple dimension
            NodeInfo child = createChild(arrayDeclarator.declarator);
            child.visitAsArrayDeclarator();
            // child's first (and only) symbol should be an array symbol (if not empty)
            if (child.symbolTable.getSymbols().isEmpty()) {
                return;
            }
            ArraySymbol childSymbol = (ArraySymbol) child.symbolTable.getSymbols().get(0);
            arraySymbol = new ArraySymbol(childSymbol, size);
        } else {
            errorHandler.handleErrorMsg("ArrayDeclarator's declarator item must be ArrayDeclarator or VarDeclarator");
            return;
        }
        // add to current symbol table
        arraySymbol.setUp(true);
        addSymbol(arraySymbol);
    }

    private void visitAsVariableDeclarator() {
        if (!(astNode instanceof ASTVariableDeclarator)) return;
        VarSymbol varSymbol = new VarSymbol(((ASTVariableDeclarator) astNode).identifier.value);
        varSymbol.setUp(true);
        addSymbol(varSymbol);
    }

    private void visitAsFunctionDeclarator() {
        if (!(astNode instanceof ASTFunctionDeclarator)) return;
        ASTFunctionDeclarator functionDeclarator = (ASTFunctionDeclarator) astNode;
        // check scope
        if (!this.isGlobal) {
            errorHandler.handleErrorMsg("FunctionDeclarator must be in global scope");
        }
        // declarator (identifier)
        if (!(functionDeclarator.declarator instanceof ASTVariableDeclarator)) {
            errorHandler.handleErrorMsg("FunctionDeclarator's declarator must be an identifier");
            return;
        }
        String identifier = ((ASTVariableDeclarator) functionDeclarator.declarator).identifier.value;
        // parameters
        List<Symbol> paramType = new ArrayList<>();
        Set<String> paramNameSet = new HashSet<>();
        if (functionDeclarator.params != null) {
            for (ASTParamsDeclarator paramsDeclarator : functionDeclarator.params) {
                NodeInfo child = createChild(paramsDeclarator);
                child.visitAsParamsDeclarator();
                for (Symbol s : child.symbolTable.getSymbols()) {
                    if (!s.isUpNeed()) continue;
                    if (!s.hasSpecifier()) {
                        // have no type
                        errorHandler.handleErrorMsg("Function Parameter must have a type");
                        continue;
                    }
                    if (s.hasIdentifier()) {
                        if (paramNameSet.contains(s.getIdentifier())) {
                            // name existed
                            errorHandler.handleErrorMsg("Function Parameter's Identifier must be unique");
                            continue;
                        }
                        paramNameSet.add(s.getIdentifier());
                    }
                    paramType.add(s);
                }
            }
        }
        FuncSymbol funcSymbol = new FuncSymbol(identifier, paramType);
        funcSymbol.setUp(true);
        addSymbol(funcSymbol);
    }

    private void visitAsParamsDeclarator() {
        if (!(astNode instanceof ASTParamsDeclarator)) return;
        ASTParamsDeclarator paramsDeclarator = (ASTParamsDeclarator) astNode;
        // specifier
        Specifier specifier = new Specifier();
        for (ASTToken specifierToken : paramsDeclarator.specfiers) {
            specifier.addSpecifier(specifierToken.value);
        }
        // declarator
        if (paramsDeclarator.declarator == null) {
            Symbol symbol = new Symbol();
            symbol.setSpecifier(specifier);
            symbol.setUp(true);
            addSymbol(symbol);
        } else if (paramsDeclarator.declarator instanceof ASTVariableDeclarator) {
            VarSymbol symbol = new VarSymbol(((ASTVariableDeclarator) paramsDeclarator.declarator).identifier.value);
            symbol.setSpecifier(specifier);
            symbol.setUp(true);
            addSymbol(symbol);
        } else if (paramsDeclarator.declarator instanceof ASTArrayDeclarator) {
            NodeInfo child = createChild(paramsDeclarator.declarator);
            child.visitAsArrayDeclarator();
            if (child.symbolTable.getSymbols().isEmpty()) {
                return;
            }
            ArraySymbol symbol = (ArraySymbol) child.symbolTable.getSymbols().get(0);
            symbol = (ArraySymbol) symbol.copy();
            symbol.setSpecifier(specifier);
            symbol.setUp(true);
            addSymbol(symbol);
        } else {
            errorHandler.handleErrorMsg("ParameterDeclarator cannot be FunctionDeclarator");
        }
    }

    private void visitAsArrayAccess() {
        if (!(astNode instanceof ASTArrayAccess)) return;
        ASTArrayAccess arrayAccess = (ASTArrayAccess) astNode;
        List<Integer> arrayIndexes = new LinkedList<>();
        ASTExpression ptr = arrayAccess;
        boolean outOfBounds = false;
        while (ptr instanceof ASTArrayAccess) {
            ASTArrayAccess arrayName = (ASTArrayAccess) ptr;
            if (arrayName.elements.size() == 1 && arrayName.elements.get(0) instanceof ASTIntegerConstant) {
                int index = ((ASTIntegerConstant) arrayName.elements.get(0)).value;
                if (index < 0) {
                    outOfBounds = true;
                    arrayIndexes.add(0, -1);
                }
                arrayIndexes.add(0, index);
            } else {
                // not concern
                arrayIndexes.add(0, -1);
            }

            // check element expression
            for (ASTExpression expression : arrayName.elements) {
                NodeInfo child = createChild(expression);
                child.visitAsExpression();
            }

            ptr = arrayName.arrayName;
        }
        if (!(ptr instanceof ASTIdentifier)) {
            NodeInfo child = createChild(ptr);
            child.visitAsExpression();
            return;
        }
        String name = ((ASTIdentifier) ptr).value;
        Symbol symbolFound = findSymbol(name);
        if (!(symbolFound instanceof ArraySymbol)) {
            errorHandler.notDefined(name);
            return;
        }
        ArraySymbol arraySymbol = (ArraySymbol) symbolFound;
        // check indexes
        if (!outOfBounds) {
            outOfBounds = !arraySymbol.checkIndexed(arrayIndexes);
        }
        if (outOfBounds) {
            errorHandler.arrayAccessOut(name);
        }
    }

    private void visitAsBinaryExpression() {
        if (!(astNode instanceof ASTBinaryExpression)) return;
        ASTBinaryExpression binaryExpression = (ASTBinaryExpression) astNode;
        NodeInfo child1 = createChild(binaryExpression.expr1);
        NodeInfo child2 = createChild(binaryExpression.expr2);
        child1.visitAsExpression();
        child2.visitAsExpression();
    }

    private void visitAsBreakStatement() {
        if (!(astNode instanceof ASTBreakStatement)) return;
        if (!this.inIteration) {
            errorHandler.breakNotInLoop();
        }
    }

    private void visitAsContinueStatement() {
        if (!(astNode instanceof ASTContinueStatement)) return;
        if (!this.inIteration) {
            errorHandler.handleErrorMsg("Continue statement must be in a loop");
        }
    }

    private void visitAsCastExpression() {
        if (!(astNode instanceof ASTCastExpression)) return;
        NodeInfo child = createChild(((ASTCastExpression) astNode).expr);
        child.visitAsExpression();
    }

    private void visitAsCharConstant() {
        // nothing need to do
    }

    private void visitAsCompoundStatement() {
        if (!(astNode instanceof ASTCompoundStatement)) return;
        ASTCompoundStatement compoundStatement = (ASTCompoundStatement) astNode;
        for (ASTNode node : compoundStatement.blockItems) {
            if (node instanceof ASTStatement) {
                NodeInfo child = createChild(node);
                child.inIteration = this.inIteration;
                child.visitAsStatement();
                mergeChildSymbolTable(child);
                if (child.hasReturn) {
                    this.hasReturn = true;
                }
            } else if (node instanceof ASTDeclaration) {
                NodeInfo child = createChild(node);
                child.visitAsDeclaration();
                mergeChildSymbolTable(child);
            } else {
                errorHandler.handleErrorMsg("Items in CompoundStatement must be Statement or Declaration");
            }
        }
    }

    private void visitAsConditionExpression() {
        if (!(astNode instanceof ASTConditionExpression)) return;
        ASTConditionExpression conditionExpression = (ASTConditionExpression) astNode;
        NodeInfo condChild = createChild(conditionExpression.condExpr);
        condChild.visitAsExpression();
        for (ASTExpression expression : conditionExpression.trueExpr) {
            NodeInfo child = createChild(expression);
            child.visitAsExpression();
        }
        NodeInfo falseChild = createChild(conditionExpression.falseExpr);
        falseChild.visitAsExpression();
    }

    private void visitAsExpression() {
        if (astNode instanceof ASTIdentifier) visitAsIdentifier();
        else if (astNode instanceof ASTArrayAccess) visitAsArrayAccess();
        else if (astNode instanceof ASTBinaryExpression) visitAsBinaryExpression();
        else if (astNode instanceof ASTCastExpression) visitAsCastExpression();
        else if (astNode instanceof ASTCharConstant) visitAsCharConstant();
        else if (astNode instanceof ASTConditionExpression) visitAsConditionExpression();
        else if (astNode instanceof ASTFloatConstant) visitAsFloatConstant();
        else if (astNode instanceof ASTFunctionCall) visitAsFunctionCall();
        else if (astNode instanceof ASTIntegerConstant) visitAsIntegerConstant();
        else if (astNode instanceof ASTMemberAccess) visitAsMemberAccess();
        else if (astNode instanceof ASTPostfixExpression) visitAsPostfixExpression();
        else if (astNode instanceof ASTStringConstant) visitAsStringConstant();
        else if (astNode instanceof ASTUnaryExpression) visitAsUnaryExpression();
        else if (astNode instanceof ASTUnaryTypename) visitAsUnaryTypename();
    }

    private void visitAsExpressionStatement() {
        if (!(astNode instanceof ASTExpressionStatement)) return;
        ASTExpressionStatement expressionStatement = (ASTExpressionStatement) astNode;
        if (expressionStatement.exprs == null) return;
        for (ASTExpression expression : expressionStatement.exprs) {
            NodeInfo child = createChild(expression);
            child.visitAsExpression();
        }
    }

    private void visitAsFloatConstant() {
        // nothing need to do
    }

    private void visitAsFunctionCall() {
        if (!(astNode instanceof ASTFunctionCall)) return;
        ASTFunctionCall functionCall = (ASTFunctionCall) astNode;
        // function name
        NodeInfo exprChild = createChild(functionCall.funcname);
        exprChild.visitAsExpression();
        // if function name is identifier
        if (functionCall.funcname instanceof ASTIdentifier) {
            String name = ((ASTIdentifier) functionCall.funcname).value;
            Symbol functionDeclared = findSymbol(name);
            if (functionDeclared != null) {
                if (!(functionDeclared instanceof FuncSymbol)) {
                    errorHandler.handleErrorMsg(name + " is not a function");
                } else {
                    // check num and constant args
                    boolean match = true;
                    FuncSymbol funcSymbol = (FuncSymbol) functionDeclared;
                    if (functionCall.argList == null) {
                        functionCall.argList = new ArrayList<>();
                    }
                    if (functionCall.argList.size() != funcSymbol.getParams().size()) {
                        match = false;
                    } else {
                        // check constant args
                        for (int i = 0; i < functionCall.argList.size(); i++) {
                            ASTExpression argInput = functionCall.argList.get(i);
                            Specifier specifierRequired = funcSymbol.getSpecifier();
                            if (argInput instanceof ASTIntegerConstant) {
                                if (!specifierRequired.checkIntConstant()) {
                                    match = false;
                                    break;
                                }
                            } else if (argInput instanceof ASTFloatConstant) {
                                if (!specifierRequired.checkFloatConstant()) {
                                    match = false;
                                    break;
                                }
                            } else if (argInput instanceof ASTCharConstant) {
                                if (!specifierRequired.checkCharConstant()) {
                                    match = false;
                                    break;
                                }
                            }
                        }
                        // check expressions
                        for (ASTExpression expression : functionCall.argList) {
                            NodeInfo child = createChild(expression);
                            child.visitAsExpression();
                        }
                    }
                    if (!match) {
                        errorHandler.funcArgNotMatch(funcSymbol.getIdentifier());
                    }
                }
            }
        }

    }

    private void visitAsGotoStatement() {
        // todo: goto label
    }

    private void visitAsIdentifier() {
        if (!(astNode instanceof ASTIdentifier)) return;
        String name = ((ASTIdentifier) astNode).value;
        if (findSymbol(name) == null) {
            errorHandler.notDefined(name);
        }
    }

    private void visitAsInitList() {
        if (!(astNode instanceof ASTInitList)) return;
        ASTInitList initList = (ASTInitList)astNode;
        // declarator
        NodeInfo declaratorChild = createChild(initList.declarator);
        declaratorChild.isGlobal = this.isGlobal;
        declaratorChild.visitAsDeclarator();
        this.symbolTable.mergeChildTable(declaratorChild.symbolTable, true);
        // expressions
        if (initList.exprs != null) {
            for (ASTExpression expression : initList.exprs) {
                NodeInfo child = createChild(expression);
                child.visitAsExpression();
            }
        }
    }

    private void visitAsIntegerConstant() {
        // nothing need to do
    }

    private void visitAsIterationDeclaredStatement() {
        if (!(astNode instanceof ASTIterationDeclaredStatement)) return;
        ASTIterationDeclaredStatement iterationDeclaredStatement = (ASTIterationDeclaredStatement) astNode;
        if (iterationDeclaredStatement.init != null) {
            NodeInfo child = createChild(iterationDeclaredStatement.init);
            child.visitAsDeclaration();
            this.symbolTable.mergeChildTable(child.symbolTable, false);
        }
        if (iterationDeclaredStatement.cond != null) {
            for (ASTExpression expression : iterationDeclaredStatement.cond) {
                NodeInfo child = createChild(expression);
                child.visitAsExpression();
            }
        }
        if (iterationDeclaredStatement.step != null) {
            for (ASTExpression expression : iterationDeclaredStatement.step) {
                NodeInfo child = createChild(expression);
                child.visitAsExpression();
            }
        }
        if (iterationDeclaredStatement.stat != null) {
            NodeInfo child = createChild(iterationDeclaredStatement.stat);
            child.inIteration = true;
            child.visitAsStatement();
        }
    }

    private void visitAsIterationStatement() {
        if (!(astNode instanceof ASTIterationStatement)) return;
        ASTIterationStatement iterationStatement = (ASTIterationStatement) astNode;
        if (iterationStatement.init != null) {
            for (ASTExpression expression : iterationStatement.init) {
                NodeInfo child = createChild(expression);
                child.visitAsExpression();
            }
        }
        if (iterationStatement.cond != null) {
            for (ASTExpression expression : iterationStatement.cond) {
                NodeInfo child = createChild(expression);
                child.visitAsExpression();
            }
        }
        if (iterationStatement.step != null) {
            for (ASTExpression expression : iterationStatement.step) {
                NodeInfo child = createChild(expression);
                child.visitAsExpression();
            }
        }
        if (iterationStatement.stat != null) {
            NodeInfo child = createChild(iterationStatement.stat);
            child.inIteration = true;
            child.visitAsStatement();
        }
    }

    private void visitAsLabeledStatement() {
        if (!(astNode instanceof  ASTLabeledStatement)) return;
        // todo: goto label
        NodeInfo child = createChild(((ASTLabeledStatement) astNode).stat);
        child.inIteration = this.inIteration;
        child.visitAsStatement();
        this.hasReturn = child.hasReturn;
        this.symbolTable.mergeChildTable(child.symbolTable, true);
    }

    private void visitAsMemberAccess() {
        if (!(astNode instanceof ASTMemberAccess)) return;
        ASTMemberAccess memberAccess = (ASTMemberAccess) astNode;
        NodeInfo expressionChild = createChild(memberAccess.master);
        expressionChild.visitAsExpression();
        NodeInfo identifierChild = createChild(memberAccess.member);
        identifierChild.visitAsIdentifier();
    }

    private void visitAsPostfixExpression() {
        if (!(astNode instanceof ASTPostfixExpression)) return;
        NodeInfo child = createChild(((ASTPostfixExpression) astNode).expr);
        child.visitAsExpression();
    }

    private void visitAsReturnStatement() {
        if (!(astNode instanceof ASTReturnStatement)) return;
        this.hasReturn = true;
        if (((ASTReturnStatement) astNode).expr == null) {
            return;
        }
        for (ASTExpression expression : ((ASTReturnStatement) astNode).expr) {
            NodeInfo child = createChild(expression);
            child.visitAsExpression();
        }
    }

    private void visitAsSelectionStatement() {
        if (!(astNode instanceof ASTSelectionStatement)) return;
        ASTSelectionStatement selectionStatement = (ASTSelectionStatement) astNode;
        for (ASTExpression expression : selectionStatement.cond) {
            NodeInfo child = createChild(expression);
            child.visitAsExpression();
        }
        NodeInfo thenChild = createChild(selectionStatement.then);
        thenChild.inIteration = this.inIteration;
        thenChild.visitAsStatement();

        if (selectionStatement.otherwise != null) {
            NodeInfo otherwiseChild = createChild(selectionStatement.otherwise);
            otherwiseChild.inIteration = this.inIteration;
            otherwiseChild.visitAsStatement();
            // has return when every selection has return
            if (thenChild.hasReturn && otherwiseChild.hasReturn) {
                this.hasReturn = true;
            }
        } else {
            this.hasReturn = false;
        }
    }

    private void visitAsStringConstant() {
        // nothing need to do
    }

    private void visitAsTypename() {
        // nothing need to do
    }

    private void visitAsUnaryExpression() {
        if (!(astNode instanceof  ASTUnaryExpression)) return;
        NodeInfo child = createChild(((ASTUnaryExpression) astNode).expr);
        child.visitAsExpression();
    }

    private void visitAsUnaryTypename() {
        // nothing need to do
    }

    private void visitAsFunctionDefine() {
        // if already declared, check type
        // if not declared, add to symbol table
        if (!(astNode instanceof ASTFunctionDefine)) return;
        ASTFunctionDefine functionDefine = (ASTFunctionDefine) astNode;
        // specifier
        Specifier specifier = new Specifier();
        for (ASTToken specifierToken : functionDefine.specifiers) {
            specifier.addSpecifier(specifierToken.value);
        }
        // declarator
        if (!(functionDefine.declarator instanceof ASTFunctionDeclarator)) {
            errorHandler.handleErrorMsg("Not a valid function define");
            return;
        }
        NodeInfo declaratorChild = createChild(functionDefine.declarator);
        declaratorChild.isGlobal = this.isGlobal;
        declaratorChild.visitAsFunctionDeclarator();
        if (declaratorChild.symbolTable.getSymbols().isEmpty()) {
            return;
        }
        FuncSymbol funcSymbol = (FuncSymbol) declaratorChild.symbolTable.getSymbols().get(0);
        Symbol declaredSymbol = findSymbol(funcSymbol.getIdentifier());
        if (declaredSymbol != null) {
            // found same name
            if (declaredSymbol instanceof FuncSymbol) {
                if (!((FuncSymbol) declaredSymbol).checkParams(funcSymbol)
                        || !specifier.equalsSpecifier(declaredSymbol.getSpecifier())) {
                    errorHandler.handleErrorMsg("FunctionDefine not match declaration");
                } else {
                    if (((FuncSymbol) declaredSymbol).isDefined()) {
                        errorHandler.duplicateDeclaration(funcSymbol.getIdentifier());
                    } else {
                        ((FuncSymbol) declaredSymbol).setDefined(true);
                    }
                }
            } else {
                errorHandler.duplicateDeclaration(funcSymbol.getIdentifier());
            }
            funcSymbol.setSpecifier(specifier);
        } else {
            funcSymbol.setSpecifier(specifier);
            funcSymbol.setDefined(true);
            funcSymbol.setUp(true);
            addSymbol(funcSymbol);
        }
        // add named parameter to symbol table
        for (Symbol s : funcSymbol.getParams()) {
            if (!s.hasIdentifier()) {
                continue;
            }
            if (s.getIdentifier().equals(funcSymbol.getIdentifier())) {
                errorHandler.duplicateDeclaration(s.getIdentifier());
                continue;
            }
            s = s.copy();
            s.setUp(false);
            addSymbol(s);
        }
        // body
        if (functionDefine.body == null) {
            return;
        }
        NodeInfo bodyChild = createChild(functionDefine.body);
        bodyChild.visitAsCompoundStatement();
        // return check
        if (funcSymbol.needReturn() && !bodyChild.hasReturn) {
            errorHandler.funcNotReturn(funcSymbol.getIdentifier());
        }
    }

    private void visitAsDeclarator() {
        if (astNode instanceof ASTArrayDeclarator) {
            visitAsArrayDeclarator();
        } else if (astNode instanceof ASTVariableDeclarator) {
            visitAsVariableDeclarator();
        } else if (astNode instanceof ASTFunctionDeclarator) {
            visitAsFunctionDeclarator();
        }
    }

    private void visitAsStatement() {
        if (astNode instanceof ASTBreakStatement) visitAsBreakStatement();
        else if (astNode instanceof ASTCompoundStatement) visitAsCompoundStatement();
        else if (astNode instanceof ASTContinueStatement) visitAsContinueStatement();
        else if (astNode instanceof ASTExpressionStatement) visitAsExpressionStatement();
        else if (astNode instanceof ASTGotoStatement) visitAsGotoStatement();
        else if (astNode instanceof ASTIterationDeclaredStatement) visitAsIterationDeclaredStatement();
        else if (astNode instanceof ASTIterationStatement) visitAsIterationStatement();
        else if (astNode instanceof ASTLabeledStatement) visitAsLabeledStatement();
        else if (astNode instanceof ASTReturnStatement) visitAsReturnStatement();
        else if (astNode instanceof ASTSelectionStatement) visitAsSelectionStatement();
    }

    private void visitAsToken() {
        // nothing need to do
    }
}
