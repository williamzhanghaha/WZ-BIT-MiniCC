package bit.minisys.minicc.icgen;

public class WZTmpValueGenerator {
    private int currentId = 0;
    public TmpValSymbol genNewTmpVal() {
        TmpValSymbol symbol = new TmpValSymbol(currentId);
        currentId++;
        return symbol;
    }
}
