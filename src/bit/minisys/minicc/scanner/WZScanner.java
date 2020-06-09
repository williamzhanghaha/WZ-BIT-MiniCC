package bit.minisys.minicc.scanner;

import bit.minisys.minicc.MiniCCCfg;
import bit.minisys.minicc.internal.util.MiniCCUtil;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;

public class WZScanner implements IMiniCCScanner {
    enum SCANNER_STATE {
        INITIAL,
        DIGIT_0,
        OCT_SEQ,
        FLOAT_DEC_SEQ,
        HEX_PRE,
        INT_U,
        INT_l,
        INT_L,
        INT_U_l,
        INT_U_L,
        INT_LL,
        FLOAT_DEC_FRAC,
        FLOAT_E,
        FLOAT_E_SIGN,
        FLOAT_E_NUM,
        DEC_SEQ,
        HEX_SEQ,
        FLOAT_HEX_FRAC,
        DOT,
        ALPHA_u,
        ALPHA_U_OR_L,
        ALPHA_u8,
        CHAR_QUOTE_START,
        CHAR_CHAR,
        CHAR_ESCAPE,
        STRING_QUOTE_START,
        STRING_ESCAPE,
        ID_OR_KW,
        OTHER_CHAR,
        PUN_AND_PRE,
        PRE_PUN,
        END,
        ERROR
    }

    private HashSet<String> keywordSet = new HashSet<>();
    private HashSet<String> punctuatorSet = new HashSet<>();
    private HashSet<String> prePunctuatorSet = new HashSet<>();

    private String strTokens = "";   // tokens generated
    private byte[] fileContent = null;  // file content in bytes
    private int fileLoc = -1;  // current char location in file
    private int tokenID = -1;  // current generated token num
    private int lineNum = 0;   // current line num
    private int colNum = -1;   // current col num
    private int tokenFileLoc = -1;  // current token start loc in file
    private int tokenLineNum = 0;   // current token start line num
    private int tokenColNum = -1;   // current token start col num
    private boolean startNewLine = true; // '\n' found in last scan


    private void readFile(String filePath) {
        File file = new File(filePath);
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            fileContent = new byte[(int)file.length()];
            fileInputStream.read(fileContent);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        fileLoc = -1;
        tokenID = -1;
        lineNum = 0;
        colNum = -1;
        strTokens = "";
    }

    private char getNextChar() {
        fileLoc ++;
        if (fileContent == null || fileLoc >= fileContent.length) {
            colNum ++;
            return Character.MAX_VALUE;
        }
        byte b = fileContent[fileLoc];
        if (startNewLine) {
            lineNum ++;
            colNum = -1;
        }
        startNewLine = b == '\n';
        colNum ++;
        return (char)b;
    }

    private void genTokenStr(String content, String type) {
        tokenID ++;
        String token = "[@" + tokenID + ","
                + tokenFileLoc + ":" + (tokenFileLoc + content.length() - 1)
                + "='" + content + "',"
                + "<" + type + ">,"
                + tokenLineNum + ":" + tokenColNum + "]\n";
        strTokens += token;
    }

    private void genEOFToken() {
        tokenID ++;
        String token = "[@" + tokenID + ","
                + tokenFileLoc + ":" + (tokenFileLoc - 1)
                + "='" + "<EOF>" + "',"
                + "<" + "EOF" + ">,"
                + tokenLineNum + ":" + tokenColNum + "]\n";
        strTokens += token;
    }

    private void genConstToken(String content) {
        // todo: only for int constant
        genTokenStr(content, "IntegerConstant");
    }

    private void genStringLiteralToken(String content) {
        genTokenStr(content, "StringLiteral");
    }

    private void genIdToken(String content) {
        genTokenStr(content, "Identifier");
    }

    private void genToken(String content) {
        genTokenStr(content, "'" + content + "'");
    }

    private void startNewToken(int fileLoc, int lineNum, int colNum) {
        tokenFileLoc = fileLoc;
        tokenLineNum = lineNum;
        tokenColNum = colNum;
    }

    private void startNewToken() {
        startNewToken(fileLoc, lineNum, colNum);
    }

