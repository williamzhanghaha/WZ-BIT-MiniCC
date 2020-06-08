package bit.minisys.minicc.icgen;

import bit.minisys.minicc.semantic.symbol.Symbol;

public class LabelSymbol extends Symbol {
    private int id;
    public LabelSymbol(int id) {
        this.id = id;
        this.identifier = "@" + id;
    }

    public int getId() {
        return id;
    }
}
