package disassemb;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import data.*;

/**
 * User: Cynric
 * Date: 15/5/24
 * Time: 10:06
 */
public class Disassembler {
    private static final String category1Pattern = "000(?<opcode>\\d{3})\\d{26}";
    private static final String category2Pattern = "110(?<rs>\\d{5})(?<rt>\\d{5})(?<opcode>\\d{3})(?<rd>\\d{5})\\d{11}";
    private static final String category3Pattern = "111(?<rs>\\d{5})(?<rt>\\d{5})(?<opcode>\\d{3})(?<imValue>\\d{16})";

    public Disassembler() {
    }

    public void disassemble(Instruction instruction) {
        String head = instruction.getContent().substring(0, 3);
        switch (head) {
            case "000":
                processCategory1(instruction);
                break;
            case "110":
                processCategory2(instruction);
                break;
            case "111":
                processCategory3(instruction);
                break;
            default:
                break;
        }
    }

    public Data parseData(String line) {
        Data data = new Data();
        data.setContent(line);
        int symbolFlag = 0;
        symbolFlag = line.charAt(0) - '0';
        line = line.substring(1);

        char[] array = line.toCharArray();
        int num = 0;
        for (int i = 0; i != array.length; i++) {
            int bit = (array[i] - '0') << (array.length - 1 - i);
            num = (num | bit);
        }
        data.setValue((symbolFlag << 31) | num);
        return data;
    }

    private static void processCategory1(Instruction instruction) {
        Pattern pattern = Pattern.compile(category1Pattern);
        Matcher matcher = pattern.matcher(instruction.getContent());
        if (matcher.matches()) {
            String opcode = matcher.group("opcode");
            InstrHandler handler = new Category1InstrHandler(opcode);
            handler.handle(instruction);
        }
    }

    private static void processCategory2(Instruction instruction) {
        Pattern pattern = Pattern.compile(category2Pattern);
        Matcher matcher = pattern.matcher(instruction.getContent());
        if (matcher.matches()) {
            InstrHandler handler = new Category2InstrHandler(matcher);
            handler.handle(instruction);
        }
    }

    private static void processCategory3(Instruction instruction) {
        Pattern pattern = Pattern.compile(category3Pattern);
        Matcher matcher = pattern.matcher(instruction.getContent());
        if (matcher.matches()) {
            InstrHandler handler = new Category3InstrHandler(matcher);
            handler.handle(instruction);
        }
    }
}


abstract class InstrHandler {
    public Integer parseInt(String s) {
        return Integer.parseInt(s);
    }

    public abstract void handle(Instruction instruction);

    public Integer binToDec(String binary) {
        return Integer.valueOf(binary, 2);
    }
}

class Category1InstrHandler extends InstrHandler {
    private Map<String, String[]> map = new HashMap<>();
    private String opcode;
    private Matcher matcher;
    private Instruction instruction;

    public Category1InstrHandler(String opcode) {
        map.put("000", new String[]{"J", "000000(?<instrIndex>\\d{26})"});
        map.put("010", new String[]{"BEQ", "000010(?<rs>\\d{5})(?<rt>\\d{5})(?<offset>\\d{16})"});
        map.put("100", new String[]{"BGTZ", "000100(?<rs>\\d{5})\\d{5}(?<offset>\\d{16})"});
        map.put("101", new String[]{"BREAK", "\\d{32}"});
        map.put("110", new String[]{"SW", "000110(?<base>\\d{5})(?<rt>\\d{5})(?<offset>\\d{16})"});
        map.put("111", new String[]{"LW", "000111(?<base>\\d{5})(?<rt>\\d{5})(?<offset>\\d{16})"});

        this.opcode = opcode;
    }

