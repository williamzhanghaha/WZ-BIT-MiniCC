package bit.minisys.minicc.ncgen;

public class WZCodeTemplate {
    public static final String DATA_SEG_START =
            ".data\n" +
            "blank : .asciiz \" \"\n";

    public static final String TEXT_SEG_START =
            ".text\n" +
            "__init:\n" +
            "\tlui $sp, 0x8000\n" +
            "\taddi $sp, $sp, 0x0000\n" +
            "\tmove $fp, $sp\n" +
            "\tadd $gp, $gp, 0x8000\n" +
            "\tjal main\n" +
            "\tli $v0, 10\n" +
            "\tsyscall\n" +
            "Mars_PrintInt:\n" +
            "\tli $v0, 1\n" +
            "\tsyscall\n" +
            "\tli $v0, 4\n" +
            "\tmove $v1, $a0\n" +
            "\tla $a0, blank\n" +
            "\tsyscall\n" +
            "\tmove $a0, $v1\n" +
            "\tjr $ra\n" +
            "Mars_GetInt:\n" +
            "\tli $v0, 5\n" +
            "\tsyscall\n" +
            "\tjr $ra\n" +
            "Mars_PrintStr:\n" +
            "\tli $v0, 4\n" +
            "\tsyscall\n" +
            "\tjr $ra\n";
}
