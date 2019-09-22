import java.util.ArrayList;

public class Lexer {
    private String[] keywords = {"const", "int", "void", "if", "else", "while", "main", "return", "printf", "scanf"};
    private String code;//传递过来的源代码
    private ArrayList<Token> tokenList;
    private Error error;

    public Lexer(String code) {
        System.out.println("开始lexer：");
        this.code = code;
        System.out.println(code);
        this.tokenList = new ArrayList<Token>();
        this.error = new Error();
        this.analysis();
    }

    public ArrayList<Token> getTokenList() {
        return tokenList;
    }

    public void analysis() {
//        System.out.println("#"+code.charAt(0)+"#");
        int n = code.length();
//        System.out.println(n);
//        System.out.println(code);
        int i = 0;
        int line = 1;
        while (i < n) {
            String symbol = "";
            SymbolType type;
            String value="";
            System.out.print(i + "#");
            //System.out.println(code.charAt(i));
            while ( i<n && (code.charAt(i) == '\0' || code.charAt(i) == '\n' || code.charAt(i) == '\r' || code.charAt(i) == '\t' || code.charAt(i)==' ')) {
                //过滤空白符，获取当前行号
                if (code.charAt(i) == '\n') {
                    line++;
                }
                i++;
            }
            if(i>=n){
                break;
            }
            if (Character.isAlphabetic(code.charAt(i)) || code.charAt(i) == '_') {
                System.out.println("char");
                //如果是字符，就判断是不是关键字和标识符，先判断关键字
                while (i<n && (Character.isAlphabetic(code.charAt(i)) || code.charAt(i) == '_' || Character.isDigit(code.charAt(i)))) {
                    System.out.println(code.charAt(i));
                    symbol = symbol + code.charAt(i);
                    i++;
                }
                //boolean iskey=false;
                type = SymbolType.IDENTIFIER;
                for (int j = 0; j < keywords.length; j++) {
                    if (symbol.equals(keywords[j])) {
                        type = SymbolType.values()[j];
                    }
                }
                tokenList.add(new Token(symbol, type, symbol, line));
            } else if (Character.isDigit(code.charAt(i))) {
                //i=numberAnalyse(i, n, line);
                symbol="";
                while(i<n && Character.isDigit(code.charAt(i))){
                    symbol=symbol+code.charAt(i);
                    i++;
                }
                if(Character.isAlphabetic(code.charAt(i)) || code.charAt(i) == '_'){
                    //判断标识符不能是数字开头
                    while(Character.isAlphabetic(code.charAt(i)) || code.charAt(i) == '_' || Character.isDigit(code.charAt(i))){
                        i++;
                    }
                    error.addErrorMessage(line, 0, ' ');
                } else if(symbol.charAt(0) == '0' && symbol.length()>1) {
                    //判断数字不能前导0
                    error.addErrorMessage(line, 1, ' ');
                } else {
                    //正确
                    type = SymbolType.INTEGER;
                    tokenList.add(new Token(symbol, type, symbol, line));
                }
            } else if (code.charAt(i) == '+') {
                symbol = "+";
                type = SymbolType.PLUSSY;
                i++;
                SymbolType tmp=SymbolType.IDENTIFIER;//随便写一个，不然idea会报错
                boolean flag=false;
                if(tokenList.size()==0){
                    flag=true;
                } else {
                    tmp=tokenList.get(tokenList.size()-1).type;
                }
                //判断是否为带符号的整数
                if(Character.isDigit(code.charAt(i)) && (flag || tmp==SymbolType.PLUSSY || tmp==SymbolType.MINUSSY ||
                        tmp==SymbolType.MULTIPLYSY || tmp==SymbolType.DIVIDSY || tmp==SymbolType.LEFTSMALLBRACKET ||
                        tmp==SymbolType.ASSIGNSY || tmp==SymbolType.EQUALSY|| tmp==SymbolType.GREATERSY|| tmp==SymbolType.LESSSY||
                        tmp==SymbolType.GREATEROREQUALSY || tmp==SymbolType.LESSOREQUALSY|| tmp==SymbolType.COMMASY)){
                    //前一个是 + - * / ( = == < <= > >= ，且后一个是数字 说明是负号（关系符号、运算符、（、=、，）
                    while(i<n && Character.isDigit(code.charAt(i))){
                        symbol=symbol+code.charAt(i);
                        i++;
                    }
                    if(Character.isAlphabetic(code.charAt(i)) || code.charAt(i) == '_'){
                        //判断标识符不能是数字开头
                        while(Character.isAlphabetic(code.charAt(i)) || code.charAt(i) == '_' || Character.isDigit(code.charAt(i))){
                            i++;
                        }
                        error.addErrorMessage(line, 0, ' ');
                    } else if(symbol.charAt(0) == '0' && symbol.length()>1) {
                        //判断数字不能前导0
                        error.addErrorMessage(line, 1, ' ');
                    } else {
                        //正确
                        type = SymbolType.INTEGER;
                        tokenList.add(new Token(symbol, type, symbol, line));
                    }
                }else{
                    tokenList.add(new Token(symbol, type, symbol, line));
                }
            } else if (code.charAt(i) == '-') {
                symbol = "-";
                type = SymbolType.MINUSSY;
                i++;
                boolean flag=false;
                SymbolType tmp=SymbolType.IDENTIFIER;
                if(tokenList.size()==0){
                    flag=true;
                } else {
                    tmp=tokenList.get(tokenList.size()-1).type;
                }
                //判断是否为带符号的整数
                if(Character.isDigit(code.charAt(i)) && (flag || tmp==SymbolType.PLUSSY || tmp==SymbolType.MINUSSY || tmp==SymbolType.MULTIPLYSY || tmp==SymbolType.DIVIDSY
                        || tmp==SymbolType.LEFTSMALLBRACKET || tmp==SymbolType.ASSIGNSY || tmp==SymbolType.EQUALSY)){
                    //前一个是 + - * / ( = == 且后一个是数字 说明是负号
                    //前面啥都没有也认为是+
                    while(i<n && Character.isDigit(code.charAt(i))){
                        symbol=symbol+code.charAt(i);
                        i++;
                    }
                    if(Character.isAlphabetic(code.charAt(i)) || code.charAt(i) == '_'){
                        //判断标识符不能是数字开头
                        while(Character.isAlphabetic(code.charAt(i)) || code.charAt(i) == '_' || Character.isDigit(code.charAt(i))){
                            i++;
                        }
                        error.addErrorMessage(line, 0, ' ');
                    } else if(symbol.charAt(0) == '0' && symbol.length()>1) {
                        //判断数字不能前导0
                        error.addErrorMessage(line, 1, ' ');
                    } else {
                        //正确
                        type = SymbolType.INTEGER;
                        tokenList.add(new Token(symbol, type, symbol, line));
                    }
                }else{
                    tokenList.add(new Token(symbol, type, symbol, line));
                }
            } else if (code.charAt(i) == '*') {
                symbol = "*";
                type = SymbolType.MULTIPLYSY;
                i++;
                tokenList.add(new Token(symbol, type, symbol, line));
            } else if (code.charAt(i) == '/') {
                symbol = "/";
                type = SymbolType.DIVIDSY;
                i++;
                //跳过注释
                if(code.charAt(i)=='*'){
                    i++;
                    while(i<n-1 && (code.charAt(i)!='*' || code.charAt(i+1)!='/')) {
                        if(code.charAt(i)=='\n'){
                            line++;
                        }
                        i++;
                    }
                    if(i<n-1){
                        i+=2;
                    } else {
                        error.addErrorMessage(line, 2, ' ');
                    }
                } else {
                    tokenList.add(new Token(symbol, type, symbol, line));
                }
            } else if (code.charAt(i) == '(') {
                symbol = "(";
                type = SymbolType.LEFTSMALLBRACKET;
                i++;
                tokenList.add(new Token(symbol, type, symbol, line));
            } else if (code.charAt(i) == ')') {
                symbol = ")";
                type = SymbolType.RIGHTSMALLBRACKET;
                i++;
                tokenList.add(new Token(symbol, type, symbol, line));
            } else if (code.charAt(i) == '{') {
                symbol = "{";
                type = SymbolType.LEFTBIGBRACKET;
                i++;
                tokenList.add(new Token(symbol, type, symbol, line));
            } else if (code.charAt(i) == '}') {
                symbol = "}";
                type = SymbolType.RIGHTBIGBRACKET;
                i++;
                tokenList.add(new Token(symbol, type, symbol, line));
            } else if(code.charAt(i)==',') {
                symbol = ",";
                type = SymbolType.COMMASY;
                i++;
                tokenList.add(new Token(symbol, type, symbol, line));
            } else if(code.charAt(i)==';') {
                symbol = ";";
                type = SymbolType.SEMISY;
                i++;
                tokenList.add(new Token(symbol, type, symbol, line));
            } else if (code.charAt(i) == '=') {
                //判断 = 和 ==
                symbol = "=";
                type = SymbolType.ASSIGNSY;
                i++;
                if (code.charAt(i) == '=') {
                    symbol = "==";
                    type = SymbolType.EQUALSY;
                    i++;
                }
                tokenList.add(new Token(symbol, type, symbol, line));
            } else if (code.charAt(i) == '<') {
                i++;
                symbol = "<";
                type = SymbolType.LESSSY;
                if (code.charAt(i) == '=') {
                    symbol = "<=";
                    type = SymbolType.LESSOREQUALSY;
                    i++;
                }
                tokenList.add(new Token(symbol, type, symbol, line));
            } else if (code.charAt(i) == '>') {
                symbol = ">";
                type = SymbolType.GREATERSY;
                i++;
                if (code.charAt(i) == '=') {
                    symbol = ">=";
                    type = SymbolType.GREATEROREQUALSY;
                    i++;
                }
                tokenList.add(new Token(symbol, type, symbol, line));
            } else if(code.charAt(i)=='!') {
                i++;
                if(code.charAt(i)=='='){
                    symbol="!=";
                    type=SymbolType.NOTEQUALSY;
                    i++;
                    tokenList.add(new Token(symbol, type, symbol, line));
                } else {
                    error.addErrorMessage(line, 3, ' ');
                }
            } else if(code.charAt(i)=='"'){
                //引号不作为识别符号，只是为了区别string，不识别反而在语法分析的时候好做一点QAQ
                //tokens.add(new Token("\"", SymbolType.QUOTESY, "\"", line));
                i++;
                //char ch;
                while(i<n && code.charAt(i)!='"' && code.charAt(i)!='\n') {
                    System.out.println(symbol);
                    //ch=code.charAt(i);
                    if(code.charAt(i)=='\\'){
                        symbol=symbol+code.charAt(i);
                        System.out.println("oh no!");
                        i++;
                        if(i>=n){
                            error.addErrorMessage(line, 4, ' ');
                            break;
                        } else {
                            symbol=symbol+code.charAt(i);
                            i++;
                        }
                        //value=value+ch;
                    } else {
                        symbol=symbol+code.charAt(i);
                        i++;
                    }
                }
                if(i<n && code.charAt(i)=='\"'){
                    i++;
                    type=SymbolType.STRING;
                    tokenList.add(new Token(symbol, type, symbol, line));
                    //tokens.add(new Token("\"", SymbolType.QUOTESY, "\"", line));
                } else {
                    error.addErrorMessage(line, 5, ' ');
                }
            } else {
                error.addErrorMessage(line, 6, code.charAt(i));
                i++;
            }
        }
    }

    public ArrayList<String> getErrorMessage() {
        return error.getErrorMessage();
    }

    public boolean success() {
        return error.getErrorMessage().size()==0;
    }
}
/*
* 标识符
* 整数（常量）
* 分解符
* 关键字
* */