package bit.minisys.minicc.semantic.symbol;

import java.util.ArrayList;
import java.util.List;

public class FuncSymbol extends Symbol {
    List<Symbol> params = new ArrayList<>();
    boolean defined = false;

    public FuncSymbol(String identifier, List<Symbol> params) {
        this.identifier = identifier;
        this.params.addAll(params);
    }

    public FuncSymbol(FuncSymbol s) {
        this.identifier = s.identifier;
        this.params = s.params;
        this.defined = s.defined;
    }

    public boolean isDefined() {
        return defined;
    }

    public void setDefined(boolean defined) {
        this.defined = defined;
    }

    public boolean checkParams(FuncSymbol funcSymbol) {
        return checkParams(funcSymbol.params);
    }

    public boolean checkParams(List<Symbol> params) {
        if (this.params.size() != params.size()) {
            return false;
        }

        for (int i = 0; i < this.params.size(); i++) {
            Symbol declaredSymbol = this.params.get(i);
            Symbol inputSymbol = params.get(i);
            if (!checkParam(declaredSymbol, inputSymbol)) {
                return false;
            }
        }

        return true;
    }

    private boolean checkParam(Symbol a, Symbol b) {
        if (a instanceof ArraySymbol) {
            if (!(b instanceof ArraySymbol)) {
                return false;
            }
            // both are array symbol
            if (((ArraySymbol) a).getSize().size() != ((ArraySymbol) b).getSize().size()) {
                return false;
            }
        }
        if (!a.getSpecifier().equalsSpecifier(b.getSpecifier())) {
            return false;
        }
        return true;
    }

    public boolean needReturn() {
        if (specifier != null) {
            return specifier.getSpecifers().size() != 1
                    || !specifier.getSpecifers().get(0).equals("void");
        }
        return false;
    }

    public List<Symbol> getParams() {
        return params;
    }
}
