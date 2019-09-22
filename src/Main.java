import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;

public class Main extends JFrame {
    //UI元素对象
    private JFrame jFrame;//最大的窗体
    private JPanel textPanel;//文字面板，包括代码文本框和输入文本域
    private JPanel tablePanel;//表格面板，包含三个表格
    private JPanel consolePanel;//控制台面板，包括控制台输出文本域
    private JMenuBar menuBar;//菜单栏
    private JMenu fileMenu;//file菜单，后两个同理
    private JMenu projectMenu;
    private JMenu aboutMenu;
    private JMenuItem openItem;//file菜单里面的open选项，后面的同理
    private JMenuItem saveItem;
    private JMenuItem cleanItem;
    private JMenuItem exitItem;
    private JMenuItem compileItem;
    private JMenuItem runItem;
    private JMenuItem aboutItem;
    private JTextArea codeTextArea;//PL0代码显示区域
    private JTextArea consoleTextArea;//控制台文本显示区域
    private JTextField inputTextField;//输入文本域
    private JScrollPane codeScrollPane;//带有滑动条的窗口，包含着各个table或者textArea，后面的同理，每个textarea和table是分开的，之后加入到各自的Panel中
    private JScrollPane consoleScrollPane;
    private JScrollPane tokenScrollPane;
    private JScrollPane symbolScrollPane;
    private JScrollPane pcodeScrollPane;
    private JLabel C0, Input, Console;
    private Font font;//显示字体
    private JTable tokenTable;//token表格
    private String[] tokenColumnNames;//表格里面的首行
    private DefaultTableModel tokenTableModel;
    private JTable symbolTable;//symbol表格
    private String[] symbolColumnNames;
    private DefaultTableModel symbolTableModel;
    private JTable pcodeTable;//pcode表格
    private String[] pcodeColumnNames;
    private DefaultTableModel pcodeTableModel;
    private File file;
    private FileDialog openDialog;//打开文件的窗口
    private FileDialog saveDialog;

    //编译器需要的对象
    private String originCode="";//原始代码：
    private String compileCode="";//编译后的代码
    private Lexer lexer=null;
    private ArrayList<Token> tokenList=null;
    private ArrayList<Pcode> pcodeList = null;
    private ArrayList<String> compileErrorList=null;
    private ArrayList<String> runErrorList=null;
    private ArrayList<String> outputList=null;



