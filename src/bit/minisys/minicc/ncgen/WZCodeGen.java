package bit.minisys.minicc.ncgen;

import bit.minisys.minicc.MiniCCCfg;
import bit.minisys.minicc.icgen.ConstantSymbol;
import bit.minisys.minicc.icgen.FuncInfo;
import bit.minisys.minicc.icgen.LabelSymbol;
import bit.minisys.minicc.icgen.WZQuat;
import bit.minisys.minicc.internal.util.MiniCCUtil;
import bit.minisys.minicc.semantic.symbol.Symbol;

import java.util.ArrayList;
import java.util.List;

public class WZCodeGen implements IMiniCCCodeGen {

    // func info list from icgen
    private final List<FuncInfo> funcInfoList;

    private final WZRegManager regManager = new WZRegManager();

    private int dataConstNum = 0;
    private int tmpLabelNum = 0;

    private WZStackFrame currentStackFrame = null;

    private final List<Symbol> argSymbols = new ArrayList<>();

    // target code
    private final StringBuilder targetCode = new StringBuilder();
    private final StringBuilder dataSegCode = new StringBuilder(WZCodeTemplate.DATA_SEG_START);
    private final StringBuilder textSegCode = new StringBuilder(WZCodeTemplate.TEXT_SEG_START);

    public WZCodeGen(List<FuncInfo> funcInfoList) {
        this.funcInfoList = funcInfoList;
    }

    @Override
    public String run(String iFile, MiniCCCfg cfg) throws Exception {
        String oFile = MiniCCUtil.remove2Ext(iFile) + MiniCCCfg.MINICC_CODEGEN_OUTPUT_EXT;
        if(!cfg.target.equals("mips")) {
            System.out.println("Not yet implemented for this target.");
            return iFile;
        }
        start();
        MiniCCUtil.createAndWriteFile(oFile, targetCode.toString());
        System.out.println("Target code generation finished!");
        return oFile;
    }

    private String addData(String s) {
        String tag = "_data_" + dataConstNum;
        dataConstNum++;
        dataSegCode.append(tag);
        dataSegCode.append(" : .asciiz ");
        dataSegCode.append(s);
        dataSegCode.append("\n");
        return tag;
    }

    private void addLabel(Symbol symbol) {
        String name;
        if (symbol instanceof LabelSymbol) {
            name = "_label_" + ((LabelSymbol) symbol).getId();
        } else {
            name = symbol.getIdentifier();
        }
        textSegCode.append(name);
        textSegCode.append(":\n");
    }

    private void addLabel(String label) {
        textSegCode.append(label);
        textSegCode.append(":\n");
    }

    private String genTmpLabel() {
        String name = "_tmp_label_" + tmpLabelNum;
        tmpLabelNum++;
        return name;
    }

    private void addCode(String code) {
        textSegCode.append("\t");
        textSegCode.append(code);
        textSegCode.append("\n");
    }

    private void start() {
        for (FuncInfo funcInfo : funcInfoList) {
            if (funcInfo.isGlobal()) {
                continue;
            }
            genCodeForFunc(funcInfo);
        }
        targetCode.append(dataSegCode);
        targetCode.append(textSegCode);
    }

    private void genCodeForFunc(FuncInfo funcInfo) {
        currentStackFrame = new WZStackFrame(funcInfo, regManager);
        regManager.freeAllTmpReg();
        addLabel(funcInfo.getFuncSymbol());
        // allocate mem for frame
        addCode("subu $sp, $sp, " + currentStackFrame.getSize());

        // generate code for quats
        for (WZQuat quat : funcInfo.getQuats()) {
            genCodeForQuat(quat);
        }


        // ret for void func
        if (!funcInfo.getFuncSymbol().needReturn()) {
            // restore $sp
            addCode("addu $sp, $sp, " + currentStackFrame.getSize());
            addCode("jr $31");
        }
    }

