package bit.minisys.minicc.semantic;

import bit.minisys.minicc.parser.ast.ASTNode;

public class ErrorHandler {

    private int errorCnt = 0;

    public void notDefined(String name) {
        name = "\"" + name + "\"";
        String errorMsg = "ES01 >> "
                + name
                + " not defined or not declared";
        handleErrorMsg(errorMsg);
    }

    public void duplicateDeclaration(String name) {
        name = "\"" + name + "\"";
        String errorMsg = "ES02 >> "
                + name
                + " has already been defined";
        handleErrorMsg(errorMsg);
    }

    public void breakNotInLoop() {
        String errorMsg = "ES03 >> "
                + "Break statement must be in a loop";
        handleErrorMsg(errorMsg);
    }

    public void funcArgNotMatch(String name) {
        name = "\"" + name + "\"";
        String errorMsg = "ES04 >> "
                + "Function "
                + name
                + " parameter(s) not match";
        handleErrorMsg(errorMsg);
    }

    public void opndNotMatch() {
        String errorMsg = "ES05 >> "
                + "Operation and expressions not match";
        handleErrorMsg(errorMsg);
    }

    public void arrayAccessOut(String name) {
        name = "\"" + name + "\"";
        String errorMsg = "ES06 >> "
                + "Array "
                + name
                + " access out of bounds";
        handleErrorMsg(errorMsg);
    }

    public void gotoNotExist() {
        String errorMsg = "ES07 >> "
                + "Goto label not found";
        handleErrorMsg(errorMsg);
    }

    public void funcNotReturn(String name) {
        name = "\"" + name + "\"";
        String errorMsg = "ES08 >> "
                + "Function "
                + name
                + " lack of return statement";
        handleErrorMsg(errorMsg);
    }

    public void require(String name) {
        name = "\"" + name + "\"";
        String errorMsg = name + "required";
        handleErrorMsg(errorMsg);
    }

    public void handleErrorMsg(String errorMsg) {
        errorCnt++;
        System.out.println(errorCnt + ": " + errorMsg);
    }

    public void printErrorCntResult() {
        if (errorCnt == 0) {
            System.out.println("No error found");
        } else {
            System.out.println("" + errorCnt + " error(s) found");
        }
    }
}
