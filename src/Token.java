public class Token {
    public String symbol;//符号
    public SymbolType type;//类型
    public String value;//值：如果是字符串就把它改成过滤了转义符之后的串（其实基本上没啥用，但是懒得改了）
    public int line;//所在行

    public Token(String symbol, SymbolType type, String value, int line) {
        this.symbol = symbol;
        this.type = type;
        this.value = value;
        this.line = line;
        StringBuilder stringBuilder=new StringBuilder();
        for (int j = 0; j < symbol.length(); j++) {
            if(symbol.charAt(j)=='\\'){
                j++;
                if(symbol.charAt(j)=='\\'){
                    stringBuilder.append("\\");
                } else if(symbol.charAt(j)=='\''){
                    stringBuilder.append("\'");
                } else if(symbol.charAt(j)=='\"'){
                    stringBuilder.append("\"");
                } else if(symbol.charAt(j)=='a' || symbol.charAt(j)=='b' || symbol.charAt(j)=='f' || symbol.charAt(j)=='v' || symbol.charAt(j)=='e'
                        || symbol.charAt(j)=='n' || symbol.charAt(j)=='r' || symbol.charAt(j)=='t'){
                    // \a \b \f \n \r \t \v 忽略掉
                    stringBuilder.append("");
                } else {
                    stringBuilder.append(symbol.charAt(j));
                }
            } else {
                stringBuilder.append(symbol.charAt(j));
            }
        }
        this.value = stringBuilder.toString();
    }
}
