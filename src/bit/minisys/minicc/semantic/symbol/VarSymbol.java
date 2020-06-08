package bit.minisys.minicc.semantic.symbol;

public class VarSymbol extends Symbol {
    public VarSymbol(String identifier) {
        this.identifier = identifier;
    }

    public VarSymbol(VarSymbol s) {
        this.identifier = s.identifier;
    }
}
