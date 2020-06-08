package bit.minisys.minicc.icgen;

import bit.minisys.minicc.semantic.symbol.Symbol;

public class TmpValSymbol extends Symbol {
    private final int tmpId;
    public TmpValSymbol(int id) {
        this.tmpId = id;
        this.identifier = "#" + id;
    }
}