    public Main(){
        jFrame = new JFrame("C0compiler");
        jFrame.setSize(1150, 800);//935
        //jFrame.getGraphicsConfiguration().getDevice().setFullScreenWindow(jFrame);
        jFrame.setResizable(false);//是否可以改变大小
        jFrame.setDefaultCloseOperation(EXIT_ON_CLOSE);

        font=new Font("宋体", Font.BOLD,20);
        C0=new JLabel("          C0          ");//空格为样式需要
        C0.setFont(font);
        Input=new JLabel("Input");
        Input.setFont(font);
        Console=new JLabel("Console");
        Console.setFont(font);

        menuBar = new JMenuBar();
        menuBar.setFont(font);
        fileMenu =new JMenu("file");
        fileMenu.setFont(font);
        projectMenu =new JMenu("project");
        projectMenu.setFont(font);
        aboutMenu=new JMenu("about");
        aboutMenu.setFont(font);
        openItem=new JMenuItem("open");
        openItem.setFont(font);
        saveItem=new JMenuItem("save");
        saveItem.setFont(font);
        cleanItem=new JMenuItem("clean");
        cleanItem.setFont(font);
        exitItem=new JMenuItem("exit");
        exitItem.setFont(font);
        compileItem=new JMenuItem("compile");
        compileItem.setFont(font);
        runItem=new JMenuItem("run");
        runItem.setFont(font);
        aboutItem=new JMenuItem("about");
        aboutItem.setFont(font);

        menuBar.add(fileMenu);
        menuBar.add(projectMenu);
        menuBar.add(aboutMenu);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(cleanItem);
        fileMenu.add(exitItem);
        projectMenu.add(compileItem);
        projectMenu.add(runItem);
        aboutMenu.add(aboutItem);

        codeTextArea=new JTextArea(25, 53);//有文本框设置为25
        codeTextArea.setLineWrap(false);//到达指定宽度之后不换行
        codeTextArea.setFont(new Font("宋体", Font.BOLD, 15));
        inputTextField=new JTextField(58);
        inputTextField.setPreferredSize(new Dimension(0, 30));
        inputTextField.addFocusListener(new JTextFieldHintListener(inputTextField, "Input:"));
        inputTextField.setFont(new Font("宋体", Font.BOLD, 15));
        consoleTextArea=new JTextArea(10, 122);//161
        consoleTextArea.setFont(new Font("宋体", Font.BOLD, 15));
        consoleTextArea.setLineWrap(false);//到达指定宽度之后不换行
        codeScrollPane=new JScrollPane(codeTextArea);
        codeScrollPane.setRowHeaderView(new LineNumberHeaderView());
        consoleScrollPane=new JScrollPane(consoleTextArea);
        codeScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        codeScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        consoleScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        consoleScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        tokenColumnNames = new String[]{"symbol", "type", "value", "line"};
        tokenTableModel = new DefaultTableModel(tokenColumnNames, 0);
        tokenTable = new JTable(tokenTableModel);
        tokenTable.setPreferredScrollableViewportSize(new Dimension(275, 500));
        tokenTable.setFont(new Font("宋体", Font.BOLD, 15));
        tokenTable.getTableHeader().setFont(new Font("宋体", Font.BOLD, 15));
        tokenTable.setRowHeight(20);
        tokenScrollPane = new JScrollPane(tokenTable);
        tokenScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tokenScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tokenScrollPane.setFont(new Font("宋体", Font.BOLD, 10));

        symbolColumnNames = new String[]{"变量名", "变量类型", "变量值", "变量层次", "变量地址"};
        symbolTableModel = new DefaultTableModel(symbolColumnNames, 0);
        symbolTable = new JTable(symbolTableModel);
        symbolTable.setPreferredScrollableViewportSize(new Dimension(275, 500));
        symbolTable.setFont(new Font("宋体", Font.BOLD, 15));
        symbolTable.getTableHeader().setFont(new Font("宋体", Font.BOLD, 15));
        symbolTable.setRowHeight(20);
        symbolScrollPane = new JScrollPane(symbolTable);
        symbolScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        symbolScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        pcodeColumnNames = new String[]{"序号", "F", "L", "A"};
        pcodeTableModel = new DefaultTableModel(pcodeColumnNames, 0);
        pcodeTable=new JTable(pcodeTableModel);
        pcodeTable.setPreferredScrollableViewportSize(new Dimension(275, 500));
        pcodeTable.setFont(new Font("宋体", Font.BOLD, 15));
        pcodeTable.getTableHeader().setFont(new Font("宋体", Font.BOLD, 15));
        pcodeTable.setRowHeight(20);
        pcodeScrollPane=new JScrollPane(pcodeTable);
        pcodeScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pcodeScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        tablePanel = new JPanel(new GridLayout(1, 3, 0,0));
        tablePanel=new JPanel(new FlowLayout());
//        tablePanel.add(new JLabel("                  "));
        tablePanel.add(tokenScrollPane);
//        tablePanel.add(symbolScrollPane);
        tablePanel.add(pcodeScrollPane);
        tablePanel.add(new JLabel("  "));
        tablePanel.setFont(new Font("宋体", Font.BOLD, 10));
//        textPanel=new JPanel(new GridLayout(2, 1));
//        textPanel.add(codeScrollPane);
//        textPanel.add(inputTextField);
        textPanel=new JPanel(new FlowLayout());
        textPanel.add(C0);
        textPanel.add(codeScrollPane);
        JLabel tmp=new JLabel(" \r\n ");
        tmp.setFont(new Font("宋体", Font.BOLD,6 ));
        //textPanel.add(tmp);
        //textPanel.add(Input);
        textPanel.add(inputTextField);
        //textPanel.add(Console);
        //consolePanel=new JPanel(new GridLayout(1,1));
        consolePanel=new JPanel();
        consolePanel.add(consoleScrollPane, new FlowLayout());
        consolePanel.setPreferredSize(new Dimension(1300, 200));

        jFrame.add(menuBar, BorderLayout.NORTH);
        jFrame.add(textPanel, BorderLayout.CENTER);
        jFrame.add(tablePanel, BorderLayout.EAST);
        jFrame.add(consolePanel, BorderLayout.SOUTH);

        jFrame.setVisible(true);
        openDialog = new FileDialog(jFrame,"打开",FileDialog.LOAD);
        saveDialog = new FileDialog(jFrame,"保存",FileDialog.SAVE);

        file=null;
        originCode="";
        Event();
    }

