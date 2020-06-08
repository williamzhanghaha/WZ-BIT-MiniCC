package bit.minisys.minicc.semantic.symbol;

import java.util.ArrayList;
import java.util.List;

public class ArraySymbol extends Symbol {
    // array size for each dimension
    // -1 represent for size that we not concerned
    private List<Integer> size = new ArrayList<>();

    public ArraySymbol(String identifier, int size) {
        this.identifier = identifier;
        this.size.add(size);
    }

    public ArraySymbol(ArraySymbol arraySymbol, int size) {
        this.identifier = arraySymbol.identifier;
        this.size.addAll(arraySymbol.size);
        this.size.add(size);
    }

    public ArraySymbol(ArraySymbol s) {
        this.identifier = s.identifier;
        this.size = s.size;
    }


    // ignore current dimension size
    public ArraySymbol(String identifier) {
        this.identifier = identifier;
        this.size.add(-1);
    }

    public List<Integer> getSize() {
        return size;
    }

    public boolean checkIndexed(List<Integer> indexes) {
        if (size.size() < indexes.size()) {
            return false;
        }
        for (int i = 0; i < indexes.size(); i++) {
            int sizeRequired = size.get(i);
            int sizeInput = indexes.get(i);
            if (sizeInput == -1 || sizeRequired == -1) {
                continue;
            }
            if (sizeInput >= sizeRequired) {
                return false;
            }
        }
        return true;
    }
}