    private boolean isAlpha(char c) {
        if ('a' <= c && c <= 'z') {
            return true;
        } else if ('A' <= c && c <= 'Z') {
            return true;
        }
        return false;
    }

    private boolean isHexNum(char c) {
        if ('0' <= c && c <= '9') {
            return true;
        } else if ('A' <= c && c <= 'F') {
            return true;
        } else if ('a' <= c && c <= 'f') {
            return true;
        }
        return false;
    }

    private boolean isCChar(char c) {
        return c != '\'' && c != '\\' && c != '\n';
    }

    private boolean isSChar(char c) {
        return c != '"' && c != '\\' && c != '\n';
    }

    private boolean isValidAfterEscape(char c) {
        return c == '\'' || c == '"' || c == '?' || c == '\\'
                || c == 'a' || c == 'b' || c == 'f' || c == 'n'
                || c == 'r' || c == 't' || c == 'v' || c == 'x'
                || ('0' <= c && c <= '9');
    }

    public WZScanner() {
        // init keyword set
        keywordSet.add("auto");
        keywordSet.add("break");
        keywordSet.add("case");
        keywordSet.add("char");
        keywordSet.add("const");
        keywordSet.add("continue");
        keywordSet.add("default");
        keywordSet.add("do");
        keywordSet.add("double");
        keywordSet.add("else");
        keywordSet.add("enum");
        keywordSet.add("extern");
        keywordSet.add("float");
        keywordSet.add("for");
        keywordSet.add("goto");
        keywordSet.add("if");
        keywordSet.add("inline");
        keywordSet.add("int");
        keywordSet.add("long");
        keywordSet.add("register");
        keywordSet.add("restrict");
        keywordSet.add("return");
        keywordSet.add("short");
        keywordSet.add("signed");
        keywordSet.add("sizeof");
        keywordSet.add("static");
        keywordSet.add("struct");
        keywordSet.add("switch");
        keywordSet.add("typedef");
        keywordSet.add("union");
        keywordSet.add("unsigned");
        keywordSet.add("void");
        keywordSet.add("volatile");
        keywordSet.add("while");

        // init punctuator set
        punctuatorSet.add("[");
        punctuatorSet.add("]");
        punctuatorSet.add("(");
        punctuatorSet.add(")");
        punctuatorSet.add("{");
        punctuatorSet.add("}");
        punctuatorSet.add(".");
        punctuatorSet.add("->");
        punctuatorSet.add("++");
        punctuatorSet.add("--");
        punctuatorSet.add("&");
        punctuatorSet.add("*");
        punctuatorSet.add("+");
        punctuatorSet.add("-");
        punctuatorSet.add("~");
        punctuatorSet.add("!");
        punctuatorSet.add("/");
        punctuatorSet.add("%");
        punctuatorSet.add("<<");
        punctuatorSet.add(">>");
        punctuatorSet.add("<");
        punctuatorSet.add(">");
        punctuatorSet.add("<=");
        punctuatorSet.add(">=");
        punctuatorSet.add("==");
        punctuatorSet.add("!=");
        punctuatorSet.add("^");
        punctuatorSet.add("|");
        punctuatorSet.add("&&");
        punctuatorSet.add("||");
        punctuatorSet.add("?");
        punctuatorSet.add(":");
        punctuatorSet.add(";");
        punctuatorSet.add("...");
        punctuatorSet.add("=");
        punctuatorSet.add("*=");
        punctuatorSet.add("/=");
        punctuatorSet.add("%=");
        punctuatorSet.add("+=");
        punctuatorSet.add("-=");
        punctuatorSet.add("<<=");
        punctuatorSet.add(">>=");
        punctuatorSet.add("&=");
        punctuatorSet.add("^=");
        punctuatorSet.add("|=");
        punctuatorSet.add(",");
        punctuatorSet.add("#");
        punctuatorSet.add("##");
        punctuatorSet.add("<:");
        punctuatorSet.add(":>");
        punctuatorSet.add("<%");
        punctuatorSet.add("%>");
        punctuatorSet.add("%:");
        punctuatorSet.add("%:%:");

        // init pre punctuator set
        for (String p: punctuatorSet) {
            for (int i = 1; i < p.length(); i++) {
                String sub = p.substring(0, i);
                prePunctuatorSet.add(sub);
            }
        }
    }