    public void Event(){
        openItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //JOptionPane.showMessageDialog(null, "failed to read the file", "please try again", JOptionPane.ERROR_MESSAGE);
                if(file!=null && originCode.equals(codeTextArea.getText())){
                    //打开文件 && 没有修改代码
                    //什么都不做，直接打开，代码在后面
                    System.out.println("打开文件 && 没有修改代码");
                } else if(file==null) {
                    //文件时空的表示用户输入（有输入的前提下即下面这个if），提示保存
                    if(!originCode.equals(codeTextArea.getText())) {
                        //如果不是打开的文件而是直接输入 就提示用户
                        int option=JOptionPane.showConfirmDialog(jFrame, "是否保存文件？","提示",JOptionPane.YES_NO_OPTION);
                        if(option == 0){//是 保存
                            saveDialog.setVisible(true);
                            String filePath=saveDialog.getDirectory();
                            String fileName=saveDialog.getFile();
                            if(filePath!=null && fileName!=null){
                                file=new File(filePath, fileName);
                            }
                            try {
                                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
                                String text = codeTextArea.getText();
                                bufferedWriter.write(text);
                                bufferedWriter.close();
                                //成功保存之后更新originCode的内容
                                originCode=text;
                                JOptionPane.showMessageDialog(jFrame, "保存成功", "提示", JOptionPane.PLAIN_MESSAGE);
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(jFrame, "保存失败，请重试", "失败", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                } else {
                    //打开的文件，但是修改了代码，提示
                    int option=JOptionPane.showConfirmDialog(jFrame, "是否保存文件？","提示",JOptionPane.YES_NO_OPTION);
                    if(option == 0) {
                        //直接保存
                        try {
                            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
                            String text = codeTextArea.getText();
                            bufferedWriter.write(text);
                            bufferedWriter.close();
                            //成功保存之后更新originCode的内容
                            originCode=text;
                            JOptionPane.showMessageDialog(jFrame, "保存成功", "提示", JOptionPane.PLAIN_MESSAGE);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(jFrame, "保存失败，请重试", "失败", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
                //前期处理完成之后打开文件
                openDialog.setVisible(true);
                String filePath = openDialog.getDirectory();
                String fileName = openDialog.getFile();
                if (filePath == null || fileName == null) {
                    return;
                }
                file = new File(filePath, fileName);
                codeTextArea.setText("");//打开文件之前清空文本区域
                clean();
                inputTextField.setText("");
                try {
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                    String line = null;
                    while ((line = bufferedReader.readLine()) != null) {
                        //整行读入到空为止
                        codeTextArea.append(line + "\n");
                    }
                    originCode=codeTextArea.getText();
                    compileCode="";
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(jFrame, "无法读取文件，请重试", "失败", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        saveItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(file == null){
                    saveDialog.setVisible(true);
                    String filePath=saveDialog.getDirectory();
                    String fileName=saveDialog.getFile();
                    if(filePath!=null && fileName!=null){
                        file=new File(filePath, fileName);
                    }
                }
                try {
                    BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
                    String text = codeTextArea.getText();
                    bufferedWriter.write(text);
                    bufferedWriter.close();
                    //成功保存之后更新originCode的内容
                    originCode=text;
                    JOptionPane.showMessageDialog(jFrame, "保存成功", "提示", JOptionPane.PLAIN_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(jFrame, "保存失败，请重试", "失败", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        cleanItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(file!=null && codeTextArea.getText().equals(originCode)) {
                    //文件打开并且没有修改，清空历史代码并退出
                    originCode = "";
                    compileCode="";
                    file = null;
                    clean();
                    consoleTextArea.setText("");
                    codeTextArea.setText("");
                    inputTextField.setText("");
                } else if(file==null) {
                    //如果是直接输入的，有修改
                    if (!codeTextArea.getText().equals(originCode)) {
                        int option = JOptionPane.showConfirmDialog(jFrame, "是否保存为文件？","提示",JOptionPane.YES_NO_OPTION);
                        //System.out.println(option);
                        if (option == 0) {
                            //选择 是 就创建文件并保存，之后还需要清空文件并退出
                            try {
                                saveDialog.setVisible(true);
                                String filePath = saveDialog.getDirectory();
                                String fileName = saveDialog.getFile();
                                if (filePath != null && fileName != null) {
                                    file = new File(filePath, fileName);
                                }
                                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
                                String text = codeTextArea.getText();
                                bufferedWriter.write(text);
                                bufferedWriter.close();
                                //只有保存成功才能退出,清空上一次保存的变量
                                originCode = "";
                                compileCode="";
                                file = null;
                                clean();
                                consoleTextArea.setText("");
                                codeTextArea.setText("");
                                inputTextField.setText("");
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(jFrame, "无法保存文件，请重试", "失败", JOptionPane.ERROR_MESSAGE);
                            }
                        } else {
                            //选择 否 就直接清空数据
                            originCode = "";
                            compileCode="";
                            file = null;
                            clean();
                            consoleTextArea.setText("");
                            codeTextArea.setText("");
                            inputTextField.setText("");
                        }
                    }
                } else {
                    //如果做了修改却没有保存，提示
                    int option = JOptionPane.showConfirmDialog(jFrame, "是否需要保存？", "提示", JOptionPane.YES_NO_OPTION);
                    if (option == 0) {
                        //选择 是 不用创建文件，直接保存后清空记录并清空
                        try {
                            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
                            String text = codeTextArea.getText();
                            bufferedWriter.write(text);
                            bufferedWriter.close();
                            //只有保存成功才能退出
                            originCode = "";
                            compileCode="";
                            file = null;
                            clean();
                            consoleTextArea.setText("");
                            codeTextArea.setText("");
                            inputTextField.setText("");
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(jFrame, "无法保存，请重试", "失败", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        //否则直接退出
                        originCode = "";
                        compileCode="";
                        file = null;
                        clean();
                        consoleTextArea.setText("");
                        codeTextArea.setText("");
                        inputTextField.setText("");
                    }
                }
            }
        });

        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(file!=null && codeTextArea.getText().equals(originCode)) {
                    //文件打开并且没有修改，清空历史代码并退出
                    originCode = "";
                    compileCode="";
                    file = null;
                    clean();
                    consoleTextArea.setText("");
                    codeTextArea.setText("");
                    inputTextField.setText("");
                    System.exit(0);
                } else if(file==null) {
                    //如果是直接输入的，提示是否保存
                    if (!codeTextArea.getText().equals(originCode)) {
                        int option = JOptionPane.showConfirmDialog(jFrame, "是否需要保存？", "提示", JOptionPane.YES_NO_OPTION);
                        //System.out.println(option);
                        if (option == 0) {
                            //选择 是 就创建文件并保存，之后还需要清空文件并退出
                            try {
                                saveDialog.setVisible(true);
                                String filePath = saveDialog.getDirectory();
                                String fileName = saveDialog.getFile();
                                if (filePath != null && fileName != null) {
                                    file = new File(filePath, fileName);
                                }
                                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
                                String text = codeTextArea.getText();
                                bufferedWriter.write(text);
                                bufferedWriter.close();
                                //只有保存成功才能退出,清空上一次保存的变量
                                originCode = "";
                                compileCode="";
                                file = null;
                                clean();
                                consoleTextArea.setText("");
                                codeTextArea.setText("");
                                inputTextField.setText("");
                                System.exit(0);
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(jFrame, "无法保存文件，请重试", "失败", JOptionPane.ERROR_MESSAGE);
                            }
                        } else {
                            //选择 否 就直接清空数据并退出
                            originCode = "";
                            compileCode="";
                            file = null;
                            clean();
                            consoleTextArea.setText("");
                            codeTextArea.setText("");
                            inputTextField.setText("");
                            System.exit(0);
                        }
                    }
                } else {
                    //如果做了修改却没有保存，提示
                    int option = JOptionPane.showConfirmDialog(jFrame, "是否需要保存？", "提示", JOptionPane.YES_NO_OPTION);
                    if (option == 0) {
                        //选择 是 不用创建文件，直接保存后清空记录并退出
                        try {
                            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
                            String text = codeTextArea.getText();
                            bufferedWriter.write(text);
                            bufferedWriter.close();
                            //只有保存成功才能退出
                            originCode = "";
                            compileCode="";
                            file = null;
                            clean();
                            consoleTextArea.setText("");
                            codeTextArea.setText("");
                            inputTextField.setText("");
                            System.exit(0);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(jFrame, "无法保存，请重试", "失败", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        //否则直接退出
                        originCode = "";
                        compileCode="";
                        file = null;
                        clean();
                        consoleTextArea.setText("");
                        codeTextArea.setText("");
                        inputTextField.setText("");
                        System.exit(0);
                    }
                }
            }
        });

        compileItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //刷新数据
                clean();
                compileErrorList=null;
                compileCode = codeTextArea.getText();
                //编译、显示
                lexer=new Lexer(codeTextArea.getText());
                if(lexer.success()) {
                    tokenList=new ArrayList<Token>(lexer.getTokenList());//生成自己的token，方便在语法分析的时候传递
                    consoleTextArea.append("词法分析成功\n");
                    //显示token
                    for (int i = 0; i < tokenList.size(); i++) {
                        Token t = tokenList.get(i);
                        if(t.type==SymbolType.INTEGER){
                            //整数转二进制
                            System.out.println("fuck: "+t.value);
                            System.out.println(new BigInteger(t.value).toString(2)+" fuck");
                            tokenTableModel.addRow(new Object[]{t.symbol, t.type, new BigInteger(t.value).toString(2)+"(2)", t.line});
                        } else {
                            //String的value是无视了所有转义符的
                            tokenTableModel.addRow(new Object[]{t.symbol, t.type, t.value, t.line});
                        }
                    }
                    //写入文件
                    File tokensFile=new File("Token.txt");
                    if(tokensFile.exists()){
                        tokensFile.delete();
                    }
                    try{
                        tokensFile.createNewFile();
                        BufferedWriter bufferedWriter=new BufferedWriter(new FileWriter(tokensFile));
                        for (int i = 0; i < tokenList.size(); i++) {
                            String s;
                            Token t=tokenList.get(i);
                            if(t.type==SymbolType.INTEGER){
                                s="("+t.symbol+", INTEGER, "+new BigInteger(t.value).toString(2)+"(2))";
                            } else if(t.type==SymbolType.STRING){
                                s="("+t.symbol+", STRING, "+t.value+")";
                            } else {
                                s="("+t.symbol+", "+t.type.name()+")";
                            }
                            bufferedWriter.write(s+"\r\n");
                        }
                        bufferedWriter.close();
                        consoleTextArea.append("已将Token写入文件Token.txt\n");
                    } catch (Exception ex){
                        System.out.println("无法将Token写入文件Token.txt\n");
                    }
                    //语法分析
                    if(tokenList.size()==0){
                        return;
                    }
                    Parser parser=new Parser(tokenList);
                    if(parser.success()){
                        consoleTextArea.append("语法分析成功\n");
                        pcodeList = new ArrayList<Pcode>(parser.getPcodeList());
                        for(int i=0; i<pcodeList.size(); i++){
                            Pcode pcode=pcodeList.get(i);
                            pcodeTableModel.addRow(new Object[]{i, pcode.F, pcode.L, pcode.A});
                        }
                        //写入文件
                        File pcodesFile=new File("Pcode.txt");
                        if(tokensFile.exists()){
                            tokensFile.delete();
                        }
                        try{
                            tokensFile.createNewFile();
                            BufferedWriter bufferedWriter=new BufferedWriter(new FileWriter(pcodesFile));
                            for (Pcode pcode: pcodeList) {
                                bufferedWriter.write(pcode.F+"\t"+pcode.L+"\t"+pcode.A +"\r\n");
                            }
                            bufferedWriter.close();
                            consoleTextArea.append("已将Pcode写入文件Pcode.txt\n");
                        } catch (Exception ex){
                            JOptionPane.showMessageDialog(jFrame, "无法将错误写入Error.txt中", "失败", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        //语法分析失败了
                        consoleTextArea.append("语法分析失败\n");
                        compileErrorList=parser.getErrorMessage();
                        for (String err : compileErrorList) {
                            consoleTextArea.append(err + "\n");
                        }
                        try{
                            File parserErrorFile=new File("Error.txt");
                            if(parserErrorFile.exists()){
                                parserErrorFile.delete();
                            }
                            parserErrorFile.createNewFile();
                            BufferedWriter bufferedWriter=new BufferedWriter(new FileWriter(parserErrorFile));
                            for (String err : compileErrorList) {
                                bufferedWriter.write(err + "\r\n");
                            }
                            bufferedWriter.close();
                            consoleTextArea.append("已将错误写入Error.txt中\n");
                        } catch(Exception ex){
                            JOptionPane.showMessageDialog(jFrame, "无法将错误写入Error.txt中", "失败", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } else {
                    //词法分析失败了
                    System.out.println("fuck");
                    consoleTextArea.append("词法分析失败\n");
                    compileErrorList=lexer.getErrorMessage();
                    for (String err : compileErrorList) {
                        consoleTextArea.append(err + "\n");
                    }
                    try{
                        File lexerErrorFile=new File("Error.txt");
                        if(lexerErrorFile.exists()){
                            lexerErrorFile.delete();
                        }
                        lexerErrorFile.createNewFile();
                        BufferedWriter bufferedWriter=new BufferedWriter(new FileWriter(lexerErrorFile));
                        for (String err : compileErrorList) {
                            bufferedWriter.write(err + "\r\n");
                        }
                        bufferedWriter.close();
                        consoleTextArea.append("已将错误写入Error.txt中\n");
                    } catch(Exception ex){
                        JOptionPane.showMessageDialog(jFrame, "无法将错误写入Error.txt中", "失败", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        runItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("run");
                if(!compileCode.equals(codeTextArea.getText())){
                    //上次编译的代码和这次运行的代码不一样
                    JOptionPane.showMessageDialog(jFrame, "请先编译代码", "失败", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if(compileErrorList==null){
                    String console=consoleTextArea.getText();
                    runErrorList=null;
                    consoleTextArea.setText("");
                    consoleTextArea.append("词法分析成功\n");
                    if(console.contains("已将Token写入文件Token.txt")) {
                        consoleTextArea.append("已将Token写入文件Token.txt\n");
                    }
                    consoleTextArea.append("语法分析成功\n");
                    if(console.contains("已将Pcode写入文件Pcode.txt")) {
                        consoleTextArea.append("已将Pcode写入文件Pcode.txt\n");
                    }
                    System.out.println(inputTextField.getText()+"-"+inputTextField.getText().replaceAll("input:", ""));
                    Interpreter interpreter=new Interpreter(pcodeList, inputTextField.getText().replaceAll("input:", ""));
                    if(interpreter.success()) {
                        outputList=interpreter.getOutput();
                        for(String s: outputList){
                            consoleTextArea.append(s);
                        }
                    } else {
                        consoleTextArea.append("运行失败\n");
                        runErrorList=interpreter.getErrorMessage();
                        for(String err: runErrorList){
                            consoleTextArea.append(err+"\n");
                        }
                        try{
                            File interpreterErrorFile=new File("Error.txt");
                            if(interpreterErrorFile.exists()){
                                interpreterErrorFile.delete();
                            }
                            interpreterErrorFile.createNewFile();
                            BufferedWriter bufferedWriter=new BufferedWriter(new FileWriter(interpreterErrorFile));
                            for (String err : runErrorList) {
                                bufferedWriter.write(err + "\r\n");
                            }
                            bufferedWriter.close();
                            consoleTextArea.append("已将错误写入Error.txt中\n");
                        } catch(Exception ex){
                            JOptionPane.showMessageDialog(jFrame, "无法将错误写入Error.txt中", "失败", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(jFrame, "代码错误，请重试", "失败", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        aboutItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               JOptionPane.showMessageDialog(jFrame, "C0Compiler\n16211077\n王启鹏");
            }
        });
    }

    public void clean(){
        tokenTableModel.getDataVector().clear();
        tokenTableModel.fireTableDataChanged();
        tokenTable.validate();
        tokenTable.updateUI();
        symbolTableModel.getDataVector().clear();
        symbolTableModel.fireTableDataChanged();
        symbolTable.validate();
        symbolTable.updateUI();
        pcodeTableModel.getDataVector().clear();
        pcodeTableModel.fireTableDataChanged();
        pcodeTable.validate();
        pcodeTable.updateUI();
        consoleTextArea.setText("");
    }


    public static void main(String[] args) {
        new Main();
    }
}