    @Override
    public void handle(Instruction instruction) {

        this.instruction = instruction;
        String name = map.get(opcode)[0];
        instruction.setName(name);

        Pattern pattern = Pattern.compile(map.get(opcode)[1]);
        matcher = pattern.matcher(instruction.getContent());

        if (matcher.matches()) {
            try {
                Method method = this.getClass().getDeclaredMethod(name);
                method.invoke(this);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void J() {
        String instrIndex = matcher.group("instrIndex");
        Integer target = (binToDec(instrIndex)) << 2;
        instruction.setPrintValue(instruction.getName() + " #" + target);
        instruction.setArgs(new int[]{target});
    }

    public void BEQ() {
        String rs = matcher.group("rs");
        String rt = matcher.group("rt");
        String offset = matcher.group("offset");
        Integer target = (binToDec(offset)) << 2;
        String printValue = instruction.getName() + " R" + binToDec(rs) + ", R" + binToDec(rt) + ", #" + target;
        instruction.setPrintValue(printValue);
        instruction.setArgs(new int[]{binToDec(rs), binToDec(rt), target});
    }

    public void BGTZ() {
        String rs = matcher.group("rs");
        String offset = matcher.group("offset");
        Integer target = (binToDec(offset)) << 2;
        String printValue = instruction.getName() + " R" + binToDec(rs) + ", #" + target;
        instruction.setPrintValue(printValue);
        instruction.setArgs(new int[]{binToDec(rs), target});
    }

    public void BREAK() {
        instruction.setPrintValue("BREAK");
    }

    public void SW() {
        String base = matcher.group("base");
        String rt = matcher.group("rt");
        String offset = matcher.group("offset");
        String printValue = instruction.getName() + " R" + binToDec(rt) + ", " + binToDec(offset) + "(R" + binToDec(base) + ")";
        instruction.setPrintValue(printValue);
        instruction.setArgs(new int[]{binToDec(base), binToDec(rt), binToDec(offset)});
    }

    public void LW() {
        String base = matcher.group("base");
        String rt = matcher.group("rt");
        String offset = matcher.group("offset");
        String printValue = instruction.getName() + " R" + binToDec(rt) + ", " + binToDec(offset) + "(R" + binToDec(base) + ")";
        instruction.setPrintValue(printValue);
        instruction.setArgs(new int[]{binToDec(base), binToDec(rt), binToDec(offset)});
    }
}

class Category2InstrHandler extends InstrHandler {
    private Map<String, String> map = new HashMap<>();
    private Matcher matcher;

    public Category2InstrHandler(Matcher matcher) {
        super();
        this.matcher = matcher;
        map.put("000", "ADD");
        map.put("001", "SUB");
        map.put("010", "MUL");
        map.put("011", "AND");
        map.put("100", "OR");
        map.put("101", "XOR");
        map.put("110", "NOR");
    }

    @Override
    public void handle(Instruction instruction) {
        String opcode = this.matcher.group("opcode");
        String rs = this.matcher.group("rs");
        String rt = this.matcher.group("rt");
        String rd = this.matcher.group("rd");
        String name = map.get(opcode);
        String printValue = name + " R" + binToDec(rd) + ", R" + binToDec(rs) + ", R" + binToDec(rt);

        instruction.setPrintValue(printValue);
        instruction.setName(name);
        instruction.setArgs(new int[]{binToDec(rd), binToDec(rs), binToDec(rt)});
    }
}

class Category3InstrHandler extends InstrHandler {
    private Map<String, String> map = new HashMap<>();
    private Matcher matcher;

    public Category3InstrHandler(Matcher matcher) {
        super();
        this.matcher = matcher;
        map.put("000", "ADDI");
        map.put("001", "ANDI");
        map.put("010", "ORI");
        map.put("011", "XORI");
    }

    @Override
    public void handle(Instruction instruction) {
        String opcode = this.matcher.group("opcode");
        String rs = this.matcher.group("rs");
        String rt = this.matcher.group("rt");
        String imValue = this.matcher.group("imValue");
        String name = map.get(opcode);
        String printValue = name + " R" + binToDec(rt) + ", R" + binToDec(rs) + ", #" + binToDec(imValue);

        instruction.setPrintValue(printValue);
        instruction.setName(name);
        instruction.setArgs(new int[]{binToDec(rt), binToDec(rs), binToDec(imValue)});
    }
}
