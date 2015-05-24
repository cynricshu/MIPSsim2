package data;

/**
 * User: Cynric
 * Date: 15/5/24
 * Time: 10:08
 */
public class Instruction {
    private int[] args;
    private String content;
    private String name;
    private String printValue;

    public Instruction(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public String getPrintValue() {
        return printValue;
    }

    public void setPrintValue(String printValue) {
        this.printValue = printValue;
    }

    public int[] getArgs() {
        return args;
    }

    public void setArgs(int[] args) {
        this.args = args;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
