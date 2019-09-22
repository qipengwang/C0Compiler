import java.util.ArrayList;

public class Error {
    private ArrayList<String> errorMessage;
    private String[] errors;
    public Error(){
        this.errorMessage=new ArrayList<String>();
        errors=new String[100];
        //词法错误
        errors[0]="标识符不能以数字开头";
        errors[1]="数字不能前导0";
        errors[2]="缺少 */";
        errors[3]="'!'后面缺少‘=’";
        errors[4]="\\后面缺少字符";
        errors[5]="缺少 \"";
        errors[6]="非法字符";
        //语法错误中的错误，包含符号表
        errors[7]="非法单词/符号";
        errors[8]="此处应该是 const";
        errors[9]="缺少分号；";
        errors[10]="此处应该为标识符";
        errors[11]="此处应该为 =";
        errors[12]="此处应该为整数";
        errors[13]="此处应为 int";
        errors[14]="缺少 main 函数";
        errors[15]="非法函数头部";//函数不是int void开始的
        errors[16]="此处应为（";
        errors[17]="此处应为 ）";
        errors[18]="此处应为 {";
        errors[19]="此处应为 }";
        errors[20]="重复定义main函数";
        errors[21]="此处应为void";
        errors[22]="缺少函数名";
        errors[23]="int类型函数缺少返回值";
        errors[24]="void类型函数不能有返回值";
        errors[25]="printf缺少逗号'，'";
        errors[26]="表达式错误";
        errors[27]="函数应定义在main之前";
        errors[28]="标识符重复定义";
        errors[29]="常量不能作为语句的开始";
        errors[30]="标识符没有定义";
        errors[31]="参数不匹配";
        errors[32]="void函数不能参与运算";
        errors[33]="scanf不能作用于常量/函数";
        errors[34]="输入不合法";
        errors[35]="计算错误：除数为0";
        errors[36]="计算错误：运行栈溢出";
        errors[37]="";
        errors[38]="";
        errors[39]="";
        errors[40]="";
    }

    public void addErrorMessage(Token token, int errorCode){
        if(errorCode==7){
            errorMessage.add("第"+token.line+"行："+token.symbol+" 是非法单词/符号");
        } else if(errorCode==28) {
            errorMessage.add("第"+token.line+"行：标识符 "+token.symbol+" 重复定义");
        } else if(errorCode==29) {
            errorMessage.add("第"+token.line+"行：常量 "+token.symbol+" 不能作为语句的开始");
        } else if(errorCode==30) {
            errorMessage.add("第"+token.line+"行：标识符 "+token.symbol+" 没有定义");
        } else {
            errorMessage.add("第"+token.line+"行："+errors[errorCode]);
        }

    }

    public void addErrorMessage(int line, int errorCode, char c){
        if(errorCode==6) {
            errorMessage.add("第"+line+"行："+c+" "+errors[errorCode]);
        } else {
            errorMessage.add("第"+line+"行："+errors[errorCode]);
        }
    }

    public void addErrorMessage(int no, int errorCode){
        errorMessage.add("第"+no+"条pcode："+errors[errorCode]);
    }

    public ArrayList<String> getErrorMessage() {
        return errorMessage;
    }
}
