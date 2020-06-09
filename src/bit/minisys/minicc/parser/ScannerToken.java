package bit.minisys.minicc.parser;

public class ScannerToken {
    public String lexme;
    public String type;
    public int	  line;
    public int    column;

    public boolean isSpecifier() {
        return type.equals("'void'") ||
                type.equals("'char'") ||
                type.equals("'short'") ||
                type.equals("'int'") ||
                type.equals("'long'") ||
                type.equals("'float'") ||
                type.equals("'double'") ||
                type.equals("'signed'") ||
                type.equals("'unsigned'");
    }

    public boolean isAssignmentOperator() {
        return type.equals("'='") ||
                type.equals("'*='") ||
                type.equals("'/='") ||
                type.equals("'%='") ||
                type.equals("'+='") ||
                type.equals("'-='") ||
                type.equals("'<<='") ||
                type.equals("'>>='") ||
                type.equals("'&='");
    }

    public boolean isUnaryOperator() {
        return type.equals("'&'") ||
                type.equals("'*'") ||
                type.equals("'+'") ||
                type.equals("'-'") ||
                type.equals("'~'") ||
                type.equals("'!'");
    }
}
