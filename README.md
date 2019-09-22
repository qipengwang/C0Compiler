# C0Compiler
- BUAA compile project. 
  A C0Compiler implemented in Java
- doc文件夹下是详细设计文档，src里面是代码
- 最后实验成绩没拿满
  - 扣分项：词法分析失败之后不会进入语法分析部分
  - 改正方法：
    - 词法分析里面其他的token用`OTHER`表示
    - 语法分析里面遇见了OTHER就正常处理就行**不用变**
    - main文件里面有个编译按钮的逻辑（`compileItem.addActionListener`）：在`if(lexer.success())` 里面有个 `if(parser.success())`，将语法分析的parser放在外面就行了，就是从 `//语法分析` 这个注释开始的部分放在 `if(lexer.success())` 的外面
- 本人蒟蒻无比，如有问题欢迎联系
  - QQ：861026685
  - E-mail：861026685@qq.com, wangqipeng@buaa.edu.cn
- 万水千山总是情，给个star行不行 :smile: 