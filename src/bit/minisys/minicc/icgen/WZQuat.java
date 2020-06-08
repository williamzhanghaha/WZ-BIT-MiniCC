package bit.minisys.minicc.icgen;

import bit.minisys.minicc.semantic.symbol.Symbol;

public class WZQuat {
    public enum WZ_QUAT_OP {

        LABEL, // (LABEL, , , @1): this is not a Quat but a label @1

        ASSIGN,  // =

        ADD,  // +
        MUL,  // *
        SUB,  // -
        DIV,  // /
        MOD,  // %

        // set result to 1 if true, 0 otherwise
        EQ,  // ==
        LT,  // <
        MT,  // >
        LET, // <=
        MET, // >=

        AND, // &&
        OR,  // ||

        JMP,  // (JMP, , , @1): jump to @1
//        JE,   // (JE, a, b, @1): jump to @1 if a == b
//        JNE,  // (JNE, a, b, @1): jump to @1 if a != b
        JF,   // (JF, a, , @1): jump to @1 if a == 0;

        RET,  // return result (result may be null)

        ARG,  // func arg is in result
        CALL, // call result
        GRV,  // get return value to result
    }

    private WZ_QUAT_OP op;
    private Symbol opnd1;
    private Symbol opnd2;
    private Symbol result;

    public WZQuat(WZ_QUAT_OP op, Symbol opnd1, Symbol opnd2, Symbol result) {
        this.op = op;
        this.opnd1 = opnd1;
        this.opnd2 = opnd2;
        this.result = result;
    }

    @Override
    public String toString() {
        if (op == WZ_QUAT_OP.LABEL) {
            return result.getIdentifier() + ":";
        }
        String str = "(" + op + ", ";
        if (opnd1 != null) {
            str += opnd1.getIdentifier();
        }
        str += ", ";
        if (opnd2 != null) {
            str += opnd2.getIdentifier();
        }
        str += ", ";
        if (result != null) {
            str += result.getIdentifier();
        }
        str += ")";
        return str;
    }

    public WZ_QUAT_OP getOp() {
        return op;
    }

    public void setOp(WZ_QUAT_OP op) {
        this.op = op;
    }

    public Symbol getOpnd1() {
        return opnd1;
    }

    public void setOpnd1(Symbol opnd1) {
        this.opnd1 = opnd1;
    }

    public Symbol getOpnd2() {
        return opnd2;
    }

    public void setOpnd2(Symbol opnd2) {
        this.opnd2 = opnd2;
    }

    public Symbol getResult() {
        return result;
    }

    public void setResult(Symbol result) {
        this.result = result;
    }
}
