public class SymbolTableItem {
    //常量也有地址，只不过是值不能修改，变量可以修改值
    //符号表一行
    public static final int CONST=1, VAR=2, INTFUN=3, VOIDFUN=4, PARAM=5, STR=6;

    public String name;//标识符名（常量名、变量名or过程名）
    public String value;//变量的值，在赋值语句的时候使用，因为可能是字符串，所以用string
    public int type;//类型：1常量（只有int）、2变量（只有int）、3int函数、4void函数、5参数、6字符串
    public int level;//所在层次
    public int address;//地址
    public int parameterNumber;//参数个数，函数专用

    public SymbolTableItem(String name, String value, int type, int level, int address, int parameterNumber) {
        this.name = name;
        this.value = value;
        this.type = type;
        this.level = level;
        this.address = address;
        this.parameterNumber=parameterNumber;
    }
}
