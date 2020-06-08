package bit.minisys.minicc.semantic.symbol;

import java.util.ArrayList;
import java.util.List;

public class Specifier {
    List<String> specifers = new ArrayList<>();

    public void addSpecifier(String s) {
        specifers.add(s);
    }

    public boolean equalsSpecifier(Specifier s) {
        if (this.specifers.size() != s.specifers.size()) {
            return false;
        }
        for (int i = 0; i < this.specifers.size(); i++) {
            String existed = this.specifers.get(i);
            String input = s.specifers.get(i);
            if (!existed.equals(input)) {
                return false;
            }
        }
        return true;
    }

    public List<String> getSpecifers() {
        return specifers;
    }

    // check if constant fit

    public boolean checkIntConstant() {
        for (String s : specifers) {
            if (s.equals("int")
                    || s.equals("long")
                    || s.equals("float")
                    || s.equals("double")
                    || s.equals("char")) {
                return true;
            }
        }
        return false;
    }

    public boolean checkFloatConstant() {
        for (String s : specifers) {
            if (s.equals("float") || s.equals("double")) {
                return true;
            }
        }
        return false;
    }

    public boolean checkCharConstant() {
        return checkIntConstant();
    }
}
