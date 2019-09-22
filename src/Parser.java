import java.util.ArrayList;
import java.util.HashMap;

/**
 * 语法分析器类
 * 合并了部分简单产生式之后的函数列表
 * <p>
 * Parser 构造函数初始化
 * success 返回是否编译成功
 * program 程序
 * constDeclaration 常量声明
 * constDefine 常量定义
 * variateDeclaration 变量声明
 * intFunctionDeclaration 有返回值函数
 * voidFunctionDeclaration 无返回值函数
 * compoundStatement 复合语句
 * parameter 参数
 * expression 表达式
 * term 项
 * factor 因子
 * statement 语句
 * assignStatement 赋值语句
 * ifStatement 条件语句
 * condition 条件
 * whileStatement 循环语句
 * callStatement 函数调用语句
 * valueParameter 值参数表
 * statementList 语句序列
 * scanfStatement 读语句
 * printfStatement 写语句
 * returnStatement 返回语句
 */

public class Parser {
    private ArrayList<Token> tokenList;
    private Error error;
    private Token currentToken;//当前读取的token
    private int level;//当前层次，1表示全局，2表示函数内部，因为if else while里面都不能定义 常量 变量 函数，所以level只有两个值
    private boolean returnFlag;//int函数的返回值标志
    private boolean mainFlag;//是否有main函数
    private int tokenPointer;//当前应该读取的token下标
    //符号表管理：变量的声明只在全局变量和函数开始的时候，所以只需要两个表就可以了，if、while里面都不能声明变量
    private ArrayList<SymbolTableItem> globalSymbolTable;//全局符号表，存储全局变量和函数名
    private HashMap<String, ArrayList<SymbolTableItem>> localSymbolTable;//局部符号表，存储局部变量和参数
    private ArrayList<Pcode> pcodeList;
    //常量变量地址，用于在解释程序的时候使用
    private final int startOffset = 3;//变量在栈中存放的起始地址，0原栈底指针，1原PC指针
    private int varAddress;
    private boolean isParameter;

    public Parser(ArrayList<Token> tokenList) {
        System.out.println("开始parser：");
        this.tokenList = tokenList;
        this.tokenList.add(new Token("", SymbolType.END, "", tokenList.get(tokenList.size() - 1).line));//加两个END标志方便后面判断以防止越界
        this.tokenList.add(new Token("", SymbolType.END, "", tokenList.get(tokenList.size() - 1).line));
        this.error = new Error();
        this.currentToken = null;
        this.level = 1;//默认在全局层
        this.returnFlag = false;
        this.mainFlag = false;
        this.tokenPointer = 0;//从0开始读取
        this.globalSymbolTable = new ArrayList<SymbolTableItem>();
        this.localSymbolTable = new HashMap<String, ArrayList<SymbolTableItem>>();
        this.pcodeList = new ArrayList<Pcode>();
        this.isParameter = false;
        program();
    }

    public boolean success() {
        return this.error.getErrorMessage().size() == 0;
    }

    public ArrayList<Pcode> getPcodeList() {
        return pcodeList;
    }

    public ArrayList<String> getErrorMessage() {
        return error.getErrorMessage();
    }

    /**
     * 插入符号表 level已知，address动态确定
     *
     * @param name            名字
     * @param value           值：常量值 变量值 字符串本身
     * @param type            类型：SymbolTableItem中定义
     * @param parameterNumber 参数个数：函数调用的错误处理函数专用
     */
    private void insertTable(String name, String value, int type, int parameterNumber, int address) {
        if (level == 1) {
            //当前在最外层
            globalSymbolTable.add(new SymbolTableItem(name, value, type, level, address, parameterNumber));
        } else {
            ArrayList<SymbolTableItem> lst = localSymbolTable.get(globalSymbolTable.get(globalSymbolTable.size() - 1).name);
            if (lst == null) {
                localSymbolTable.put(globalSymbolTable.get(globalSymbolTable.size() - 1).name, new ArrayList<SymbolTableItem>());
                lst = localSymbolTable.get(globalSymbolTable.get(globalSymbolTable.size() - 1).name);
            }
            lst.add(new SymbolTableItem(name, value, type, level, address, parameterNumber));
        }
    }

    private SymbolTableItem searchTable(String name) {
        //查找对应表项，没有返回null
        if (level == 2) {
            //level==2至少插入了函数名
            ArrayList<SymbolTableItem> LST = localSymbolTable.get(globalSymbolTable.get(globalSymbolTable.size() - 1).name);
            if (LST != null) {
                for (SymbolTableItem sti : LST) {
                    if (sti.name.equals(name)) {
                        return sti;
                    }
                }
            }
        }
        for (SymbolTableItem sti : globalSymbolTable) {
            if (sti.name.equals(name)) {
                return sti;
            }
        }
        return null;//都没有
    }

