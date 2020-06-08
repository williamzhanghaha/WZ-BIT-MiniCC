package bit.minisys.minicc.icgen;

import bit.minisys.minicc.MiniCCCfg;
import bit.minisys.minicc.internal.util.MiniCCUtil;
import bit.minisys.minicc.parser.ast.ASTCompilationUnit;

public class WZICGen implements IMiniCCICGen {

    private final ASTCompilationUnit program;

    public WZICGen(ASTCompilationUnit program) {
        this.program = program;
    }

    @Override
    public String run(String iFile) throws Exception {
        if (program == null || program.info == null) {
            System.out.println("No WZSemantic Result Found.");
            System.out.println("IC Gen Failed!");
        }
        WZICBuilder icBuilder = new WZICBuilder();
        icBuilder.visit(program);
        String output = icBuilder.getOutput();

        String oFile = MiniCCUtil.remove2Ext(iFile) + MiniCCCfg.MINICC_ICGEN_OUTPUT_EXT;
        MiniCCUtil.createAndWriteFile(oFile, output);
        System.out.println("IC Gen finished!");
        return oFile;
    }
}
