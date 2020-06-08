package bit.minisys.minicc.ncgen;

import bit.minisys.minicc.MiniCCCfg;
import bit.minisys.minicc.icgen.FuncInfo;
import bit.minisys.minicc.internal.util.MiniCCUtil;

import java.util.List;

public class WZCodeGen implements IMiniCCCodeGen {

    private final List<FuncInfo> funcInfoList;

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



        System.out.println("Target code generation finished!");

        return oFile;
    }
}
