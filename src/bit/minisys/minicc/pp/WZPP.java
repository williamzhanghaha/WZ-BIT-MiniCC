package bit.minisys.minicc.pp;

import bit.minisys.minicc.MiniCCCfg;
import bit.minisys.minicc.internal.util.MiniCCUtil;

import java.util.ArrayList;

@Deprecated
public class WZPP implements IMiniCCPreProcessor {
    @Override
    public String run(String iFile) throws Exception {
        System.out.println("Adding declarations for MARS functions");
        ArrayList<String> src = MiniCCUtil.readFile(iFile);
        StringBuilder output = new StringBuilder();
        output.append("void Mars_PrintStr(char *);\n");
        output.append("int Mars_GetInt();\n");
        output.append("void Mars_PrintInt(int);\n");
        for (String line : src) {
            output.append(line);
            output.append("\n");
        }
        String oFile = MiniCCUtil.removeAllExt(iFile) + MiniCCCfg.MINICC_PP_OUTPUT_EXT;
        MiniCCUtil.createAndWriteFile(oFile, output.toString());
        return oFile;
    }
}