    @Override
    public String run(String iFile) throws Exception {
        System.out.println("Scanning...");
        readFile(iFile);
        if (fileContent == null) {
            System.out.println("Cannot Read File");
            return null;
        }
        System.out.println("File Read");

        SCANNER_STATE state = SCANNER_STATE.INITIAL;  // current state
        boolean end = false;   // end loop
        boolean keep = false;  // do not read new char when true
        char c = ' ';          // current read char
        String lexeme = "";    // current lexeme (do not include current char)

        while(!end) {
            if (!keep) {
                c = getNextChar();
            }
            keep = false;

            switch (state) {
                case INITIAL:
                    startNewToken();
                    if (c == ' ' || c == '\n' || c == '\t' || c == '\r') {
                        break;
                    } else if (c == '0') {
                        state = SCANNER_STATE.DIGIT_0;
                        lexeme = lexeme + c;
                    } else if ('1' <= c && c <= '9') {
                        state = SCANNER_STATE.DEC_SEQ;
                        lexeme = lexeme + c;
                    } else if (c == 'u') {
                        state = SCANNER_STATE.ALPHA_u;
                        lexeme = lexeme + c;
                    } else if (c == 'U' || c == 'L') {
                        state = SCANNER_STATE.ALPHA_U_OR_L;
                        lexeme = lexeme + c;
                    } else if (c == '\'') {
                        state = SCANNER_STATE.CHAR_QUOTE_START;
                        lexeme = lexeme + c;
                    } else if (c == '"') {
                        state = SCANNER_STATE.STRING_QUOTE_START;
                        lexeme = lexeme + c;
                    } else if (c == '.') {
                        state = SCANNER_STATE.DOT;
                        lexeme = lexeme + c;
                    } else if (c == '_' || isAlpha(c)) {
                        state = SCANNER_STATE.ID_OR_KW;
                        lexeme = lexeme + c;
                    } else if (c == Character.MAX_VALUE) {
                        state = SCANNER_STATE.END;
                    } else {
                        state = SCANNER_STATE.OTHER_CHAR;
                        lexeme = lexeme + c;
                    }
                    break;
                case DIGIT_0:
                    if ('0' <= c && c <= '7') {
                        state = SCANNER_STATE.OCT_SEQ;
                        lexeme = lexeme + c;
                    } else if (c == '8' || c == '9') {
                        state = SCANNER_STATE.FLOAT_DEC_SEQ;
                        lexeme = lexeme + c;
                    } else if (c == 'x' || c == 'X') {
                        state = SCANNER_STATE.HEX_PRE;
                        lexeme = lexeme + c;
                    } else if (c == 'u' || c == 'U') {
                        state = SCANNER_STATE.INT_U;
                        lexeme = lexeme + c;
                    } else if (c == 'l') {
                        state = SCANNER_STATE.INT_l;
                        lexeme = lexeme + c;
                    } else if (c == 'L') {
                        state = SCANNER_STATE.INT_L;
                        lexeme = lexeme + c;
                    } else if (c == '.') {
                        state = SCANNER_STATE.FLOAT_DEC_FRAC;
                        lexeme = lexeme + c;
                    } else if (c == 'e' || c == 'E') {
                        state = SCANNER_STATE.FLOAT_E;
                        lexeme = lexeme + c;
                    } else {
                        state = SCANNER_STATE.INITIAL;
                        genConstToken(lexeme);
                        keep = true;
                        lexeme = "";
                    }
                    break;
                case OCT_SEQ:
                    if ('0' <= c && c <= '7') {
                        state = SCANNER_STATE.OCT_SEQ;
                        lexeme = lexeme + c;
                    } else if (c == '8' || c == '9') {
                        state = SCANNER_STATE.FLOAT_DEC_SEQ;
                        lexeme = lexeme + c;
                    } else if (c == '.') {
                        state = SCANNER_STATE.FLOAT_DEC_FRAC;
                        lexeme = lexeme + c;
                    } else if (c == 'e' || c == 'E') {
                        state = SCANNER_STATE.FLOAT_E;
                        lexeme = lexeme + c;
                    } else if (c == 'u' || c == 'U') {
                        state = SCANNER_STATE.INT_U;
                        lexeme = lexeme + c;
                    } else if (c == 'l') {
                        state = SCANNER_STATE.INT_l;
                        lexeme = lexeme + c;
                    } else if (c == 'L') {
                        state = SCANNER_STATE.INT_L;
                        lexeme = lexeme + c;
                    } else {
                        state = SCANNER_STATE.INITIAL;
                        genConstToken(lexeme);
                        keep = true;
                        lexeme = "";
                    }
                    break;
                case FLOAT_DEC_SEQ:
                    if ('0' <= c && c <= '9') {
                        state = SCANNER_STATE.FLOAT_DEC_SEQ;
                        lexeme = lexeme + c;
                    } else if (c == '.') {
                        state = SCANNER_STATE.FLOAT_DEC_FRAC;
                        lexeme = lexeme + c;
                    } else if (c == 'e' || c == 'E') {
                        state = SCANNER_STATE.FLOAT_E;
                        lexeme = lexeme + c;
                    } else {
                        state = SCANNER_STATE.ERROR;
                        keep = true;
                    }
                    break;
                case HEX_PRE:
                    if (isHexNum(c)) {
                        state = SCANNER_STATE.HEX_SEQ;
                        lexeme = lexeme + c;
                    } else {
                        state = SCANNER_STATE.ERROR;
                        keep = true;
                    }
                    break;
                case INT_U:
                    if (c == 'l') {
                        state = SCANNER_STATE.INT_U_l;
                        lexeme = lexeme + c;
                    } else if (c == 'L') {
                        state = SCANNER_STATE.INT_U_L;
                        lexeme = lexeme + c;
                    } else {
                        state = SCANNER_STATE.INITIAL;
                        genConstToken(lexeme);
                        keep = true;
                        lexeme = "";
                    }
                    break;
                case INT_l:
                    if (c == 'l') {
                        state = SCANNER_STATE.INT_LL;
                        lexeme = lexeme + c;
                    } else if (c == 'u' || c == 'U') {
                        state = SCANNER_STATE.INITIAL;
                        genConstToken(lexeme + c);
                        lexeme = "";
                    } else if (c == 'L') {
                        state = SCANNER_STATE.ERROR;
                        keep = true;
                    } else {
                        state = SCANNER_STATE.INITIAL;
                        genConstToken(lexeme);
                        keep = true;
                        lexeme = "";
                    }
                    break;
                case INT_L:
                    if (c == 'L') {
                        state = SCANNER_STATE.INT_LL;
                        lexeme = lexeme + c;
                    } else if (c == 'u' || c == 'U') {
                        state = SCANNER_STATE.INITIAL;
                        genConstToken(lexeme + c);
                        lexeme = "";
                    } else if (c == 'l') {
                        state = SCANNER_STATE.ERROR;
                        keep = true;
                    } else {
                        state = SCANNER_STATE.INITIAL;
                        genConstToken(lexeme);
                        keep = true;
                        lexeme = "";
                    }
                    break;
                case INT_U_l:
                    if (c == 'l') {
                        state = SCANNER_STATE.INITIAL;
                        genConstToken(lexeme + c);
                        lexeme = "";
                    } else if (c == 'L') {
                        state = SCANNER_STATE.ERROR;
                        keep = true;
                    } else {
                        state = SCANNER_STATE.INITIAL;
                        genConstToken(lexeme);
                        keep = true;
                        lexeme = "";
                    }
                    break;
                case INT_U_L:
                    if (c == 'L') {
                        state = SCANNER_STATE.INITIAL;
                        genConstToken(lexeme + c);
                        lexeme = "";
                    } else if (c == 'l') {
                        state = SCANNER_STATE.ERROR;
                        keep = true;
                    } else {
                        state = SCANNER_STATE.INITIAL;
                        genConstToken(lexeme);
                        keep = true;
                        lexeme = "";
                    }
                    break;
                case INT_LL:
                    if (c == 'u' || c == 'U') {
                        state = SCANNER_STATE.INITIAL;
                        genConstToken(lexeme + c);
                        lexeme = "";
                    } else {
                        state = SCANNER_STATE.INITIAL;
                        genConstToken(lexeme);
                        keep = true;
                        lexeme = "";
                    }
                    break;
                case FLOAT_DEC_FRAC:
                    if ('0' <= c && c <= '9') {
                        state = SCANNER_STATE.FLOAT_DEC_FRAC;
                        lexeme = lexeme + c;
                    } else if (c == 'e' || c == 'E') {
                        state = SCANNER_STATE.FLOAT_E;
                        lexeme = lexeme + c;
                    } else if (c == 'f' || c == 'F' || c == 'l' || c == 'L') {
                        state = SCANNER_STATE.INITIAL;
                        genConstToken(lexeme + c);
                        lexeme = "";
                    } else {
                        state = SCANNER_STATE.INITIAL;
                        genConstToken(lexeme);
                        keep = true;
                        lexeme = "";
                    }
                    break;
                case FLOAT_E:
                    if (c == '+' || c == '-') {
                        state = SCANNER_STATE.FLOAT_E_SIGN;
                        lexeme = lexeme + c;
                    } else if ('0' <= c && c <= '9') {
                        state = SCANNER_STATE.FLOAT_E_NUM;
                        lexeme = lexeme + c;
                    } else {
                        state = SCANNER_STATE.ERROR;
                        keep = true;
                    }
                    break;
                case FLOAT_E_SIGN:
                    if ('0' <= c && c <= '9') {
                        state = SCANNER_STATE.FLOAT_E_NUM;
                        lexeme = lexeme + c;
                    } else {
                        state = SCANNER_STATE.ERROR;
                        keep = true;
                    }
                    break;
                case FLOAT_E_NUM:
                    if ('0' <= c && c <= '9') {
                        state = SCANNER_STATE.FLOAT_E_NUM;
                        lexeme = lexeme + c;
                    } else if (c == 'f' || c == 'F' || c == 'l' || c == 'L') {
                        state = SCANNER_STATE.INITIAL;
                        genConstToken(lexeme + c);
                        lexeme = "";
                    } else {
                        state = SCANNER_STATE.INITIAL;
                        genConstToken(lexeme);
                        keep = true;
                        lexeme = "";
                    }
                    break;
                case DEC_SEQ:
                    if ('0' <= c && c <= '9') {
                        state = SCANNER_STATE.DEC_SEQ;
                        lexeme = lexeme + c;
                    } else if (c == '.') {
                        state = SCANNER_STATE.FLOAT_DEC_FRAC;
                        lexeme = lexeme + c;
                    } else if (c == 'e' || c == 'E') {
                        state = SCANNER_STATE.FLOAT_E;
                        lexeme = lexeme + c;
                    } else if (c == 'u' || c == 'U') {
                        state = SCANNER_STATE.INT_U;
                        lexeme = lexeme + c;
                    } else if (c == 'l') {
                        state = SCANNER_STATE.INT_l;
                        lexeme = lexeme + c;
                    } else if (c == 'L') {
                        state = SCANNER_STATE.INT_L;
                        lexeme = lexeme + c;
                    } else {
                        state = SCANNER_STATE.INITIAL;
                        genConstToken(lexeme);
                        keep = true;
                        lexeme = "";
                    }
                    break;
                case HEX_SEQ:
                    if (isHexNum(c)) {
                        state = SCANNER_STATE.HEX_SEQ;
                        lexeme = lexeme + c;
                    } else if (c == 'u' || c == 'U') {
                        state = SCANNER_STATE.INT_U;
                        lexeme = lexeme + c;
                    } else if (c == 'l') {
                        state = SCANNER_STATE.INT_l;
                        lexeme = lexeme + c;
                    } else if (c == 'L') {
                        state = SCANNER_STATE.INT_L;
                        lexeme = lexeme + c;
                    } else if (c == '.') {
                        state = SCANNER_STATE.FLOAT_HEX_FRAC;
                        lexeme = lexeme + c;
                    } else if (c == 'p' || c == 'P') {
                        state = SCANNER_STATE.FLOAT_E;
                        lexeme = lexeme + c;
                    } else if (c == 'f' || c == 'F') {
                        state = SCANNER_STATE.INITIAL;
                        genConstToken(lexeme + c);
                        lexeme = "";
                    } else {
                        state = SCANNER_STATE.INITIAL;
                        genConstToken(lexeme);
                        keep = true;
                        lexeme = "";
                    }
                    break;
                case FLOAT_HEX_FRAC:
                    if (isHexNum(c)) {
                        state = SCANNER_STATE.FLOAT_HEX_FRAC;
                        lexeme = lexeme + c;
                    } else if (c == 'p' || c == 'P') {
                        state = SCANNER_STATE.FLOAT_E;
                        lexeme = lexeme + c;
                    } else {
                        state = SCANNER_STATE.ERROR;
                        keep = true;
                    }
                    break;
                case DOT:
                    if ('0' <= c && c <= '9') {
                        state = SCANNER_STATE.FLOAT_DEC_FRAC;
                        lexeme = lexeme + c;
                    } else {
                        state = SCANNER_STATE.OTHER_CHAR;
                        keep = true;
                    }
                    break;
                case ALPHA_u:
                    if (c == '8') {
                        state = SCANNER_STATE.ALPHA_u8;
                        lexeme = lexeme + c;
                    } else if (c == '\'') {
                        state = SCANNER_STATE.CHAR_QUOTE_START;
                        lexeme = lexeme + c;
                    } else if (c == '"') {
                        state = SCANNER_STATE.STRING_QUOTE_START;
                        lexeme = lexeme + c;
                    } else {
                        state = SCANNER_STATE.ID_OR_KW;
                        keep = true;
                    }
                    break;
                case ALPHA_U_OR_L:
                    if (c == '\'') {
                        state = SCANNER_STATE.CHAR_QUOTE_START;
                        lexeme = lexeme + c;
                    } else if (c == '"') {
                        state = SCANNER_STATE.STRING_QUOTE_START;
                        lexeme = lexeme + c;
                    } else {
                        state = SCANNER_STATE.ID_OR_KW;
                        keep = true;
                    }
                    break;
                case ALPHA_u8:
                    if (c == '\'') {
                        state = SCANNER_STATE.ERROR;
                        keep = true;
                    } else if (c == '"') {
                        state = SCANNER_STATE.STRING_QUOTE_START;
                        lexeme = lexeme + c;
                    } else {
                        state = SCANNER_STATE.ID_OR_KW;
                        keep = true;
                    }
                    break;
                case CHAR_QUOTE_START:
                    if (isCChar(c)) {
                        state = SCANNER_STATE.CHAR_CHAR;
                        lexeme = lexeme + c;
                    } else if (c == '\\') {
                        state = SCANNER_STATE.CHAR_ESCAPE;
                        lexeme = lexeme + c;
                    } else {
                        state = SCANNER_STATE.ERROR;
                        keep = true;
                    }
                    break;
                case CHAR_CHAR:
                    if (c == '\'') {
                        state = SCANNER_STATE.INITIAL;
                        genConstToken(lexeme + c);
                        lexeme = "";
                    } else if (isCChar(c)) {
                        state = SCANNER_STATE.CHAR_CHAR;
                        lexeme = lexeme + c;
                    } else if (c == '\\') {
                        state = SCANNER_STATE.CHAR_ESCAPE;
                        lexeme = lexeme + c;
                    } else {
                        state = SCANNER_STATE.ERROR;
                        keep = true;
                    }
                    break;
                case CHAR_ESCAPE:
                    if (isValidAfterEscape(c)) {
                        state = SCANNER_STATE.CHAR_CHAR;
                        lexeme = lexeme + c;
                    } else {
                        state = SCANNER_STATE.ERROR;
                        keep = true;
                    }
                    break;
                case STRING_QUOTE_START:
                    if (isSChar(c)) {
                        state = SCANNER_STATE.STRING_QUOTE_START;
                        lexeme = lexeme + c;
                    } else if (c == '"') {
                        state = SCANNER_STATE.INITIAL;
                        genStringLiteralToken(lexeme + c);
                        lexeme = "";
                    } else if (c == '\\') {
                        state = SCANNER_STATE.STRING_ESCAPE;
                        lexeme = lexeme + c;
                    } else {
                        state = SCANNER_STATE.ERROR;
                        keep = true;
                    }
                    break;
                case STRING_ESCAPE:
                    if (isValidAfterEscape(c)) {
                        state = SCANNER_STATE.STRING_QUOTE_START;
                        lexeme = lexeme + c;
                    } else {
                        state = SCANNER_STATE.ERROR;
                        keep = true;
                    }
                    break;
                case ID_OR_KW:
                    if (c == '_' || isAlpha(c) || ('0' <= c && c <= '9')) {
                        state = SCANNER_STATE.ID_OR_KW;
                        lexeme = lexeme + c;
                    } else {
                        state = SCANNER_STATE.INITIAL;
                        if (keywordSet.contains(lexeme)) {
                            genToken(lexeme);
                        } else {
                            genIdToken(lexeme);
                        }
                        keep = true;
                        lexeme = "";
                    }
                    break;
                case OTHER_CHAR:
                    keep = true;
                    if (punctuatorSet.contains(lexeme) && prePunctuatorSet.contains(lexeme)) {
                        state = SCANNER_STATE.PUN_AND_PRE;
                    } else if (punctuatorSet.contains(lexeme)) {
                        state = SCANNER_STATE.INITIAL;
                        genToken(lexeme);
                        lexeme = "";
                    } else if (prePunctuatorSet.contains(lexeme)) {
                        state = SCANNER_STATE.PRE_PUN;
                    } else {
                        state = SCANNER_STATE.ERROR;
                    }
                    break;
                case PUN_AND_PRE:
                    String s = lexeme + c;
                    if (punctuatorSet.contains(s) && prePunctuatorSet.contains(s)) {
                        state = SCANNER_STATE.PUN_AND_PRE;
                        lexeme = s;
                    } else if (punctuatorSet.contains(s)) {
                        state = SCANNER_STATE.INITIAL;
                        genToken(s);
                        lexeme = "";
                    } else if (prePunctuatorSet.contains(s)) {
                        state = SCANNER_STATE.PRE_PUN;
                        lexeme = s;
                    } else {
                        state = SCANNER_STATE.INITIAL;
                        genToken(lexeme);
                        keep = true;
                        lexeme = "";
                    }
                    break;
                case PRE_PUN:
                    String ss = lexeme + c;
                    if (punctuatorSet.contains(ss) && prePunctuatorSet.contains(ss)) {
                        state = SCANNER_STATE.PUN_AND_PRE;
                        lexeme = ss;
                    } else if (punctuatorSet.contains(ss)) {
                        state = SCANNER_STATE.INITIAL;
                        genToken(ss);
                        lexeme = "";
                    } else if (prePunctuatorSet.contains(ss)) {
                        state = SCANNER_STATE.PRE_PUN;
                        lexeme = ss;
                    } else {
                        state = SCANNER_STATE.ERROR;
                        keep = true;
                    }
                    break;
                case END:
                    end = true;
                    genEOFToken();
                    break;
                case ERROR:
                    end = true;
                    String errorStr = "ERROR: \n"
                            + "At: " + lineNum + ":" + colNum + "\n"
                            + "Lexeme: " + lexeme + "\n"
                            + "With: " + c + "\n";
                    System.out.println(errorStr);
                    break;
            }
        }

        String oFile = MiniCCUtil.removeAllExt(iFile) + MiniCCCfg.MINICC_SCANNER_OUTPUT_EXT;
        MiniCCUtil.createAndWriteFile(oFile, strTokens);

        return oFile;
    }
}
