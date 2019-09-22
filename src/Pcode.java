public class Pcode {
    //书 P 316
    public static final String LIT="LIT";
    public static final String OPR="OPR";
    public static final String LOD="LOD";
    public static final String STO="STO";
    public static final String CALA="CALA";//开辟新栈帧
    public static final String CALB="CALB";//移动PC大新的指令
    public static final String INT="INT";
    public static final String JMP="JMP";
    public static final String JPC="JPC";
    public static final String RED="RED";
    public static final String WRTI="WRTI";//WRT拆成两个指令
    public static final String WRTS="WRTS";

    public String F;
    public int L;
    public int A;
    public String S;

    public Pcode(String f, int l, int a, String s) {
        this.F = f;
        this.L = l;
        this.A = a;
        this.S = s;
    }
}