    private boolean checkTable(String name) {
        //检查当前name是否能插入，不能返回false
        //标识符只要在当前层次不重名就可以了，局部变量和函数也可以重名！（长知识了QAQ）
        if (level == 1) {
            for (SymbolTableItem sti : globalSymbolTable) {
                if (sti.name.equals(name)) {
                    return false;
                }
            }
        } else {
            ArrayList<SymbolTableItem> LST = localSymbolTable.get(globalSymbolTable.get(globalSymbolTable.size() - 1).name);
            if (LST != null) {
                for (SymbolTableItem sti : LST) {
                    if (sti.name.equals(name)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void nextToken() {
        currentToken = tokenList.get(tokenPointer);
        tokenPointer++;
    }

    private void generatePcode(String f, int l, int a) {
        pcodeList.add(new Pcode(f, l, a, ""));
    }

    private void generatePcode(String f, int l, int a, String s) {
        pcodeList.add(new Pcode(f, l, a, s));
    }

    private void skip(SymbolType... ST) {
        //跳转到ST里面的一个为止，java可变参数，ST是一个数组
        while (currentToken.type != SymbolType.END) {
            for (SymbolType aSt : ST) {
                if (currentToken.type == aSt) {
                    return;
                }
            }
            nextToken();
        }
    }

    private int string2int(String s) {
        System.out.println("string2int: " + s);
        int ans = 0, p = 1, i = 0;
        if (s.charAt(0) == '+') {
            p = 1;
            i++;
        } else if (s.charAt(0) == '-') {
            p = -1;
            i++;
        }
        for (; i < s.length(); i++) {
            ans = ans * 10 + s.charAt(i) - '0';
        }
        return ans * p;
    }

    /**
     * ＜程序＞ ::=  [＜常量说明部分＞][＜变量说明部分＞]｛＜函数定义部分＞｝＜主函数＞
     * 主函数的头部有int和void两种类型，放在两种函数的定义里面判断，只需要判断标识符是否为main即可
     * <program> -> [<constDeclaration>][<variateDeclaration>]{<intFunctionDeclaration>|<voidFunctionDeclaration>}
     */
    private void program() {
        /**
         * 想象上帝调用main：
         * int main(){...}
         * void GOD(){
         *     main();
         *     return;
         * }
         */
        generatePcode(Pcode.INT, 0, 0);//开辟全局变量空间，a之后回填
        generatePcode(Pcode.CALA, 0, 0);//“上帝”调用main函数，开辟新栈，a之后回填
        generatePcode(Pcode.CALB, 0, 0);//指令跳转，a之后回填
        generatePcode(Pcode.OPR, 0, 0);//认为有一个“上帝”在调用main函数，现在返回给上帝了
        varAddress = startOffset;
        nextToken();
        if (currentToken.type == SymbolType.END) {//啥都没有，分析个屁呀
            return;
        }
        if (currentToken.type != SymbolType.CONSTSY && currentToken.type != SymbolType.INTSY && currentToken.type != SymbolType.VOIDSY) {
            error.addErrorMessage(currentToken, 7);//非法符号
            System.out.println(currentToken.line + ": " + 7);
            skip(SymbolType.CONSTSY, SymbolType.INTSY, SymbolType.VOIDSY);//跳转到第一个const int void开始分析
        }

        //处理常量
        if (currentToken.type == SymbolType.CONSTSY) {
            constDeclaration();
        }

        if (currentToken.type != SymbolType.INTSY && currentToken.type != SymbolType.VOIDSY) {
            error.addErrorMessage(currentToken, 7);
            System.out.println(currentToken.line + ": " + 7);
            skip(SymbolType.INTSY, SymbolType.VOIDSY);
        }

        //如果当前读到了int，就提前看看后面的后面的符号，如果不是（，说明是变量定义
        if (currentToken.type == SymbolType.INTSY && tokenList.get(tokenPointer + 1).type != SymbolType.LEFTSMALLBRACKET) {
            //处理变量定义
            variateDeclaration();
        }

        if (currentToken.type != SymbolType.INTSY && currentToken.type != SymbolType.VOIDSY) {
            error.addErrorMessage(currentToken, 7);
            System.out.println(currentToken.line + ": " + 7);
            skip(SymbolType.INTSY, SymbolType.VOIDSY);
        }
        System.out.println("varAddress=" + varAddress);
        pcodeList.get(0).A = varAddress;//回填，设置全局变量空间，栈顶指针指向可以存放数据的第一个位置，从0开始

        //处理函数直到代码结尾
        while (currentToken.type != SymbolType.END) {
            //两个while可以处理函数之间来个 <= 这样的符号
            while (currentToken.type == SymbolType.INTSY || currentToken.type == SymbolType.VOIDSY) {
                returnFlag = false;
                varAddress = startOffset;
                //处理函数部分，根据头部调用intfun和voidfun，函数里面判断main
                if (currentToken.type == SymbolType.INTSY) {
                    if (mainFlag) {
                        error.addErrorMessage(currentToken, 27);
                        System.out.println(currentToken.line + ": " + 27);
                    }
                    intFunctionDeclaration();
                } else {
                    if (mainFlag) {
                        error.addErrorMessage(currentToken, 27);
                        System.out.println(currentToken.line + ": " + 27);
                    }
                    voidFunctionDeclaration();
                }
            }
            if (currentToken.type != SymbolType.END) {
                error.addErrorMessage(currentToken, 7);
                System.out.println(currentToken.line + ": " + 7);
                skip(SymbolType.INTSY, SymbolType.VOIDSY);
            }
        }

        if (!mainFlag) {
            //缺少main函数
            error.addErrorMessage(currentToken, 14);
            System.out.println(currentToken.line + ": " + 14);
        }
    }

    /**
     * ＜常量说明部分＞  ::=  const ＜常量定义＞｛,＜常量定义＞｝;
     * <constDeclaration> -> const ＜constDefine＞ ｛,＜constDefine＞ ｝;
     */
    private void constDeclaration() {
        System.out.println("constDeclaration");
        //program里面判断一定是const
        nextToken();
        constDefine();
        while (currentToken.type == SymbolType.COMMASY) {
            nextToken();
            constDefine();
        }
        if (currentToken.type == SymbolType.SEMISY) {
            nextToken();
        } else {
            int tmp = tokenPointer;
            Token t = currentToken;
            //根据层次跳转，因为不同层后面跟的东西不同
            if (level == 1) {
                //后面是函数定义
                skip(SymbolType.SEMISY, SymbolType.INTSY, SymbolType.VOIDSY);
            } else {
                //后面是句子，句子可以为空，所以有 } 的判断
                skip(SymbolType.SEMISY, SymbolType.IFSY, SymbolType.WHILESY, SymbolType.LEFTBIGBRACKET, SymbolType.RIGHTBIGBRACKET,
                        SymbolType.IDENTIFIER, SymbolType.RETURNSY, SymbolType.SCANFSY, SymbolType.PRINTFSY);
            }
            if (tmp == tokenPointer) {
                error.addErrorMessage(currentToken, 9);
                System.out.println(currentToken.line + ": " + 9);
            } else if (currentToken.type == SymbolType.SEMISY) {
                nextToken();
                error.addErrorMessage(t, 7);
                System.out.println(t.line + ": " + 7);
            } else {
                error.addErrorMessage(t, 7);
                System.out.println(currentToken.line + ": " + 7);
                error.addErrorMessage(currentToken, 9);
                System.out.println(currentToken.line + ": " + 9);
            }
        }
    }

    /**
     * ＜常量定义＞  ::=  ＜标识符＞＝＜整数＞
     * ＜constDefine＞  ::=  ＜标识符＞＝＜整数＞
     */
    private void constDefine() {
        String name, value;
        System.out.println("constDefine");
        //因为在<常量说明部分>会进行跳转，所以这里不处理跳转
        if (currentToken.type == SymbolType.IDENTIFIER) {
            name = currentToken.symbol;
            nextToken();
            if (currentToken.type == SymbolType.ASSIGNSY) {
                nextToken();
                if (currentToken.type == SymbolType.INTEGER) {
                    //插入符号表
                    value = currentToken.value;
                    if (checkTable(name)) {
                        insertTable(name, value, SymbolTableItem.CONST, 0, varAddress++);
                    } else {
                        error.addErrorMessage(currentToken, 28);
                        System.out.println(currentToken.line + ": " + 28);
                    }
                    nextToken();
                } else {
                    //应该是整数
                    error.addErrorMessage(currentToken, 12);
                    System.out.println(currentToken.line + ": " + 12);
                }
            } else {
                //应该是 =
                error.addErrorMessage(currentToken, 11);
                System.out.println(currentToken.line + ": " + 11);
            }
        } else {
            //应该是标识符
            error.addErrorMessage(currentToken, 10);
            System.out.println(currentToken.line + ": " + 10);
        }
    }

    /**
     * 原文法：
     * ＜变量说明部分＞ ::=  ＜声明头部＞｛，＜标识符＞｝；
     * ＜声明头部＞ ::=  int　＜标识符＞
     * <p>
     * 替换后的文法：
     * ＜变量说明部分＞ ::=  int　＜标识符＞ ｛，＜标识符＞｝；
     * <variateDeclaration> -> int ＜标识符＞ ｛，＜标识符＞｝；
     */
    private void variateDeclaration() {
        //跳转这块和符号表对应起来，用level做判断进行跳转
        System.out.println("variateDeclaration");
        nextToken(); //一定是由int进来的
        if (currentToken.type == SymbolType.IDENTIFIER) {
            if (checkTable(currentToken.symbol)) {
                insertTable(currentToken.symbol, currentToken.value, SymbolTableItem.VAR, 0, varAddress++);
            } else {
                error.addErrorMessage(currentToken, 28);
                System.out.println(currentToken.line + ": " + 28);
            }
            nextToken();
            while (currentToken.type == SymbolType.COMMASY) {
                nextToken();
                if (currentToken.type == SymbolType.IDENTIFIER) {
                    if (checkTable(currentToken.symbol)) {
                        insertTable(currentToken.symbol, currentToken.value, SymbolTableItem.VAR, 0, varAddress++);
                    } else {
                        error.addErrorMessage(currentToken, 28);
                        System.out.println(currentToken.line + ": " + 28);
                    }
                    nextToken();
                } else {
                    //此处应该是标识符
                    error.addErrorMessage(currentToken, 10);
                    System.out.println(currentToken.line + ": " + 10);
                    break;
                }
            }
            if (currentToken.type == SymbolType.SEMISY) {
                nextToken();
                return;
            }
            //else，上面有return就没写
        } else {
            //此处应该是标识符
            error.addErrorMessage(currentToken, 10);
            System.out.println(currentToken.line + ": " + 10);
        }

        //3个原本的else统一处理非法符号和缺少；的问题
        Token t = currentToken;
        int tmp = tokenPointer;
        if (level == 1) {
            skip(SymbolType.SEMISY, SymbolType.INTSY, SymbolType.VOIDSY);
        } else {
            skip(SymbolType.SEMISY, SymbolType.IFSY, SymbolType.WHILESY, SymbolType.LEFTBIGBRACKET, SymbolType.RIGHTBIGBRACKET,
                    SymbolType.IDENTIFIER, SymbolType.RETURNSY, SymbolType.SCANFSY, SymbolType.PRINTFSY);
        }
        if (currentToken.type == SymbolType.SEMISY) {
            //后面有；就说明是有非法符号
            nextToken();
            error.addErrorMessage(t, 7);
            System.out.println(t.line + ": " + 7);
        } else if (tokenPointer == tmp) {
            //出错的位置没有向后跳，说明只是少了；
            error.addErrorMessage(currentToken, 9);
            System.out.println(currentToken.line + ": " + 9);
        } else {
            //缺少了；还向后跳转了，说明有非法的符号，就要报两个错
            error.addErrorMessage(t, 7);
            System.out.println(t.line + ": " + 7);
            error.addErrorMessage(currentToken, 9);
            System.out.println(currentToken.line + ": " + 9);
        }
    }

    /**
     * 原文法：
     * ＜函数定义部分＞ ::= （＜声明头部＞｜void ＜标识符＞）＜参数＞＜复合语句＞
     * ＜声明头部＞ ::=  int　＜标识符＞
     * <p>
     * 替换后的文法：
     * ＜函数定义部分＞ ::= （int｜void）＜标识符＞＜参数＞＜复合语句＞
     * 这里实现int类型的函数，因为需要记录返回值
     * <intFunctionDeclaration> -> int ＜标识符＞＜parameters＞＜compoundStatement＞
     */
    private void intFunctionDeclaration() {
        System.out.println("intFunctionDeclaration");
        //program里面判断一定是int
        nextToken();
        if (currentToken.type == SymbolType.IDENTIFIER) {
            if (checkTable(currentToken.symbol)) {
                insertTable(currentToken.symbol, currentToken.value, SymbolTableItem.INTFUN, 0, pcodeList.size());
            } else {
                error.addErrorMessage(currentToken, 28);
                System.out.println(currentToken.line + ": " + 28);
            }
            level = 2;
            nextToken();
            globalSymbolTable.get(globalSymbolTable.size() - 1).parameterNumber = parameter();
            compoundStatement();
            generatePcode(Pcode.OPR, 1, 0);
            level = 1;
        } else if (currentToken.type == SymbolType.MAINSY) {
            if (!mainFlag) {
                insertTable(currentToken.symbol, currentToken.value, SymbolTableItem.INTFUN, 0, pcodeList.size());
                mainFlag = true;
            } else {
                //重复定义main函数
                error.addErrorMessage(currentToken, 20);
                System.out.println(currentToken.line + ": " + 20);
            }
            level = 2;
            nextToken();
            globalSymbolTable.get(globalSymbolTable.size() - 1).parameterNumber = parameter();
            compoundStatement();
            pcodeList.get(1).A = varAddress;//转移栈帧
            pcodeList.get(2).A = globalSymbolTable.get(globalSymbolTable.size() - 1).address;//指令跳转
            generatePcode(Pcode.OPR, 1, 0);//void类型函数自动添加return （栈顶元素）语句
            level = 1;
        } else {
            //缺少函数名
            error.addErrorMessage(currentToken, 22);
            System.out.println(currentToken.line + ": " + 22);
            //跳转到下一个函数的开始
            skip(SymbolType.INTSY, SymbolType.VOIDSY);
            while (currentToken.type != SymbolType.END && tokenList.get(tokenPointer + 1).type != SymbolType.LEFTSMALLBRACKET) {
                skip(SymbolType.INTSY, SymbolType.VOIDSY);
            }
        }
    }

    /**
     * <voidFunctionDeclaration> -> void ＜标识符＞＜parameters＞＜compoundStatement＞
     */
    private void voidFunctionDeclaration() {
        System.out.println("voidFunctionDeclaration");
        //program里面判断一定是void
        nextToken();
        if (currentToken.type == SymbolType.IDENTIFIER) {
            if (checkTable(currentToken.symbol)) {
                insertTable(currentToken.symbol, currentToken.value, SymbolTableItem.VOIDFUN, 0, pcodeList.size());
            } else {
                error.addErrorMessage(currentToken, 28);
                System.out.println(currentToken.line + ": " + 28);
            }
            level = 2;
            nextToken();
            globalSymbolTable.get(globalSymbolTable.size() - 1).parameterNumber = parameter();
            compoundStatement();
            generatePcode(Pcode.OPR, 0, 0);
            level = 1;
        } else if (currentToken.type == SymbolType.MAINSY) {
            if (!mainFlag) {
                insertTable(currentToken.symbol, currentToken.value, SymbolTableItem.VOIDFUN, 0, pcodeList.size());
                mainFlag = true;
            } else {
                //重复定义main函数
                error.addErrorMessage(currentToken, 20);
                System.out.println(currentToken.line + ": " + 20);
            }
            level = 2;
            nextToken();
            globalSymbolTable.get(globalSymbolTable.size() - 1).parameterNumber = parameter();
            compoundStatement();
            pcodeList.get(1).A = varAddress;//main回填
            pcodeList.get(2).A = globalSymbolTable.get(globalSymbolTable.size() - 1).address;
            generatePcode(Pcode.OPR, 0, 0);//void类型函数自动添加return语句
            level = 1;
        } else {
            error.addErrorMessage(currentToken, 22);
            System.out.println(currentToken.line + ": " + 22);
            skip(SymbolType.INTSY, SymbolType.VOIDSY);
            while (currentToken.type != SymbolType.END && tokenList.get(tokenPointer + 1).type != SymbolType.LEFTSMALLBRACKET) {
                skip(SymbolType.INTSY, SymbolType.VOIDSY);
            }
        }
    }

    /**
     * ＜复合语句＞ ::=  ‘{’[＜常量说明部分＞][＜变量说明部分＞]＜语句序列＞‘}’
     * ＜compoundStatement＞ -> ‘{’[＜constDeclaration＞][＜variateDeclaration＞]＜statementList＞‘}’
     */
    private void compoundStatement() {
        System.out.println("compoundStatement");
        if (currentToken.type == SymbolType.LEFTBIGBRACKET) {
            nextToken();
            //处理常量定义
            if (currentToken.type == SymbolType.CONSTSY) {
                constDeclaration();
            }
            //这里不会和函数定义冲突，因为后面是语句，所以直接看是不是int就行
            if (currentToken.type == SymbolType.INTSY) {
                variateDeclaration();
            }
            //语句序列
            statementList();

            //返回值判断放着这里行号准确一点
            if (globalSymbolTable.get(globalSymbolTable.size() - 1).type == 4 && returnFlag) {//void有返回值就GG
                error.addErrorMessage(currentToken, 24);
                System.out.println(currentToken.line + ": " + 24);
            } else if (globalSymbolTable.get(globalSymbolTable.size() - 1).type == 3 && !returnFlag) {//int没有返回值GG
                error.addErrorMessage(currentToken, 23);
                System.out.println(currentToken.line + ": " + 23);
            }

            if (currentToken.type == SymbolType.RIGHTBIGBRACKET) {
                nextToken();
            } else {
                Token t = currentToken;
                int ptr = tokenPointer;
                skip(SymbolType.INTSY, SymbolType.VOIDSY, SymbolType.RIGHTBIGBRACKET);
                if (currentToken.type == SymbolType.RIGHTBIGBRACKET) {
                    error.addErrorMessage(t, 7);
                    System.out.println(t.line + ": " + 7);
                    nextToken();
                } else if (ptr == tokenPointer) {
                    error.addErrorMessage(currentToken, 19);
                    System.out.println(currentToken.line + ": " + 19);
                } else {
                    error.addErrorMessage(t, 7);
                    System.out.println(t.line + ": " + 7);
                    error.addErrorMessage(currentToken, 19);
                    System.out.println(currentToken.line + ": " + 19);
                }
            }
        } else {
            Token t = currentToken;
            int tmp = tokenPointer;
            skip(SymbolType.CONSTSY, SymbolType.INTSY, SymbolType.VOIDSY, SymbolType.IFSY, SymbolType.WHILESY, SymbolType.LEFTBIGBRACKET,
                    SymbolType.RIGHTBIGBRACKET, SymbolType.IDENTIFIER, SymbolType.RETURNSY, SymbolType.SCANFSY, SymbolType.PRINTFSY);
            if (currentToken.type == SymbolType.LEFTBIGBRACKET) {
                nextToken();
                error.addErrorMessage(t, 7);
                System.out.println(t.line + ": " + 7);
            } else if (tmp == tokenPointer) {
                error.addErrorMessage(currentToken, 18);
                System.out.println(currentToken.line + ": " + 18);
            } else {
                error.addErrorMessage(t, 7);
                System.out.println(t.line + ": " + 7);
                error.addErrorMessage(currentToken, 18);
                System.out.println(currentToken.line + ": " + 18);
            }
        }
    }


    /**
     * 原文法
     * ＜参数＞ ::=  ‘(’＜参数表＞‘)’
     * ＜参数表＞ ::=  int ＜标识符＞｛，int ＜标识符＞} | 空
     * <p>
     * 替换后的文法：
     * ＜参数＞ ::=  ‘(’int ＜标识符＞｛，int ＜标识符＞} | 空‘)’
     * <parameter> -> (’int ＜标识符＞｛，int ＜标识符＞} | 空‘)’
     */
    private int parameter() {
        //找到几个参数就写几个参数
        System.out.println("parameter");
        int num = 0;//参数个数返回值
        System.out.println(currentToken.symbol);
        if (currentToken.type == SymbolType.LEFTSMALLBRACKET) {
            nextToken();
            //处理参数表为空
            if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
                nextToken();
                return num;
            }
            //处理非空
            if (currentToken.type == SymbolType.INTSY) {
                nextToken();
                if (currentToken.type == SymbolType.IDENTIFIER) {
                    num++;
                    if (checkTable(currentToken.symbol)) {
                        insertTable(currentToken.symbol, currentToken.value, SymbolTableItem.PARAM, 0, varAddress++);
                    } else {
                        error.addErrorMessage(currentToken, 28);
                        System.out.println(currentToken.line + ": " + 28);
                    }
                    nextToken();
                    System.out.println("wqp is soooooo weak!!");
                    while (currentToken.type == SymbolType.COMMASY) {
                        nextToken();
                        if (currentToken.type == SymbolType.INTSY) {
                            nextToken();
                            if (currentToken.type == SymbolType.IDENTIFIER) {
                                num++;
                                if (checkTable(currentToken.symbol)) {
                                    insertTable(currentToken.symbol, currentToken.value, SymbolTableItem.PARAM, 0, varAddress++);
                                } else {
                                    error.addErrorMessage(currentToken, 28);
                                    System.out.println(currentToken.line + ": " + 28);
                                }
                                nextToken();
                            } else {
                                error.addErrorMessage(currentToken, 10);//
                                System.out.println(currentToken.line + ": " + 10);
                                break;
                            }
                        } else {
                            error.addErrorMessage(currentToken, 13);//
                            System.out.println(currentToken.line + ": " + 13);
                            break;
                        }
                    }
                    System.out.println("currentToken.symbol: " + currentToken.symbol);
                    if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
                        nextToken();
                        return num;
                    } //这里原本有个else放到后面了
                } else {
                    error.addErrorMessage(currentToken, 10);//
                    System.out.println(currentToken.line + ": " + 10);
                }
            } else {
                error.addErrorMessage(currentToken, 13);//
                System.out.println(currentToken.line + ": " + 13);
            }
        } else {
            error.addErrorMessage(currentToken, 16);//
            System.out.println(currentToken.line + ": " + 16);
        }
        //出错之后统一处理
        if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
            //（int）就会进来
            nextToken();
        } else {
            Token t = currentToken;
            int ptr = tokenPointer;
            skip(SymbolType.RIGHTSMALLBRACKET, SymbolType.LEFTBIGBRACKET);
            if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
                error.addErrorMessage(t, 7);
                System.out.println(t.line + ": " + 7);
                nextToken();
            } else if (ptr == tokenPointer) {
                error.addErrorMessage(currentToken, 17);
                System.out.println(currentToken.line + ": " + 17);
            } else {
                error.addErrorMessage(t, 7);
                System.out.println(t.line + ": " + 7);
                error.addErrorMessage(currentToken, 17);
                System.out.println(currentToken.line + ": " + 17);
            }
        }
        return num;
    }


    /**
     * ＜语句序列＞ ::=  ＜语句＞｛＜语句＞｝
     * ＜statementList＞ -> ＜statemennt＞｛＜statemennt＞｝
     * 语句序列的结束一定是 RIGHTBIGBRACKET
     */
    private void statementList() {
        System.out.println("statementList");
        //跳转到语句开始
        //语句序列后面跟着的一定是一个 } ，这里可以判断是否为空以及开头是否合法，不是就GG了
        int ptr = tokenPointer;
        Token t = currentToken;
        skip(SymbolType.IFSY, SymbolType.WHILESY, SymbolType.LEFTBIGBRACKET, SymbolType.IDENTIFIER, SymbolType.RETURNSY,
                SymbolType.SCANFSY, SymbolType.PRINTFSY, SymbolType.RIGHTBIGBRACKET);
        if (ptr != tokenPointer) {
            error.addErrorMessage(t, 7);
            System.out.println(t.line + ": " + 7);
        }
        //不是语句序列的结束或者是新的函数的开始，就一直循环，这样可以跳过句子中间的非法的符号
        //语句序列后面一定是 }
        while (currentToken.type != SymbolType.END && currentToken.type != SymbolType.INTSY &&
                currentToken.type != SymbolType.VOIDSY && currentToken.type != SymbolType.RIGHTBIGBRACKET) {
            //两个句子中间插入一个 <= 等符号咋办。。。
            //会不会想多了，助教应该不会这么为难我们的吧。。。
            while (currentToken.type == SymbolType.IFSY || currentToken.type == SymbolType.WHILESY || currentToken.type == SymbolType.LEFTBIGBRACKET ||
                    currentToken.type == SymbolType.IDENTIFIER || currentToken.type == SymbolType.RETURNSY || currentToken.type == SymbolType.SCANFSY ||
                    currentToken.type == SymbolType.PRINTFSY) {
                statement();
                System.out.println("why at SL: " + currentToken.type.name());
            }
            if (currentToken.type != SymbolType.END && currentToken.type != SymbolType.INTSY &&
                    currentToken.type != SymbolType.VOIDSY && currentToken.type != SymbolType.RIGHTBIGBRACKET) {
                //跳转到句子或者是函数的开始（缺少 } ）
                t = currentToken;
                skip(SymbolType.INTSY, SymbolType.VOIDSY, SymbolType.RIGHTBIGBRACKET, SymbolType.IFSY, SymbolType.WHILESY,
                        SymbolType.LEFTBIGBRACKET, SymbolType.IDENTIFIER, SymbolType.RETURNSY, SymbolType.SCANFSY, SymbolType.PRINTFSY);
                error.addErrorMessage(t, 7);
                System.out.println(t.line + ": " + 7);
            } else {
                System.out.println("why is itttttttt!!!!!!" + currentToken.type.name());
            }
        }
        System.out.println("I am dying~~~~~~~~~~~" + currentToken.type.name());
    }

    /**
     * ＜语句＞ ::= ＜条件语句＞｜＜循环语句＞｜‘{’<语句序列>‘}’｜＜函数调用语句＞;｜＜赋值语句＞;| <返回语句>;｜＜读语句＞;｜＜写语句＞;｜＜空＞
     * <statement> -><ifStatement> | <whileStatement>｜‘{’<statementList>‘}’｜＜callStatement＞;｜＜assignStatement＞; |
     * <returnStatement>;｜＜scanfStatement＞;｜＜printfStatement＞;
     */
    private void statement() {
        System.out.println("statement");
        if (currentToken.type == SymbolType.IFSY) {
            ifStatement();
        } else if (currentToken.type == SymbolType.WHILESY) {
            whileStatement();
        } else if (currentToken.type == SymbolType.LEFTBIGBRACKET) {
            nextToken();
            statementList();
            if (currentToken.type == SymbolType.RIGHTBIGBRACKET) {
                nextToken();
            } else {
                //语句序列后面还可以跟语句，所以跳转到句子的开始或者函数的开始或者 } 结束，}需要下一个
                int ptr = tokenPointer;
                Token t = currentToken;
                skip(SymbolType.INTSY, SymbolType.VOIDSY, SymbolType.RIGHTBIGBRACKET);
                if (currentToken.type == SymbolType.RIGHTBIGBRACKET) {
                    error.addErrorMessage(t, 7);
                    System.out.println(t.line + ": " + 7);
                } else if (ptr == tokenPointer) {
                    error.addErrorMessage(currentToken, 19);
                    System.out.println(currentToken.line + ": " + 19);
                } else {
                    error.addErrorMessage(t, 7);
                    System.out.println(t.line + ": " + 7);
                    error.addErrorMessage(currentToken, 19);
                    System.out.println(currentToken.line + ": " + 19);
                }
            }
        } else if (currentToken.type == SymbolType.IDENTIFIER) {
            SymbolTableItem item = searchTable(currentToken.symbol);
            if (item != null) {
                if (item.type == SymbolTableItem.INTFUN || item.type == SymbolTableItem.VOIDFUN) {
                    //如果是函数表项
                    callStatement();
                } else if (item.type == SymbolTableItem.VAR || item.type == SymbolTableItem.PARAM) {
                    //变量 或者 参数
                    assignStatement();
                } else {
                    error.addErrorMessage(currentToken, 29);
                    System.out.println(currentToken.line + ": " + 29);
                    nextToken(); //一定有非法的单词，就跳转到下一句话的开始就好了,或者是语句块的结束
                    skip(SymbolType.INTSY, SymbolType.VOIDSY, SymbolType.RIGHTBIGBRACKET, SymbolType.SEMISY, SymbolType.IFSY, SymbolType.WHILESY,
                            SymbolType.LEFTBIGBRACKET, SymbolType.IDENTIFIER, SymbolType.RETURNSY, SymbolType.SCANFSY, SymbolType.PRINTFSY);
                    if (currentToken.type == SymbolType.SEMISY) {
                        nextToken();
                    }
                    return;
                }
                System.out.println("after IDEN statement: " + currentToken.symbol);
                if (currentToken.type == SymbolType.SEMISY) {
                    nextToken();
                } else {
                    int ptr = tokenPointer;
                    Token t = currentToken;
                    skip(SymbolType.INTSY, SymbolType.VOIDSY, SymbolType.RIGHTBIGBRACKET, SymbolType.SEMISY, SymbolType.IFSY, SymbolType.WHILESY,
                            SymbolType.LEFTBIGBRACKET, SymbolType.IDENTIFIER, SymbolType.RETURNSY, SymbolType.SCANFSY, SymbolType.PRINTFSY);
                    if (currentToken.type == SymbolType.SEMISY) {
                        nextToken();
                        error.addErrorMessage(t, 7);
                        System.out.println(t.line + ": " + 7);
                    } else if (ptr == tokenPointer) {
                        error.addErrorMessage(currentToken, 9);
                        System.out.println(currentToken.line + ": " + 9);
                    } else {
                        error.addErrorMessage(t, 7);
                        System.out.println(t.line + ": " + 7);
                        error.addErrorMessage(currentToken, 9);
                        System.out.println(currentToken.line + ": " + 9);
                    }
                }
            } else {
                //标识符未定义
                error.addErrorMessage(currentToken, 30);
                System.out.println(currentToken.line + ": " + 30);
                nextToken();
                skip(SymbolType.INTSY, SymbolType.VOIDSY, SymbolType.RIGHTBIGBRACKET, SymbolType.SEMISY, SymbolType.IFSY, SymbolType.WHILESY,
                        SymbolType.LEFTBIGBRACKET, SymbolType.IDENTIFIER, SymbolType.RETURNSY, SymbolType.SCANFSY, SymbolType.PRINTFSY);
                if (currentToken.type == SymbolType.SEMISY) {
                    nextToken();
                }
            }
        } else if (currentToken.type == SymbolType.RETURNSY) {
            returnStatement();
            if (currentToken.type == SymbolType.SEMISY) {
                nextToken();
            } else {
                int ptr = tokenPointer;
                Token t = currentToken;
                skip(SymbolType.INTSY, SymbolType.VOIDSY, SymbolType.RIGHTBIGBRACKET, SymbolType.SEMISY, SymbolType.IFSY, SymbolType.WHILESY,
                        SymbolType.LEFTBIGBRACKET, SymbolType.IDENTIFIER, SymbolType.RETURNSY, SymbolType.SCANFSY, SymbolType.PRINTFSY);
                if (currentToken.type == SymbolType.SEMISY) {
                    nextToken();
                    error.addErrorMessage(t, 7);
                    System.out.println(t.line + ": " + 7);
                } else if (ptr == tokenPointer) {
                    error.addErrorMessage(currentToken, 9);
                    System.out.println(currentToken.line + ": " + 9);
                } else {
                    error.addErrorMessage(t, 7);
                    System.out.println(t.line + ": " + 7);
                    error.addErrorMessage(currentToken, 9);
                    System.out.println(currentToken.line + ": " + 9);
                }
            }
        } else if (currentToken.type == SymbolType.SCANFSY) {
            scanfStatement();
            if (currentToken.type == SymbolType.SEMISY) {
                nextToken();
            } else {
                int ptr = tokenPointer;
                Token t = currentToken;
                skip(SymbolType.INTSY, SymbolType.VOIDSY, SymbolType.RIGHTBIGBRACKET, SymbolType.SEMISY, SymbolType.IFSY, SymbolType.WHILESY,
                        SymbolType.LEFTBIGBRACKET, SymbolType.IDENTIFIER, SymbolType.RETURNSY, SymbolType.SCANFSY, SymbolType.PRINTFSY);
                if (currentToken.type == SymbolType.SEMISY) {
                    nextToken();
                    error.addErrorMessage(t, 7);
                    System.out.println(t.line + ": " + 7);
                } else if (ptr == tokenPointer) {
                    error.addErrorMessage(currentToken, 9);
                    System.out.println(currentToken.line + ": " + 9);
                } else {
                    error.addErrorMessage(t, 7);
                    System.out.println(t.line + ": " + 7);
                    error.addErrorMessage(currentToken, 9);
                    System.out.println(currentToken.line + ": " + 9);
                }
            }
        } else if (currentToken.type == SymbolType.PRINTFSY) {
            //下面两句话保留一个
            //printfStatement();//按照文法来
            printfStatementA();//按照C语言的习惯来
            if (currentToken.type == SymbolType.SEMISY) {
                nextToken();
            } else {
                int ptr = tokenPointer;
                Token t = currentToken;
                skip(SymbolType.INTSY, SymbolType.VOIDSY, SymbolType.RIGHTBIGBRACKET, SymbolType.SEMISY, SymbolType.IFSY, SymbolType.WHILESY,
                        SymbolType.LEFTBIGBRACKET, SymbolType.IDENTIFIER, SymbolType.RETURNSY, SymbolType.SCANFSY, SymbolType.PRINTFSY);
                if (currentToken.type == SymbolType.SEMISY) {
                    nextToken();
                    error.addErrorMessage(t, 7);
                    System.out.println(t.line + ": " + 7);
                } else if (ptr == tokenPointer) {
                    error.addErrorMessage(currentToken, 9);
                    System.out.println(currentToken.line + ": " + 9);
                } else {
                    error.addErrorMessage(t, 7);
                    System.out.println(t.line + ": " + 7);
                    error.addErrorMessage(currentToken, 9);
                    System.out.println(currentToken.line + ": " + 9);
                }
            }
        } else { //不合法的句子，在if或者while里面调用的时候就可能跳过来，比如: while(1>0) 666
            error.addErrorMessage(currentToken, 7);
            System.out.println(currentToken.line + ": " + 7);
            skip(SymbolType.IFSY, SymbolType.WHILESY, SymbolType.LEFTBIGBRACKET, SymbolType.IDENTIFIER, SymbolType.RETURNSY,
                    SymbolType.SCANFSY, SymbolType.PRINTFSY, SymbolType.INTSY, SymbolType.VOIDSY, SymbolType.RIGHTBIGBRACKET);
        }
    }

    /**
     * ＜表达式＞ ::=  [＋｜－]＜项＞｛＜加法运算符＞＜项＞｝
     * <expression> -> [+|-]<term>{(+|-)<term>}
     */
    private void expression() {
        System.out.println("expression");
        SymbolType type = SymbolType.PLUSSY;
        if (currentToken.type == SymbolType.PLUSSY || currentToken.type == SymbolType.MINUSSY) {
            type = currentToken.type;
            nextToken();
        }
        term();
        if (type == SymbolType.MINUSSY) {
            generatePcode(Pcode.OPR, 0, 1);
        }
        while (currentToken.type == SymbolType.PLUSSY || currentToken.type == SymbolType.MINUSSY) {
            type = currentToken.type;
            nextToken();
            term();
            if (type == SymbolType.PLUSSY) {
                generatePcode(Pcode.OPR, 0, 2);
            } else {
                generatePcode(Pcode.OPR, 0, 3);
            }
        }
        System.out.println("end of expression: " + currentToken.symbol);
    }

    /**
     * ＜项＞ ::=  ＜因子＞{＜乘法运算符＞＜因子＞}
     * <term> -> <factor>{(*|/) <factor>}
     */
    private void term() {
        System.out.println("term");
        factor();
        while (currentToken.type == SymbolType.MULTIPLYSY || currentToken.type == SymbolType.DIVIDSY) {
            SymbolType type = currentToken.type;
            nextToken();
            factor();
            if (type == SymbolType.MULTIPLYSY) {
                generatePcode(Pcode.OPR, 0, 4);
            } else {
                generatePcode(Pcode.OPR, 0, 5);
            }
        }
    }

    /**
     * ＜因子＞ ::=  ＜标识符＞｜‘（’＜表达式＞‘）’｜＜整数＞｜＜函数调用语句＞
     * <factor> -> ＜标识符＞｜‘（’＜expression＞‘）’｜＜整数＞｜＜callStatement＞
     */
    private void factor() {
        System.out.println("factor");
        if (currentToken.type != SymbolType.IDENTIFIER && currentToken.type != SymbolType.LEFTSMALLBRACKET && currentToken.type != SymbolType.INTEGER) {
            error.addErrorMessage(currentToken, 26);
            System.out.println(currentToken.line + ": " + 26);
            System.out.println(currentToken.symbol);
            //按照文法跳转到表达式后面可能出现的所有情况
            skip(SymbolType.WHILESY, SymbolType.LEFTBIGBRACKET, SymbolType.RETURNSY, SymbolType.SCANFSY, SymbolType.PRINTFSY, SymbolType.IDENTIFIER,
                    SymbolType.IFSY, SymbolType.RIGHTBIGBRACKET, SymbolType.RIGHTSMALLBRACKET, SymbolType.COMMASY, SymbolType.SEMISY, SymbolType.GREATERSY,
                    SymbolType.GREATEROREQUALSY, SymbolType.LESSSY, SymbolType.LESSOREQUALSY, SymbolType.EQUALSY, SymbolType.NOTEQUALSY);
        } else if (currentToken.type == SymbolType.IDENTIFIER) {
            //标识符这里已经可以开始查表了
            SymbolTableItem item = searchTable(currentToken.symbol);
            if (item != null) {
                if (item.type == SymbolTableItem.CONST) {//常量
                    generatePcode(Pcode.LIT, 0, string2int(item.value));
                    nextToken();
                } else if (item.type == SymbolTableItem.VAR || item.type == SymbolTableItem.PARAM) {//变量 参数
                    if (isParameter) {
                        //因为这个时候栈帧已经改变了，base指向调用函数基址，所以必须用 2 表示当前调用者的变量 or 0 表示全局变量
                        generatePcode(Pcode.LOD, 2 * (item.level - 1), item.address);
                    } else {
                        generatePcode(Pcode.LOD, item.level - 1, item.address);
                    }
                    nextToken();
                } else if (item.type == SymbolTableItem.INTFUN) {//int函数，返回值自动在栈顶
                    callStatement();
                } else if (item.type == SymbolTableItem.VOIDFUN) {
                    callStatement();
                    error.addErrorMessage(currentToken, 32);
                    System.out.println(currentToken.line + ": " + 32);
                }
            } else {
                error.addErrorMessage(currentToken, 30);
                System.out.println(currentToken.line + ": " + 30);
                nextToken();
                skip(SymbolType.WHILESY, SymbolType.LEFTBIGBRACKET, SymbolType.RETURNSY, SymbolType.SCANFSY, SymbolType.PRINTFSY, SymbolType.IDENTIFIER,
                        SymbolType.IFSY, SymbolType.RIGHTBIGBRACKET, SymbolType.RIGHTSMALLBRACKET, SymbolType.COMMASY, SymbolType.SEMISY, SymbolType.GREATERSY,
                        SymbolType.GREATEROREQUALSY, SymbolType.LESSSY, SymbolType.LESSOREQUALSY, SymbolType.EQUALSY, SymbolType.NOTEQUALSY);
            }
        } else if (currentToken.type == SymbolType.INTEGER) {
            generatePcode(Pcode.LIT, 0, string2int(currentToken.symbol));
            nextToken();
        } else {// (
            nextToken();
            expression();//表达式计算完结果就在栈顶
            if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
                nextToken();
            } else {
                error.addErrorMessage(currentToken, 17);
                System.out.println(currentToken.line + ": " + 17);
                //表达式的情况有点多，就跳转到下句话的开始 or 表达式的后面可能跟的）；
                skip(SymbolType.IFSY, SymbolType.WHILESY, SymbolType.LEFTBIGBRACKET, SymbolType.RETURNSY, SymbolType.SCANFSY, SymbolType.IDENTIFIER,
                        SymbolType.PRINTFSY, SymbolType.SEMISY, SymbolType.RIGHTBIGBRACKET, SymbolType.RIGHTSMALLBRACKET);
            }
        }
    }

