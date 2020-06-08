package bit.minisys.minicc.icgen;

import bit.minisys.minicc.parser.ast.ASTCompilationUnit;
import bit.minisys.minicc.parser.ast.ASTFunctionDefine;
import bit.minisys.minicc.parser.ast.ASTNode;
import bit.minisys.minicc.semantic.symbol.FuncSymbol;
import bit.minisys.minicc.semantic.symbol.Symbol;

import java.util.*;

public class FuncInfo {
    // global calculation like
    // int a = 0;        <- assign default value
    // int b = a + 1;    <- assign default value
    // int main () {}
    // will be treat as in a function
    private boolean isGlobal = false;

    private ASTNode astNode;

    // symbol table of current function
    private final List<Symbol> symbolTable = new ArrayList<>();
    private final Map<String, Symbol> symbolMap = new HashMap<>();
    private final Map<String, Integer> argOrder = new HashMap<>();
    private final List<Symbol> args = new LinkedList<>();

    // function symbol
    private FuncSymbol symbol = null;

    // quats
    private final List<WZQuat> quats = new LinkedList<>();

    public FuncInfo(ASTCompilationUnit astNode) {
        isGlobal = true;
        this.astNode = astNode;
        this.symbolTable.addAll(astNode.info.getSymbolTable().getSymbols());
        for (Symbol s : symbolTable) {
            symbolMap.put(s.getIdentifier(), s);
        }
    }

    public FuncInfo(ASTFunctionDefine astNode) {
        this.astNode = astNode;
        int i = 0;
        for (Symbol s : astNode.info.getSymbolTable().getSymbols()) {
            if (!(s instanceof FuncSymbol)) {
                symbolTable.add(s);
                argOrder.put(s.getIdentifier(), i++);
                args.add(s);
            } else if (symbol == null) {
                symbol = (FuncSymbol) s;
            }
        }
        symbolTable.addAll(astNode.body.info.getSymbolTable().getSymbols());
        for (Symbol s : symbolTable) {
            symbolMap.put(s.getIdentifier(), s);
        }
    }

    public FuncSymbol getFuncSymbol() {
        return symbol;
    }

    public List<Symbol> getSymbolTable() {
        return symbolTable;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    public void addQuat(WZQuat quat) {
        quats.add(quat);
    }

    public Symbol getSymbol(String name) {
        return symbolMap.getOrDefault(name, null);
    }

    public void addSymbol(Symbol symbol) {
        symbolTable.add(symbol);
        symbolMap.put(symbol.getIdentifier(), symbol);
    }

    public String toString() {
        StringBuilder output = new StringBuilder();
        if (!isGlobal) {
            output.append("Func: ").append(symbol.getIdentifier()).append("\n");

            output.append("return: ");
            for (String specifier : symbol.getSpecifier().getSpecifers()) {
                output.append(specifier).append(" ");
            }
            output.append("\n");

            output.append("params: ");
            for (Symbol symbol: args) {
                output.append(symbol.getIdentifier()).append(" ");
            }
            output.append("\nContent {\n");

            for (WZQuat quat : quats) {
                if (quat.getOp() != WZQuat.WZ_QUAT_OP.LABEL) {
                    output.append("    ").append(quat).append("\n");
                } else {
                    output.append(quat).append("\n");
                }
            }
            output.append("}\n");
        } else {
            for (WZQuat quat : quats) {
                output.append(quat).append("\n");
            }
        }
        return new String(output);
    }

    public List<WZQuat> getQuats() {
        return quats;
    }
}
