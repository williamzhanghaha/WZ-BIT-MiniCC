package bit.minisys.minicc.semantic.symbol;

public class Symbol {
    // should be passed to upper node
    protected boolean up = false;

    // identifier value
    protected String identifier = null;

    // specifier
    protected Specifier specifier = null;

    // copy symbol and set up false
    public Symbol copy() {
        Symbol newSymbol;
        if (this instanceof VarSymbol) {
            newSymbol = new VarSymbol((VarSymbol) this);
        } else if (this instanceof ArraySymbol) {
            newSymbol = new ArraySymbol((ArraySymbol) this);
        } else if (this instanceof FuncSymbol) {
            newSymbol = new FuncSymbol((FuncSymbol) this);
        } else {
            newSymbol = new Symbol();
            newSymbol.identifier = this.identifier;
        }
        newSymbol.specifier = this.specifier;
        return newSymbol;
    }

    public void setUp(boolean up) {
        this.up = up;
    }

    public boolean isUpNeed() {
        return up;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Specifier getSpecifier() {
        return specifier;
    }

    public void setSpecifier(Specifier specifier) {
        this.specifier = specifier;
    }

    public boolean hasSpecifier() {
        return specifier != null;
    }

    public boolean hasIdentifier() {
        return identifier != null;
    }
}
