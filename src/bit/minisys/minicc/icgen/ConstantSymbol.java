package bit.minisys.minicc.icgen;

import bit.minisys.minicc.parser.ast.*;
import bit.minisys.minicc.semantic.symbol.Symbol;

public class ConstantSymbol extends Symbol {
    public enum CONST_TYPE {
        INT,
        FLOAT,
        CHAR,
        STRING
    }
    private ASTNode astNode;
    private final CONST_TYPE type;

    // for int constant
    private int intVal;

    public ConstantSymbol(ASTIntegerConstant astNode) {
        this.astNode = astNode;
        this.identifier = "" + astNode.value;
        type = CONST_TYPE.INT;
        intVal = astNode.value;
    }

    public ConstantSymbol(int val) {
        astNode = null;
        identifier = "" + val;
        type = CONST_TYPE.INT;
        intVal = val;
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

    public int getIntVal() {
        return intVal;
    }
}
