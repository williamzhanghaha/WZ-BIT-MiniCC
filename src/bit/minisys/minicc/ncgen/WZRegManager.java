package bit.minisys.minicc.ncgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WZRegManager {
    private final Map<Integer, WZReg> regs = new HashMap<>();

    // LRU
    int clockPtr = 8;
    private final Map<Integer, Boolean> recentlyUsed = new HashMap<>();

    public WZRegManager() {
        regs.put(0, new WZReg(0));
        for (int i = 2; i <= 15; i++) {
            regs.put(i, new WZReg(i));
        }
        regs.put(24, new WZReg(24));
        regs.put(25, new WZReg(25));
        regs.put(29, new WZReg(29));
        regs.put(31, new WZReg(31));
        for (int i = 8; i <= 15; i++) {
            recentlyUsed.put(i, false);
        }
    }

    public WZReg getReg(int num) {
        if (num >= 8 && num <= 15) {
            recentlyUsed.put(num, true);
        }
        return regs.get(num);
    }

    public void useReg(WZReg reg) {
        int num = reg.getRegNumber();
        if (num >= 8 && num <= 15)
        recentlyUsed.put(num, true);
    }

    public int getAvailableRegNum() {
        for (int i = 8; i <= 15; i ++) {
            WZReg reg = regs.get(i);
            if (reg.getVar() == null) {
                recentlyUsed.put(i, true);
                return i;
            }
        }
        return -1;
    }

    private void increaseClockPtr() {
        clockPtr++;
        if (clockPtr > 15) {
            clockPtr = 8;
        }
    }

    public int getLRURegNum() {
        // if has available reg, return -1
        int num = getAvailableRegNum();
        if (num != -1) {
            recentlyUsed.put(num, false);
            return -1;
        }
        while (recentlyUsed.get(clockPtr)) {
            recentlyUsed.put(clockPtr, false);
            increaseClockPtr();
        }
        int ptr = clockPtr;
        increaseClockPtr();
        return ptr;
    }

    public void freeReg(int num) {
        WZReg reg = regs.get(num);
        reg.setVar(null);
        recentlyUsed.put(num, false);
    }

    public void freeAllTmpReg() {
        for (int i = 8; i <= 15; i++) {
            if (regs.get(i).getVar() != null) {
                regs.get(i).getVar().moveBackToMem();
            }
        }
    }

    public List<WZReg> getAllUsedTmpReg() {
        List<WZReg> usedRegs = new ArrayList<>();
        for (int i = 8; i <= 15; i++) {
            if (regs.get(i).getVar() != null) {
                usedRegs.add(regs.get(i));
            }
        }
        return usedRegs;
    }

    public List<WZReg> getAllUsedArgReg() {
        List<WZReg> usedRegs = new ArrayList<>();
        for (int i = 4; i <= 7; i++) {
            if (regs.get(i).getVar() != null) {
                usedRegs.add(regs.get(i));
            }
        }
        return usedRegs;
    }

    public WZReg getRegTmp1() {
        return regs.get(24);
    }

    public WZReg getRegTmp2() {
        return regs.get(25);
    }

    public WZReg getRegForArg(int index) {
        if (index >= 0 && index < 4) {
            return regs.get(4 + index);
        }
        return null;
    }
}