    private void genCodeForQuat(WZQuat quat) {
        addCode("\n\t# For ic: " + quat.toString());
        if (quat.getOp() == WZQuat.WZ_QUAT_OP.LABEL) {
            moveBackAllTmpReg();
            moveBackAllArgReg();
            addLabel(quat.getResult());
        } else if (quat.getOp() == WZQuat.WZ_QUAT_OP.ASSIGN) {
            WZVar toVar = currentStackFrame.getWZVarFromSymbol(quat.getResult());
            WZReg toReg = getReg(toVar, false);
            if (quat.getOpnd1() instanceof ConstantSymbol) {
                ConstantSymbol constantSymbol = (ConstantSymbol) quat.getOpnd1();
                // todo: add more constant type
                if (constantSymbol.getType() == ConstantSymbol.CONST_TYPE.INT) {
                    addCode("li $" + toReg.getRegNumber() + ", " + constantSymbol.getIntVal());
                }
            } else {
                WZVar fromVar = currentStackFrame.getWZVarFromSymbol(quat.getOpnd1());
                WZReg fromReg = getReg(fromVar, true);
                addCode("move $" + toReg.getRegNumber() + ", $" + fromReg.getRegNumber());
            }
            toVar.setActiveInMem(false);
        } else if (quat.getOp() == WZQuat.WZ_QUAT_OP.JMP) {
            moveBackAllTmpReg();
            moveBackAllArgReg();
            LabelSymbol symbol = (LabelSymbol) quat.getResult();
            String label = "_label_" + symbol.getId();
            addCode("j " + label);
        } else if (quat.getOp() == WZQuat.WZ_QUAT_OP.JF) {
            moveBackAllTmpReg();
            moveBackAllArgReg();
            LabelSymbol symbol = (LabelSymbol) quat.getResult();
            String label = "_label_" + symbol.getId();
            WZReg op1Reg;
            if (quat.getOpnd1() instanceof ConstantSymbol) {
                ConstantSymbol constantSymbol = (ConstantSymbol) quat.getOpnd1();
                op1Reg = regManager.getRegTmp1();
                if (constantSymbol.getType() == ConstantSymbol.CONST_TYPE.INT) {
                    addCode("li $" + op1Reg.getRegNumber() + ", " + constantSymbol.getIntVal());
                }
            } else {
                WZVar op1Var = currentStackFrame.getWZVarFromSymbol(quat.getOpnd1());
                op1Reg = getReg(op1Var, true);
            }
            WZReg op2Reg = regManager.getRegTmp2();
            addCode("li $" + op2Reg.getRegNumber() + ", 0");
            addCode("beq $" + op1Reg.getRegNumber() + ", $" + op2Reg.getRegNumber() + ", " + label);
        } else if (quat.getOp() == WZQuat.WZ_QUAT_OP.RET) {
            if (quat.getResult() != null) {
                if (quat.getResult() instanceof ConstantSymbol) {
                    ConstantSymbol constantSymbol = (ConstantSymbol) quat.getResult();
                    if (constantSymbol.getType() == ConstantSymbol.CONST_TYPE.INT) {
                        addCode("li $2, " + constantSymbol.getIntVal());
                    }
                } else {
                    WZVar resultVar = currentStackFrame.getWZVarFromSymbol(quat.getResult());
                    WZReg reg = getReg(resultVar, true);
                    addCode("move $2, $" + reg.getRegNumber());
                }
            }
            addCode("addu $sp, $sp, " + currentStackFrame.getSize());
            addCode("jr $ra");
        } else if (quat.getOp() == WZQuat.WZ_QUAT_OP.ADD
                || quat.getOp() == WZQuat.WZ_QUAT_OP.MUL
                || quat.getOp() == WZQuat.WZ_QUAT_OP.SUB
                || quat.getOp() == WZQuat.WZ_QUAT_OP.DIV
                || quat.getOp() == WZQuat.WZ_QUAT_OP.MOD
                || quat.getOp() == WZQuat.WZ_QUAT_OP.EQ
                || quat.getOp() == WZQuat.WZ_QUAT_OP.LT
                || quat.getOp() == WZQuat.WZ_QUAT_OP.MT
                || quat.getOp() == WZQuat.WZ_QUAT_OP.LET
                || quat.getOp() == WZQuat.WZ_QUAT_OP.MET
                || quat.getOp() == WZQuat.WZ_QUAT_OP.AND
                || quat.getOp() == WZQuat.WZ_QUAT_OP.OR) {
            WZReg op1Reg, op2Reg;
            WZVar toVar = currentStackFrame.getWZVarFromSymbol(quat.getResult());
            WZReg toReg = getReg(toVar, false);
            // todo: add more constant type
            if (quat.getOpnd1() instanceof ConstantSymbol) {
                ConstantSymbol constantSymbol = (ConstantSymbol) quat.getOpnd1();
                op1Reg = regManager.getRegTmp1();
                if (constantSymbol.getType() == ConstantSymbol.CONST_TYPE.INT) {
                    addCode("li $" + op1Reg.getRegNumber() + ", " + constantSymbol.getIntVal());
                }
            } else {
                WZVar op1Var = currentStackFrame.getWZVarFromSymbol(quat.getOpnd1());
                op1Reg = getReg(op1Var, true);
            }
            if (quat.getOpnd2() instanceof ConstantSymbol) {
                ConstantSymbol constantSymbol = (ConstantSymbol) quat.getOpnd2();
                op2Reg = regManager.getRegTmp2();
                if (constantSymbol.getType() == ConstantSymbol.CONST_TYPE.INT) {
                    addCode("li $" + op2Reg.getRegNumber() + ", " + constantSymbol.getIntVal());
                }
            } else {
                WZVar op2Var = currentStackFrame.getWZVarFromSymbol(quat.getOpnd2());
                op2Reg = getReg(op2Var, true);
            }

            if (quat.getOp() == WZQuat.WZ_QUAT_OP.ADD
                    || quat.getOp() == WZQuat.WZ_QUAT_OP.MUL
                    || quat.getOp() == WZQuat.WZ_QUAT_OP.SUB
                    || quat.getOp() == WZQuat.WZ_QUAT_OP.AND
                    || quat.getOp() == WZQuat.WZ_QUAT_OP.OR) {
                String opString = "";
                if (quat.getOp() == WZQuat.WZ_QUAT_OP.ADD) {
                    opString = "add";
                } else if (quat.getOp() == WZQuat.WZ_QUAT_OP.MUL) {
                    opString = "mul";
                } else if (quat.getOp() == WZQuat.WZ_QUAT_OP.SUB) {
                    opString = "sub";
                } else if (quat.getOp() == WZQuat.WZ_QUAT_OP.AND) {
                    opString = "and";
                } else if (quat.getOp() == WZQuat.WZ_QUAT_OP.OR) {
                    opString = "or";
                }
                addCode(opString + " $" + toReg.getRegNumber() + ", $" + op1Reg.getRegNumber() + ", $" + op2Reg.getRegNumber());
            } else if (quat.getOp() == WZQuat.WZ_QUAT_OP.DIV) {
                addCode("div $" + op1Reg.getRegNumber() + ", $" + op2Reg.getRegNumber());
                addCode("mflo $" + toReg.getRegNumber());
            } else if (quat.getOp() == WZQuat.WZ_QUAT_OP.MOD) {
                addCode("div $" + op1Reg.getRegNumber() + ", $" + op2Reg.getRegNumber());
                addCode("mfhi $" + toReg.getRegNumber());
            } else if (quat.getOp() == WZQuat.WZ_QUAT_OP.EQ
                    || quat.getOp() == WZQuat.WZ_QUAT_OP.LT
                    || quat.getOp() == WZQuat.WZ_QUAT_OP.MT
                    || quat.getOp() == WZQuat.WZ_QUAT_OP.LET
                    || quat.getOp() == WZQuat.WZ_QUAT_OP.MET) {
                String tmpLabel1 = genTmpLabel();
                String tmpLabel2 = genTmpLabel();
                if (quat.getOp() == WZQuat.WZ_QUAT_OP.EQ) {
                    addCode("beq $" + op1Reg.getRegNumber() + ", $" + op2Reg.getRegNumber() + ", " + tmpLabel1);
                } else if (quat.getOp() == WZQuat.WZ_QUAT_OP.LT) {
                    addCode("blt $" + op1Reg.getRegNumber() + ", $" + op2Reg.getRegNumber() + ", " + tmpLabel1);
                } else if (quat.getOp() == WZQuat.WZ_QUAT_OP.MT) {
                    addCode("bgt $" + op1Reg.getRegNumber() + ", $" + op2Reg.getRegNumber() + ", " + tmpLabel1);
                } else if (quat.getOp() == WZQuat.WZ_QUAT_OP.LET) {
                    addCode("ble $" + op1Reg.getRegNumber() + ", $" + op2Reg.getRegNumber() + ", " + tmpLabel1);
                } else if (quat.getOp() == WZQuat.WZ_QUAT_OP.MET) {
                    addCode("bge $" + op1Reg.getRegNumber() + ", $" + op2Reg.getRegNumber() + ", " + tmpLabel1);
                }
                addCode("li $" + toReg.getRegNumber() + ", 0");
                addCode("b " + tmpLabel2);
                addLabel(tmpLabel1);
                addCode("li $" + toReg.getRegNumber() + ", 1");
                addLabel(tmpLabel2);
            }
            toVar.setActiveInMem(false);
        } else if (quat.getOp() == WZQuat.WZ_QUAT_OP.ARG) {
            argSymbols.add(quat.getResult());
        } else if (quat.getOp() == WZQuat.WZ_QUAT_OP.CALL) {
            moveBackAllTmpReg();
            moveBackAllArgReg();
            for (int i = argSymbols.size() - 1; i >= 0; i--) {
                Symbol symbol = argSymbols.get(i);
                WZReg srcReg;
                if (symbol instanceof ConstantSymbol) {
                    ConstantSymbol constantSymbol = (ConstantSymbol) symbol;
                    srcReg = regManager.getRegTmp1();
                    if (constantSymbol.getType() == ConstantSymbol.CONST_TYPE.INT) {
                        addCode("li $" + srcReg.getRegNumber() + ", " + constantSymbol.getIntVal());
                    } else if (constantSymbol.getType() == ConstantSymbol.CONST_TYPE.STRING) {
                        String content = constantSymbol.getIdentifier();
                        String label = addData(content);
                        addCode("la $" + srcReg.getRegNumber() + ", " + label);
                    }
                } else {
                    WZVar op1Var = currentStackFrame.getWZVarFromSymbol(symbol);
                    srcReg = getReg(op1Var, true);
                }
                int pos = argSymbols.size() - 1 - i;
                if (pos < 4) {
                    WZReg toReg = regManager.getRegForArg(pos);
                    addCode("move $" + toReg.getRegNumber() + ", $" + srcReg.getRegNumber());
                } else {
                    // todo: more than 4 args
                }
            }
            addCode("sw $ra, " + currentStackFrame.getRaOffset() + "($sp)");
            addCode("jal " + quat.getResult().getIdentifier());
            addCode("lw $ra, " + currentStackFrame.getRaOffset() + "($sp)");
            argSymbols.clear();
        } else if (quat.getOp() == WZQuat.WZ_QUAT_OP.GRV) {
            WZVar toVar = currentStackFrame.getWZVarFromSymbol(quat.getResult());
            WZReg toReg = getReg(toVar, false);
            addCode("move $" + toReg.getRegNumber() + ", $2");
            toVar.setActiveInMem(false);
        }

    }

