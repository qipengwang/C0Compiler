import java.lang.*;
import java.util.ArrayList;

public class Interpreter {
    private ArrayList<Pcode> pcodeList;
    private ArrayList<String> stringList;
    private int[] stack;
    private int t, b, p, i;//栈顶指针，当前栈基指针，pcode指针（PC），scanf输出指针
    private String[] input;
    private Error error;
    private ArrayList<String> output;

    public Interpreter(ArrayList<Pcode> pcodes, String string) {
        this.pcodeList = pcodes;
        this.stack = new int[1000000];
        this.t = this.b = this.p = this.i = 0;
        input = string.strip().replaceAll("\\s+", " ").split(" ");
        System.out.println("interpreter:input=");
        for (String str : input) {
            System.out.println(str);
        }
        error = new Error();
        output = new ArrayList<String>();
        try {
            run();
        } catch (Exception e) {
            error.addErrorMessage(p - 1, 36);
        }
    }

    public boolean success() {
        return error.getErrorMessage().size() == 0;
    }

    public ArrayList<String> getOutput() {
        return output;
    }

    public ArrayList<String> getErrorMessage() {
        return error.getErrorMessage();
    }

    private void run() {
        Pcode pcode = pcodeList.get(p++);
        while (p != 0) {
            System.out.println(pcode.F + " " + pcode.L + " " + pcode.A + " " + p);
            if (pcode.F.equals(Pcode.LIT)) {
                stack[t++] = pcode.A;
            } else if (pcode.F.equals(Pcode.OPR)) {
                if (pcode.A == 0) {
                    stack[b + 2] = stack[t - 1];
                    t = b;
                    b = stack[t];
                    p = stack[t + 1];
                    if (pcode.L == 1) {
                        stack[t] = stack[t + 2];
                        t++;
                    }
                } else if (pcode.A == 1) {
                    stack[t] = -stack[t];
                } else if (pcode.A == 2) {
                    t--;
                    stack[t - 1] += stack[t];
                } else if (pcode.A == 3) {
                    t--;
                    stack[t - 1] -= stack[t];
                } else if (pcode.A == 4) {
                    t--;
                    stack[t - 1] *= stack[t];
                } else if (pcode.A == 5) {
                    t--;
                    try {
                        stack[t - 1] /= stack[t];
                    } catch (Exception e) {
                        error.addErrorMessage(p - 1, 35);
                        return;
                    }
                } else if (pcode.A == 7) {
                    t--;
                    stack[t - 1] = stack[t - 1] == stack[t] ? 1 : 0;
                } else if (pcode.A == 8) {
                    t--;
                    stack[t - 1] = stack[t - 1] != stack[t] ? 1 : 0;
                } else if (pcode.A == 9) {
                    t--;
                    stack[t - 1] = stack[t - 1] < stack[t] ? 1 : 0;
                } else if (pcode.A == 10) {
                    t--;
                    stack[t - 1] = stack[t - 1] >= stack[t] ? 1 : 0;
                } else if (pcode.A == 11) {
                    t--;
                    stack[t - 1] = stack[t - 1] > stack[t] ? 1 : 0;
                } else if (pcode.A == 12) {
                    t--;
                    stack[t - 1] = stack[t - 1] <= stack[t] ? 1 : 0;
                } else {
                    System.out.println("error");
                }
            } else if (pcode.F.equals(Pcode.LOD)) {
                if (pcode.L == 2) {
                    stack[t++] = stack[stack[b] + pcode.A];
                } else {
                    stack[t++] = stack[b * pcode.L + pcode.A];
                }
            } else if (pcode.F.equals(Pcode.STO)) {
                stack[b * pcode.L + pcode.A] = stack[--t];
            } else if (pcode.F.equals(Pcode.CALA)) {
                //开辟新栈帧但是PC不变，PC在将参数压栈之后变
                stack[t] = b;
                b = t;
                t += pcode.A;
            } else if (pcode.F.equals(Pcode.CALB)) {
                stack[b + 1] = p;
                p = pcode.A;
            } else if (pcode.F.equals(Pcode.INT)) {
                t += pcode.A;
            } else if (pcode.F.equals(Pcode.JMP)) {
                p = pcode.A;
            } else if (pcode.F.equals(Pcode.JPC)) {
                if (stack[--t] == 0) {
                    p = pcode.A;
                }
            } else if (pcode.F.equals(Pcode.RED)) {
                System.out.println("RED: " + i);
                if (i < input.length) {
                    try {
                        stack[b * pcode.L + pcode.A] = Integer.parseInt(input[i++]);
                    } catch (Exception e) {
                        System.out.println("数字不合法");
                        error.addErrorMessage(p - 1, 34);
                        return;
                    }
                } else {
                    System.out.println("输入不够");
                    error.addErrorMessage(p - 1, 34);
                    return;
                }
            } else if (pcode.F.equals(Pcode.WRTI)) {
                output.add(stack[t - 1] + "\n");
            } else if (pcode.F.equals(Pcode.WRTS)) {
                output.add(escape(pcode.S) + "\n");
            }
            if (p == 0) {
                //可以改成do-while，就不用这一步判断了
                return;
            }
            System.out.print(b + "-" + t + ":");
            for (int x = b; x < t; x++) {
                System.out.print(stack[x] + " ");
            }
            System.out.println();
            pcode = pcodeList.get(p++);
        }
    }

    private String escape(String s) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int j = 0; j < s.length(); j++) {
            if (s.charAt(j) == '\\') {
                j++;
                if (s.charAt(j) == '\\') {
                    stringBuilder.append("\\");
                } else if (s.charAt(j) == '\'') {
                    stringBuilder.append("\'");
                } else if (s.charAt(j) == '\"') {
                    stringBuilder.append("\"");
                } else if (s.charAt(j) == 'a' || s.charAt(j) == 'b' || s.charAt(j) == 'f' || s.charAt(j) == 'v' || s.charAt(j) == 'e') {
                    // \a \b \f \v \e忽略掉
                    stringBuilder.append("");
                } else if (s.charAt(j) == 'n') {
                    stringBuilder.append('\n');
                } else if (s.charAt(j) == 't') {
                    stringBuilder.append('\t');
                } else if (s.charAt(j) == 'r') {
                    stringBuilder.append('\r');
                } else {
                    // \p 这种直接忽略转义了,就认为是p（codeblocks里面是这样的）
                    stringBuilder.append(s.charAt(j));
                }
            } else {
                stringBuilder.append(s.charAt(j));
            }
        }
        return stringBuilder.toString();
    }
}
