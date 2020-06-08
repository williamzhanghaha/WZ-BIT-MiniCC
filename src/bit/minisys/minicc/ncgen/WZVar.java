package bit.minisys.minicc.ncgen;

import bit.minisys.minicc.semantic.symbol.Symbol;

import java.util.ArrayList;
import java.util.List;

public class WZVar {
    enum VAR_TYPE {
        ARG,
        RET_ADD,
        LOCAL,
        TEMP
    }

    private VAR_TYPE type;
    private Symbol symbol;
    private int size;

    private boolean isActiveInMem;
    private List<WZReg> activeRegs = new ArrayList<>();

    public WZVar(VAR_TYPE type, Symbol symbol, boolean isActiveInMem) {
        this.type = type;
        this.symbol = symbol;
        this.isActiveInMem = isActiveInMem;
    }

    public void setActiveInMem(boolean activeInMem) {
        isActiveInMem = activeInMem;
    }

    public boolean isActiveInMem() {
        return isActiveInMem;
    }

    public void addActiveReg(WZReg reg) {
        activeRegs.add(reg);
    }

    public void moveBackToMem() {
        for (WZReg reg : activeRegs) {
            reg.setVar(null);
        }
        activeRegs.clear();
        isActiveInMem = true;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public VAR_TYPE getType() {
        return type;
    }

    public List<WZReg> getActiveRegs() {
        return activeRegs;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
