package bit.minisys.minicc.semantic;

import bit.minisys.minicc.semantic.symbol.Specifier;
import bit.minisys.minicc.semantic.symbol.Symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SymbolTable {
    private final List<Symbol> symbols = new ArrayList<>();
    // map for (symbol identifier value : symbol) for named symbol only
    private final Map<String, Symbol> namedSymbolMap = new HashMap<>();

    public List<Symbol> getSymbols() {
        return symbols;
    }

    public boolean addSymbol(Symbol symbol) {
        if (symbol.hasIdentifier()) {
            if (namedSymbolMap.containsKey(symbol.getIdentifier())) {
                // duplicate declaration
                return false;
            }
            namedSymbolMap.put(symbol.getIdentifier(), symbol);
        }
        this.symbols.add(symbol);
        return true;
    }

    // add child's up-needed symbol
    public void mergeChildTable(SymbolTable child, boolean up) {
        for (Symbol symbol : child.symbols) {
            if (symbol.isUpNeed()) {
                Symbol s = symbol.copy();
                s.setUp(up);
                this.addSymbol(s);
            }
        }
    }

    // add specifier and merge child table
    public void mergeChildTableWithSpecifier(Specifier specifier, SymbolTable child, boolean up) {
        for (Symbol symbol : child.symbols) {
            if (symbol.isUpNeed()) {
                Symbol symbolWithSpecifier = symbol.copy();
                symbolWithSpecifier.setSpecifier(specifier);
                symbolWithSpecifier.setUp(up);
                this.addSymbol(symbolWithSpecifier);
            }
        }
    }

    public Symbol getSymbol(String identifier) {
        return namedSymbolMap.getOrDefault(identifier, null);
    }
}
