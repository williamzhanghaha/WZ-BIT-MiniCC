package bit.minisys.minicc.icgen;

import bit.minisys.minicc.parser.ast.*;
import bit.minisys.minicc.semantic.symbol.Symbol;

public class ConstantSymbol extends Symbol {
    enum CONST_TYPE {
        INT,
        FLOAT,
        CHAR,
        STRING
    }
    private ASTNode astNode;
    private final CONST_TYPE type;

    public ConstantSymbol(ASTIntegerConstant astNode) {
        this.astNode = astNode;
        this.identifier = "" + astNode.value;
        type = CONST_TYPE.INT;
    }

    public ConstantSymbol(int val) {
        astNode = null;
        identifier = "" + val;
        type = CONST_TYPE.INT;
    }

    public ConstantSymbol(ASTCharConstant astNode) {
        this.astNode = astNode;
        this.identifier = astNode.value;
        type = CONST_TYPE.CHAR;
    }

    public ConstantSymbol(ASTFloatConstant astNode) {
        this.astNode = astNode;
        this.identifier = "" + astNode.value;
        type = CONST_TYPE.FLOAT;
    }

    public ConstantSymbol(ASTStringConstant astNode) {
        this.astNode = astNode;
        this.identifier = astNode.value;
        type = CONST_TYPE.STRING;
    }

    public CONST_TYPE getType() {
        return type;
    }
}
