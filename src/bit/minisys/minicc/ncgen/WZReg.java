package bit.minisys.minicc.ncgen;

public class WZReg {
    private int regNumber;
    private WZVar var = null;

    public WZReg(int regNumber) {
        this.regNumber = regNumber;
    }

    public WZVar getVar() {
        return var;
    }

    public void setVar(WZVar var) {
        this.var = var;
    }

    public int getRegNumber() {
        return regNumber;
    }
}
