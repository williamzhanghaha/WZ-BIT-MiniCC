package bit.minisys.minicc.ncgen;

import bit.minisys.minicc.icgen.FuncInfo;
import bit.minisys.minicc.icgen.TmpValSymbol;
import bit.minisys.minicc.semantic.symbol.Symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WZStackFrame {
    // frame content
    private final List<WZVar> args = new ArrayList<>();
    private WZVar retAddr;
    private final List<WZVar> localVars = new ArrayList<>();
    private final List<WZVar> tmpVars = new ArrayList<>();

    private final Map<Symbol, WZVar> varMap = new HashMap<>();

    // var offset from $sp
    private final Map<WZVar, Integer> varOffset = new HashMap<>();

    // total size
    private int size = 0;

    private final FuncInfo funcInfo;
    private final WZRegManager regManager;

    public WZStackFrame(FuncInfo funcInfo, WZRegManager regManager) {
        this.funcInfo = funcInfo;
        this.regManager = regManager;
        retAddr = new WZVar(WZVar.VAR_TYPE.RET_ADD, null, false);
        retAddr.setSize(4);

        // init vars
        int numOfArgs = funcInfo.getFuncSymbol().getParams().size();
        List<Symbol> symbolTable = funcInfo.getSymbolTable();
        for (int i = 0; i < symbolTable.size(); i++) {
            Symbol symbol = symbolTable.get(i);
            if (i < numOfArgs) {
                // is arg
                WZVar arg = new WZVar(WZVar.VAR_TYPE.ARG, symbol, i >= 4);
                if (i <= 3) {
                    // in reg
                    WZReg argReg = regManager.getRegForArg(i);
                    argReg.setVar(arg);
                    arg.addActiveReg(argReg);
                }
                // todo: specific item size
                // here we assert size of type is 4
                arg.setSize(4);
                args.add(arg);
                varMap.put(symbol, arg);
            } else if (symbol instanceof TmpValSymbol) {
                WZVar tmpVar = new WZVar(WZVar.VAR_TYPE.TEMP, symbol, true);
                // todo: specific item size
                tmpVar.setSize(4);
                tmpVars.add(tmpVar);
                varMap.put(symbol, tmpVar);
            } else {
                WZVar localVar = new WZVar(WZVar.VAR_TYPE.LOCAL, symbol, true);
                // todo: specific item size
                localVar.setSize(4);
                localVars.add(localVar);
                varMap.put(symbol, localVar);
            }
        }

        // calc var offset
        for (int i = tmpVars.size() - 1; i >= 0; i--) {
            WZVar var = tmpVars.get(i);
            varOffset.put(var, size);
            size += var.getSize();
        }
        for (int i = localVars.size() - 1; i >= 0; i--) {
            WZVar var = localVars.get(i);
            varOffset.put(var, size);
            size += var.getSize();
        }
        varOffset.put(retAddr, size);
        size += retAddr.getSize();
        for (int i = args.size() - 1; i >= 0; i--) {
            WZVar var = args.get(i);
            varOffset.put(var, size);
            size += var.getSize();
        }
    }

    public int getSize() {
        return size;
    }

    public WZVar getWZVarFromSymbol(Symbol symbol) {
        return varMap.get(symbol);
    }

    public int getOffset(WZVar var) {
        return varOffset.get(var);
    }

    public int getOffset(Symbol symbol) {
        return varOffset.get(varMap.get(symbol));
    }

    public int getRaOffset() {
        return varOffset.get(retAddr);
    }
}