    private WZReg getReg(WZVar var, boolean needAssign) {
        // var in reg
        if (!var.getActiveRegs().isEmpty()) {
            WZReg reg = var.getActiveRegs().get(0);
            // for LRU
            regManager.useReg(reg);
            return reg;
        }

        // var not in reg
        int num = regManager.getAvailableRegNum();
        if (num != -1) {
            // has available reg
            WZReg reg = regManager.getReg(num);
            reg.setVar(var);
            var.addActiveReg(reg);
            addCode("# select $" + num + " for var " + var.getSymbol().getIdentifier());
            if (needAssign) {
                // add code for load
                addCode("lw $" + num + ", " + currentStackFrame.getOffset(var) + "($sp)");
            }
            return reg;
        } else {
            // no available reg
            num = regManager.getLRURegNum();
            WZReg reg = regManager.getReg(num);
            addCode("# select $" + num + " for var " + var.getSymbol().getIdentifier());
            // store old reg value
            WZVar oldVar = reg.getVar();
            if (!oldVar.isActiveInMem()) {
                addCode("sw $" + num + ", " + currentStackFrame.getOffset(oldVar) + "($sp)");
            }
            // clear all reg for old var
            oldVar.moveBackToMem();
            // load new var
            reg.setVar(var);
            var.addActiveReg(reg);
            if (needAssign) {
                addCode("lw $" + num + ", " + currentStackFrame.getOffset(var) + "($sp)");
            }
            return reg;
        }
    }

    private void moveBackAllTmpReg() {
        for (WZReg reg : regManager.getAllUsedTmpReg()) {
            WZVar var = reg.getVar();
            if (var == null) {
                return;
            }
            if (!var.isActiveInMem()) {
                addCode("sw $" + reg.getRegNumber() + ", " + currentStackFrame.getOffset(var) + "($sp)");
            }
            var.moveBackToMem();
        }
    }

    public void moveBackAllArgReg() {
        for (WZReg reg : regManager.getAllUsedArgReg()) {
            WZVar var = reg.getVar();
            if (var == null) {
                return;
            }
            if (!var.isActiveInMem()) {
                addCode("sw $" + reg.getRegNumber() + ", " + currentStackFrame.getOffset(var) + "($sp)");
            }
            var.moveBackToMem();
        }
    }

}