    /**
     * ＜赋值语句＞ ::=  ＜标识符＞＝＜表达式＞
     * ＜assignStatement＞ -> ＜标识符＞＝＜expression＞
     */
    private void assignStatement() {
        //statement里面判断了，一定是变量或者参数
        System.out.println("assignStatement");
        SymbolTableItem item = searchTable(currentToken.symbol);
        if (item == null) {
            return;
        }
        nextToken();
        if (currentToken.type == SymbolType.ASSIGNSY) {
            nextToken();
            expression();
            generatePcode(Pcode.STO, item.level - 1, item.address);
        } else {
            error.addErrorMessage(currentToken, 11);
            System.out.println(currentToken.line + ": " + 11);
            skip(SymbolType.IFSY, SymbolType.WHILESY, SymbolType.LEFTBIGBRACKET, SymbolType.IDENTIFIER, SymbolType.RETURNSY,
                    SymbolType.SCANFSY, SymbolType.PRINTFSY, SymbolType.SEMISY, SymbolType.RIGHTBIGBRACKET);
        }
    }

    /**
     * ＜条件语句＞ ::=  if‘（’＜条件＞‘）’＜语句＞〔else＜语句＞〕
     * ＜ifStatement＞ -> if‘（’＜condition＞‘）’＜statement＞ [else＜statement＞]
     */
    private void ifStatement() {
        System.out.println("ifStatement");
        //statement调用，所以一定是if
        nextToken();
        if (currentToken.type == SymbolType.LEFTSMALLBRACKET) {
            nextToken();
            condition();
            int pre = pcodeList.size();//
            if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
                nextToken();
                generatePcode(Pcode.JPC, 0, 0);//a之后回填
                statement();
                pcodeList.get(pre).A = pcodeList.size();//回填JPC
                if (currentToken.type == SymbolType.ELSESY) {
                    pcodeList.get(pre).A++;
                    pre = pcodeList.size();
                    generatePcode(Pcode.JMP, 0, 0);//a之后回填
                    nextToken();
                    statement();
                    pcodeList.get(pre).A = pcodeList.size();
                }
            } else {
                int ptr = tokenPointer;
                Token t = currentToken;
                skip(SymbolType.IFSY, SymbolType.WHILESY, SymbolType.LEFTBIGBRACKET, SymbolType.IDENTIFIER, SymbolType.RETURNSY,
                        SymbolType.SCANFSY, SymbolType.PRINTFSY, SymbolType.SEMISY, SymbolType.RIGHTSMALLBRACKET);
                if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
                    error.addErrorMessage(t, 7);
                    System.out.println(t.line + ": " + 7);
                    nextToken();
                } else if (ptr == tokenPointer) {
                    error.addErrorMessage(currentToken, 17);
                    System.out.println(currentToken.line + ": " + 17);
                } else {
                    error.addErrorMessage(t, 7);
                    System.out.println(t.line + ": " + 7);
                    error.addErrorMessage(currentToken, 17);
                    System.out.println(currentToken.line + ": " + 17);
                }
                if (currentToken.type == SymbolType.RIGHTSMALLBRACKET || currentToken.type == SymbolType.SEMISY) {
                    nextToken();
                }
            }
        } else {
            error.addErrorMessage(currentToken, 16);
            System.out.println(currentToken.line + ": " + 16);
            skip(SymbolType.IFSY, SymbolType.WHILESY, SymbolType.LEFTBIGBRACKET, SymbolType.IDENTIFIER, SymbolType.RETURNSY,
                    SymbolType.SCANFSY, SymbolType.PRINTFSY, SymbolType.RIGHTSMALLBRACKET, SymbolType.SEMISY);
            if (currentToken.type == SymbolType.RIGHTSMALLBRACKET || currentToken.type == SymbolType.SEMISY) {
                nextToken();
            }
        }
    }

    /**
     * ＜条件＞ ::=  ＜表达式＞＜关系运算符＞＜表达式＞｜＜表达式＞
     * ＜condition＞ -> ＜expression＞＜关系运算符＞＜expression＞｜＜expression＞
     */
    private void condition() {
        System.out.println("condition");
        expression();
        System.out.println(currentToken.symbol);
        if (currentToken.type == SymbolType.LESSSY || currentToken.type == SymbolType.GREATERSY || currentToken.type == SymbolType.LESSOREQUALSY ||
                currentToken.type == SymbolType.GREATEROREQUALSY || currentToken.type == SymbolType.EQUALSY || currentToken.type == SymbolType.NOTEQUALSY) {
            SymbolType type = currentToken.type;
            nextToken();
            expression();
            if (type == SymbolType.EQUALSY) { // ==
                generatePcode(Pcode.OPR, 0, 7);
            } else if (type == SymbolType.NOTEQUALSY) { // !=
                generatePcode(Pcode.OPR, 0, 8);
            } else if (type == SymbolType.LESSSY) {// <
                generatePcode(Pcode.OPR, 0, 9);
            } else if (type == SymbolType.GREATEROREQUALSY) {// >=
                generatePcode(Pcode.OPR, 0, 10);
            } else if (type == SymbolType.GREATERSY) { // >
                generatePcode(Pcode.OPR, 0, 11);
            } else { // <=
                generatePcode(Pcode.OPR, 0, 12);
            }
        }
    }

    /**
     * ＜循环语句＞ ::=  while‘（’＜条件＞‘）’＜语句＞
     * ＜whileStatement＞ -> while‘（’＜condition＞‘）’＜statement＞
     */
    private void whileStatement() {
        System.out.println("whileStatement");
        //statement调用，所以一定是while
        nextToken();
        if (currentToken.type == SymbolType.LEFTSMALLBRACKET) {
            nextToken();
            int p1 = pcodeList.size();
            condition();
            int p2 = pcodeList.size();
            generatePcode(Pcode.JPC, 0, 0);//a=0之后回填
            if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
                nextToken();
                statement();
                generatePcode(Pcode.JMP, 0, p1);
                pcodeList.get(p2).A = pcodeList.size();
            } else {
                int ptr = tokenPointer;
                Token t = currentToken;
                skip(SymbolType.IFSY, SymbolType.WHILESY, SymbolType.LEFTBIGBRACKET, SymbolType.IDENTIFIER, SymbolType.RETURNSY,
                        SymbolType.SCANFSY, SymbolType.PRINTFSY, SymbolType.SEMISY, SymbolType.RIGHTSMALLBRACKET);
                if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
                    error.addErrorMessage(t, 7);
                    System.out.println(t.line + ": " + 7);
                    nextToken();
                } else if (ptr == tokenPointer) {
                    error.addErrorMessage(currentToken, 17);
                    System.out.println(currentToken.line + ": " + 17);
                } else {
                    error.addErrorMessage(t, 7);
                    System.out.println(t.line + ": " + 7);
                    error.addErrorMessage(currentToken, 17);
                    System.out.println(currentToken.line + ": " + 17);
                }
                if (currentToken.type == SymbolType.RIGHTSMALLBRACKET || currentToken.type == SymbolType.SEMISY) {
                    nextToken();
                }
            }
        } else {
            error.addErrorMessage(currentToken, 16);
            System.out.println(currentToken.line + ": " + 16);
            skip(SymbolType.IFSY, SymbolType.WHILESY, SymbolType.LEFTBIGBRACKET, SymbolType.IDENTIFIER, SymbolType.RETURNSY,
                    SymbolType.SCANFSY, SymbolType.PRINTFSY, SymbolType.RIGHTSMALLBRACKET, SymbolType.SEMISY);
            if (currentToken.type == SymbolType.RIGHTSMALLBRACKET || currentToken.type == SymbolType.SEMISY) {
                nextToken();
            }
        }
    }

    /**
     * ＜函数调用语句＞ ::=  ＜标识符＞‘（’＜值参数表＞‘）’
     * ＜callStatement＞ -> ＜标识符＞‘（’＜valueParameter＞‘）’
     */
    private void callStatement() {
        System.out.println("callStatement");
        ArrayList<SymbolTableItem> LT = localSymbolTable.get(currentToken.symbol);
        SymbolTableItem item = searchTable(currentToken.symbol);
        if (item == null) {
            return;
        }
        nextToken();
        if (currentToken.type == SymbolType.LEFTSMALLBRACKET) {
            nextToken();
            if (LT == null) {
                generatePcode(Pcode.CALA, 0, startOffset);
            } else {
                generatePcode(Pcode.CALA, 0, LT.get(LT.size() - 1).address + 1);
            }
            isParameter = true;
            int parNum = valueParameter();
            isParameter = false;
            generatePcode(Pcode.CALB, 0, item.address);
            if (item.parameterNumber != parNum) {
                error.addErrorMessage(currentToken, 31);
                System.out.println(currentToken.line + ": " + 31);
            }
            if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
                nextToken();
            } else {
                int ptr = tokenPointer;
                Token t = currentToken;
                skip(SymbolType.IFSY, SymbolType.WHILESY, SymbolType.LEFTBIGBRACKET, SymbolType.IDENTIFIER, SymbolType.RETURNSY,
                        SymbolType.SCANFSY, SymbolType.PRINTFSY, SymbolType.RIGHTSMALLBRACKET, SymbolType.SEMISY);
                if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
                    error.addErrorMessage(t, 7);
                    System.out.println(t.line + ": " + 7);
                    nextToken();
                } else if (ptr == tokenPointer) {
                    error.addErrorMessage(currentToken, 17);
                    System.out.println(currentToken.line + ": " + 17);
                } else {
                    error.addErrorMessage(t, 7);
                    System.out.println(t.line + ": " + 7);
                    error.addErrorMessage(currentToken, 17);
                    System.out.println(currentToken.line + ": " + 17);
                }
            }
        } else {
            error.addErrorMessage(currentToken, 16);
            System.out.println(currentToken.line + ": " + 16);
            skip(SymbolType.IFSY, SymbolType.WHILESY, SymbolType.LEFTBIGBRACKET, SymbolType.IDENTIFIER, SymbolType.RETURNSY,
                    SymbolType.SCANFSY, SymbolType.PRINTFSY, SymbolType.RIGHTSMALLBRACKET, SymbolType.SEMISY);
            if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
                nextToken();
            }
        }
    }

    /**
     * ＜值参数表＞ ::=  ＜表达式＞｛，＜表达式＞｝｜＜空＞
     * ＜valueParameter＞ -> ＜expression＞｛，＜expression＞｝｜＜空＞
     */
    private int valueParameter() {
        int num = 0, add = startOffset;
        System.out.println("valueParameter");
        //只是在函数调用语句里面使用，所以空的釉面一定是 ）
        if (currentToken.type == SymbolType.RIGHTSMALLBRACKET || currentToken.type == SymbolType.SEMISY) {
            //fuck(; 也认为是没有参数
            return num;
        }
        expression();
        generatePcode(Pcode.STO, 1, add++);
        num++;
        while (currentToken.type == SymbolType.COMMASY) {
            nextToken();
            expression();
            generatePcode(Pcode.STO, 1, add++);
            num++;
        }
        return num;
    }

    /**
     * ＜读语句＞ ::=  scanf‘(’＜标识符＞‘）’;
     * ＜scanfStatement＞ -> scanf‘(’＜标识符＞‘）’
     */
    private void scanfStatement() {
        System.out.println("scanfStatement");
        //statement里面调用的，一定是scanf
        nextToken();
        if (currentToken.type == SymbolType.LEFTSMALLBRACKET) {
            nextToken();
            if (currentToken.type == SymbolType.IDENTIFIER) {
                SymbolTableItem item = searchTable(currentToken.symbol);
                if (item != null) {
                    if (item.type == SymbolTableItem.CONST || item.type == SymbolTableItem.INTFUN || item.type == SymbolTableItem.VOIDFUN) {
                        error.addErrorMessage(currentToken, 33);
                        System.out.println(currentToken.line + ": " + 33);
                    } else {
                        generatePcode(Pcode.RED, item.level - 1, item.address);
                    }
                    nextToken();
                    if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
                        nextToken();
                        return;
                    }
                } else {
                    error.addErrorMessage(currentToken, 30);
                    System.out.println(currentToken.line + ": " + 30);
                    nextToken();//跳过当前没有定义的标识符
                }
            } else {
                error.addErrorMessage(currentToken, 10);
                System.out.println(currentToken.line + ": " + 10);
            }
        } else { //这里可能还需要改一下
            error.addErrorMessage(currentToken, 16);
            System.out.println(currentToken.line + ": " + 16);
            skip(SymbolType.IFSY, SymbolType.WHILESY, SymbolType.LEFTBIGBRACKET, SymbolType.IDENTIFIER, SymbolType.RETURNSY,
                    SymbolType.SCANFSY, SymbolType.PRINTFSY, SymbolType.RIGHTSMALLBRACKET, SymbolType.SEMISY);
            if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
                nextToken();
            }
        }

        //很多else放在一起，跳转到scanf结束
        int ptr = tokenPointer;
        Token t = currentToken;
        skip(SymbolType.IFSY, SymbolType.WHILESY, SymbolType.LEFTBIGBRACKET, SymbolType.IDENTIFIER, SymbolType.RETURNSY,
                SymbolType.SCANFSY, SymbolType.PRINTFSY, SymbolType.RIGHTSMALLBRACKET, SymbolType.SEMISY);
        if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
            error.addErrorMessage(t, 7);
            System.out.println(t.line + ": " + 7);
            nextToken();
        } else if (ptr == tokenPointer) {
            error.addErrorMessage(currentToken, 17);
            System.out.println(currentToken.line + ": " + 17);
        } else {
            error.addErrorMessage(t, 7);
            System.out.println(t.line + ": " + 7);
            error.addErrorMessage(currentToken, 17);
            System.out.println(currentToken.line + ": " + 17);
        }
    }

    /**
     * ＜写语句＞ ::=  printf‘(’[<字符串>,][＜表达式＞]‘）’
     * <printfStatement> -> printf‘(’[<字符串>,][＜expression＞]‘）’
     */
    private void printfStatement() {
        System.out.println("printfStatement");
        //statement里面调用的，一定是printf
        nextToken();
        if (currentToken.type == SymbolType.LEFTSMALLBRACKET) {
            nextToken();
            if (currentToken.type == SymbolType.STRING) {
                generatePcode(Pcode.WRTS, 0, 0, currentToken.symbol);
                nextToken();
                System.out.println(currentToken.type.name());
                if (currentToken.type == SymbolType.COMMASY) {
                    nextToken();
                    if (currentToken.type == SymbolType.PLUSSY || currentToken.type == SymbolType.MINUSSY || currentToken.type == SymbolType.LEFTSMALLBRACKET ||
                            currentToken.type == SymbolType.IDENTIFIER || currentToken.type == SymbolType.INTEGER) {
                        expression();
                        generatePcode(Pcode.WRTI, 0, 0);
                    }
                    if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
                        nextToken();
                        return;
                    }
                } else if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
                    error.addErrorMessage(currentToken, 25);
                    System.out.println(currentToken.line + ": " + 25);
                    nextToken();
                    return;
                } else {
                    error.addErrorMessage(currentToken, 25);
                    System.out.println(currentToken.line + ": " + 25);
                }
            } else if (currentToken.type == SymbolType.PLUSSY || currentToken.type == SymbolType.MINUSSY || currentToken.type == SymbolType.LEFTSMALLBRACKET ||
                    currentToken.type == SymbolType.IDENTIFIER || currentToken.type == SymbolType.INTEGER) {
                //如果是表达式的first集合就进行表达式的处理
                expression();
                generatePcode(Pcode.WRTI, 0, 0);
                if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
                    nextToken();
                    return;
                } //else
            } else if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
                //里面可以是空
                nextToken();
                return;
            } //else
        } else {
            error.addErrorMessage(currentToken, 16);
            System.out.println(currentToken.line + ": " + 16);
        }
        //很多else放在一起处理，统一跳转
        int ptr = tokenPointer;
        Token t = currentToken;
        skip(SymbolType.IFSY, SymbolType.WHILESY, SymbolType.LEFTBIGBRACKET, SymbolType.IDENTIFIER, SymbolType.RETURNSY,
                SymbolType.SCANFSY, SymbolType.PRINTFSY, SymbolType.RIGHTSMALLBRACKET, SymbolType.SEMISY);
        if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
            error.addErrorMessage(t, 7);
            System.out.println(t.line + ": " + 7);
            nextToken();
        } else if (ptr == tokenPointer) {
            error.addErrorMessage(currentToken, 17);
            System.out.println(currentToken.line + ": " + 17);
        } else {
            error.addErrorMessage(t, 7);
            System.out.println(t.line + ": " + 7);
            error.addErrorMessage(currentToken, 17);
            System.out.println(currentToken.line + ": " + 17);
        }
    }

    //按照C语言的处理习惯进行处理，即，链接两个参数，原文法错误
    private void printfStatementA() {
        System.out.println("printfStatementA");
        //statement里面调用的，一定是printf
        nextToken();
        if (currentToken.type == SymbolType.LEFTSMALLBRACKET) {
            nextToken();
            if (currentToken.type == SymbolType.STRING) {
                generatePcode(Pcode.WRTS, 0, 0, currentToken.symbol);
                nextToken();
                if (currentToken.type == SymbolType.COMMASY) {
                    nextToken();
                    expression();
                    generatePcode(Pcode.WRTI, 0, 0);
                    if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
                        nextToken();
                        return;
                    }
                } else if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
                    nextToken();
                    return;
                }
            } else if (currentToken.type == SymbolType.PLUSSY || currentToken.type == SymbolType.MINUSSY || currentToken.type == SymbolType.LEFTSMALLBRACKET ||
                    currentToken.type == SymbolType.IDENTIFIER || currentToken.type == SymbolType.INTEGER) {
                //如果是表达式的first集合就进行表达式的处理
                expression();
                generatePcode(Pcode.WRTI, 0, 0);
                if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
                    nextToken();
                    return;
                } //else
            } else if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
                //里面可以是空
                nextToken();
                return;
            } //else
        } else {
            error.addErrorMessage(currentToken, 16);
            System.out.println(currentToken.line + ": " + 16);
        }
        //很多else放在一起处理，统一跳转 )
        int ptr = tokenPointer;
        Token t = currentToken;
        skip(SymbolType.IFSY, SymbolType.WHILESY, SymbolType.LEFTBIGBRACKET, SymbolType.IDENTIFIER, SymbolType.RETURNSY,
                SymbolType.SCANFSY, SymbolType.PRINTFSY, SymbolType.RIGHTSMALLBRACKET, SymbolType.SEMISY);
        if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
            error.addErrorMessage(t, 7);
            System.out.println(t.line + ": " + 7);
            nextToken();
        } else if (ptr == tokenPointer) {
            error.addErrorMessage(currentToken, 17);
            System.out.println(currentToken.line + ": " + 17);
        } else {
            error.addErrorMessage(t, 7);
            System.out.println(t.line + ": " + 7);
            error.addErrorMessage(currentToken, 17);
            System.out.println(currentToken.line + ": " + 17);
        }
    }

    /**
     * ＜返回语句＞ ::=  return [ ‘(’＜表达式＞’)’ ]
     * ＜returnStatement＞ -> return [ ‘(’＜expression＞’)’ ]
     */
    private void returnStatement() {
        System.out.println("returnStatement");
        //statement里面调用的，一定是return
        System.out.println(currentToken.type.name());
        nextToken();
        System.out.println(currentToken.type.name());
        if (currentToken.type == SymbolType.LEFTSMALLBRACKET) {
            returnFlag = true;
            nextToken();
            expression();
            generatePcode(Pcode.OPR, 1, 0);
            if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
                nextToken();
            } else {
                int ptr = tokenPointer;
                Token t = currentToken;
                skip(SymbolType.IFSY, SymbolType.WHILESY, SymbolType.LEFTBIGBRACKET, SymbolType.IDENTIFIER, SymbolType.RETURNSY,
                        SymbolType.SCANFSY, SymbolType.PRINTFSY, SymbolType.RIGHTSMALLBRACKET, SymbolType.SEMISY);
                if (currentToken.type == SymbolType.RIGHTSMALLBRACKET) {
                    error.addErrorMessage(t, 7);
                    System.out.println(t.line + ": " + 7);
                    nextToken();
                } else if (ptr == tokenPointer) {
                    error.addErrorMessage(currentToken, 17);
                    System.out.println(currentToken.line + ": " + 17);
                } else {
                    error.addErrorMessage(t, 7);
                    System.out.println(t.line + ": " + 7);
                    error.addErrorMessage(currentToken, 17);
                    System.out.println(currentToken.line + ": " + 17);
                }
            }
        } else if (currentToken.type == SymbolType.SEMISY) {
            System.out.println("oh I love C0 sooooooo muck!");
            generatePcode(Pcode.OPR, 0, 0);
        } else {
            int ptr = tokenPointer;
            Token t = currentToken;
            skip(SymbolType.IFSY, SymbolType.WHILESY, SymbolType.LEFTBIGBRACKET, SymbolType.IDENTIFIER,
                    SymbolType.RETURNSY, SymbolType.SCANFSY, SymbolType.PRINTFSY, SymbolType.SEMISY, SymbolType.RIGHTBIGBRACKET);
            if (currentToken.type == SymbolType.SEMISY) {
                error.addErrorMessage(t, 7);
                System.out.println(t.line + ": " + 7);
                nextToken();
            } else if (ptr == tokenPointer) {
                error.addErrorMessage(currentToken, 9);
                System.out.println(currentToken.line + ": " + 9);
            } else {
                error.addErrorMessage(t, 7);
                System.out.println(t.line + ": " + 7);
                error.addErrorMessage(currentToken, 9);
                System.out.println(currentToken.line + ": " + 9);
            }
        }
    }
}