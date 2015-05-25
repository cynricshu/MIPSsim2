package data;

/**
 * User: Cynric
 * Date: 15/5/20
 * Time: 10:08
 */
public class Instruction {
    private int[] args;
    private String content;
    private String name;
    private String printValue;
    private Integer Fi;
    private Integer Fj;
    private Integer Fk;
    private Integer immediate;
    private Integer address;
    private Integer readedData;

    public Integer getReadedData() {
        return readedData;
    }

    public void setReadedData(Integer readedData) {
        this.readedData = readedData;
    }

    public Integer getAddress() {
        return address;
    }

    public void setAddress(Integer address) {
        this.address = address;
    }

    public Integer getFi() {
        return Fi;
    }

    public void setFi(Integer fi) {
        Fi = fi;
    }

    public Integer getFj() {
        return Fj;
    }

    public void setFj(Integer fj) {
        Fj = fj;
    }

    public Integer getFk() {
        return Fk;
    }

    public void setFk(Integer fk) {
        Fk = fk;
    }

    public Integer getImmediate() {
        return immediate;
    }

    public void setImmediate(Integer immediate) {
        this.immediate = immediate;
    }

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
