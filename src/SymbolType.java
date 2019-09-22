public enum SymbolType {
    //"const", "int", "void", "if", "else", "while", "main", "return", "printf", "scanf"
    CONSTSY,            //const
    INTSY,              //int
    VOIDSY,             //void
    IFSY,               //if
    ELSESY,             //else
    WHILESY,            //while
    MAINSY,             //main
    RETURNSY,           //return
    PRINTFSY,           //scanf
    SCANFSY,            //printf
    IDENTIFIER,         //标识符
    INTEGER,            //整数
    STRING,             //字符串
    PLUSSY,             //+
    MINUSSY,            //-
    MULTIPLYSY,         //*
    DIVIDSY,            // /
    LEFTSMALLBRACKET,   //(
    RIGHTSMALLBRACKET,  //)
    LEFTBIGBRACKET,     //{
    RIGHTBIGBRACKET,    //}
    LESSSY,             //<
    LESSOREQUALSY,      //<=
    GREATERSY,          //>
    GREATEROREQUALSY,   //>=
    EQUALSY,            //==
    ASSIGNSY,           //=
    NOTEQUALSY,         //!=
    COMMASY,            //,
    SEMISY,             //;
    END,                //表示程序结束，用在语法分析上面
}
