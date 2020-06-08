package bit.minisys.minicc.icgen;

public class WZLabelGenerator {
    private int currentId = 0;
    public LabelSymbol genNewLabel() {
        LabelSymbol labelSymbol = new LabelSymbol(currentId);
        currentId++;
        return labelSymbol;
    }
}
