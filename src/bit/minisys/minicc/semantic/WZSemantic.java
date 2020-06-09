package bit.minisys.minicc.semantic;

import bit.minisys.minicc.parser.ast.ASTCompilationUnit;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

public class WZSemantic implements IMiniCCSemantic {
    private ASTCompilationUnit program;
    @Override
    public String run(String iFile) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        program = mapper.readValue(new File(iFile), ASTCompilationUnit.class);

        // Add Mars Functions
        ASTCompilationUnit marsFuncs = mapper.readValue(WZMarsFuncJson.CONTENT, ASTCompilationUnit.class);
        program.items.addAll(0, marsFuncs.items);

        ErrorHandler errorHandler = new ErrorHandler();

        System.out.println("===================Semantic===================");

        NodeInfo programNode = new NodeInfo(program, errorHandler);
        if (programNode.visitProgram()) {
            // complete
            System.out.println("Semantic completed");
            errorHandler.printErrorCntResult();
        } else {
            System.out.println("Not a program");
        }

        System.out.println("==============================================");

        return iFile;
    }

    public ASTCompilationUnit getProgram() {
        return program;
    }
}
