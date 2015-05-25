package simulator;

import data.Data;
import data.Instruction;
import data.Register;
import main.MIPSsim;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * User: Cynric
 * Date: 15/5/18
 * Time: 13:56
 */


public class Simulator {
    public static final int PRE_ISSUE_SIZE = 4;
    public Instruction execInstr;
    public Instruction waitInstr;
    public LinkedList<Instruction> inExecList = new LinkedList<>();
    public PipelineContext previousContext = new PipelineContext();
    public PipelineContext currentContext = new PipelineContext();
    public Register currentRegState = new Register();
    public Register prevRegState = new Register();
    List<Instruction> instrList = new ArrayList<>();
    List<Data> dataList = new ArrayList<>();
    int startAddress = 128;
    int PC = startAddress;
    int cycle = 1;
    int dataAddress;
    boolean stalled = false;
    boolean isBreak = false;
    BufferedWriter writer;

    public void exec() {
        dataAddress = startAddress + instrList.size() * 4;
        cycle = 1;
        try {
            writer = new BufferedWriter(new FileWriter(MIPSsim._simulationFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (!isBreak) {
            instrFetch();
            issue();
            execute();
            memory();
            writeBack();
            previousContext = new PipelineContext(currentContext);
            printCycle(writer);
            cycle++;
        }
        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void printCycle(BufferedWriter writer) {
        int index;

        String strWaitingIns = "";
        if (waitInstr != null) {
            strWaitingIns = "[" + waitInstr.getPrintValue() + "]";
        }

        String strExecutedIns = "";
        if (execInstr != null) {
            strExecutedIns = "[" + execInstr.getPrintValue() + "]";
        }

        String strPreIssueBuf[] = new String[]{"", "", "", ""};
        index = 0;
        for (Instruction ins : previousContext.preIssue) {
            strPreIssueBuf[index] = "[" + ins.getPrintValue() + "]";
            index++;
        }

        String strPreALUBuf[] = new String[]{"", ""};
        index = 0;
        for (Instruction ins : previousContext.preALU) {
            strPreALUBuf[index] = "[" + ins.getPrintValue() + "]";
            index++;
        }

        String strPostALUBuf = "";
        if (previousContext.postALU.size() >= 1) {
            strPostALUBuf = "[" + previousContext.postALU.getFirst().getPrintValue() + "]";
        }


        String strPreMEM = "";
        if (previousContext.preMEM.size() >= 1) {
            strPreMEM = "[" + previousContext.preMEM.getFirst().getPrintValue() + "]";
        }


        String strPostMEM = "";
        if (previousContext.postMEM.size() >= 1) {
            strPostMEM = "[" + previousContext.postMEM.getFirst().getPrintValue() + "]";
        }

        try {
            writer.append("--------------------\n");
            writer.append(String.format("Cycle:%d", cycle));
            writer.append("\n\n");
            writer.append("IF Unit:\n");
            writer.append(String.format("\tWaiting Instruction:%s\n", strWaitingIns));
            writer.append(String.format("\tExecuted Instruction:%s\n", strExecutedIns));

            writer.append("Pre-Issue Queue:\n");
            writer.append(String.format("\tEntry 0:%s\n", strPreIssueBuf[0]));
            writer.append(String.format("\tEntry 1:%s\n", strPreIssueBuf[1]));
            writer.append(String.format("\tEntry 2:%s\n", strPreIssueBuf[2]));
            writer.append(String.format("\tEntry 3:%s\n", strPreIssueBuf[3]));

            writer.append("Pre-ALU Queue:\n");
            writer.append(String.format("\tEntry 0:%s\n", strPreALUBuf[0]));
            writer.append(String.format("\tEntry 1:%s\n", strPreALUBuf[1]));

            writer.append(String.format("Pre-MEM Queue:%s\n", strPreMEM));
            writer.append(String.format("Post-MEM Queue:%s\n", strPostMEM));
            writer.append(String.format("Post-ALU Queue:%s\n", strPostALUBuf));

            writer.append("\n");
            writer.append("Registers");
            writeRegister(writer);
            writer.append("Data");
            writeData(writer);
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void writeRegister(BufferedWriter writer) {
        int index = 0;
        try {
            for (; index < 32; index++) {
                if ((index % 8) == 0) {
                    if (index < 10) {
                        writer.append("\nR0" + index + ":");
                    } else {
                        writer.append("\nR" + index + ":");
                    }
                }
                writer.append("\t" + prevRegState.registers[index]);
            }
            writer.append("\n\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeData(BufferedWriter writer) {
        int index = 0;
        int address = dataAddress;
        try {
            for (; index < dataList.size(); index++) {
                if ((index % 8) == 0) {
                    writer.append("\n" + address + ":");
                }
                writer.append("\t" + dataList.get(index).getValue());
                address += 4;
            }
            writer.append("\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void instrFetch() {
        int emptySlotSize = PRE_ISSUE_SIZE - previousContext.preIssue.size();
        boolean isBranchInstr;

        waitInstr = null;
        execInstr = null;
        isBranchInstr = false;

        if (emptySlotSize >= 1) {
            isBranchInstr = procFetch(1);
        }

        if (emptySlotSize >= 2 && !isBranchInstr) {
            procFetch(2);
        }
    }

    private boolean procFetch(int order) {
        boolean branch_inst = false;

        Instruction instr = getInstrByAddress(PC);

        if (instr.getName().equals("BREAK")) {
            isBreak = true;
            execInstr = instr;
            return true;
        }

        if (isBranchInstr(instr) && !isBreak) { // for branch instr, execute in IF period
            boolean branchHazard = false;
            branch_inst = true;

            if ("J".equals(instr.getName())) {
                stalled = false;
                PC = instr.getImmediate();
                waitInstr = null;
                execInstr = instr;
            } else {
                branchHazard = checkRAW(instr.getFj(), instr.getFk(), previousContext.preIssue.size());
                // check RAW
                for (Instruction previousInstr : currentContext.preIssue) {
                    if (previousInstr.getFi() != null) {
                        if ((instr.getFj() != null && previousInstr.getFi().intValue() == instr.getFj())
                                || (instr.getFk() != null && previousInstr.getFi().intValue() == instr.getFk())) {
                            branchHazard = true;
                            break;
                        }
                    }
                }
            }

            if (branchHazard) {
                stalled = true;
                waitInstr = instr;
                execInstr = null;
            } else {
                stalled = false;
                switch (instr.getName()) {
                    case "BEQ":
                        if (prevRegState.registers[instr.getFj()] == prevRegState.registers[instr.getFk()]) {
                            PC = PC + instr.getImmediate();
                        }
                        PC += 4;
                        break;
                    case "BGTZ":
                        if (prevRegState.registers[instr.getFj()] > 0) {
                            PC = PC + instr.getImmediate();
                        }
                        PC += 4;
                        break;
                }
                waitInstr = null;
                execInstr = instr;
            }
        }

        if (!branch_inst && !stalled) {
            currentContext.preIssue.addLast(instr);
            PC += 4;
        } else if (isBranchInstr(instr) && isBreak) {
            execInstr = instr;
            PC += 4;
        }
        return branch_inst;
    }

    private boolean isBranchInstr(Instruction instruction) {
        String instrName = instruction.getName();
        if ("BGTZ".equals(instrName) || "J".equals(instrName) || "BEQ".equals(instrName)) {
            return true;
        }
        return false;
    }

    private boolean checkRAW(Integer reg1, Integer reg2, int lastIndex) {
        boolean hazard = false;

        for (int i = 0; i < lastIndex; i++) {
            Instruction instr = previousContext.preIssue.get(i);
            if (instr.getFi() != null) {
                if ((reg1 != null && instr.getFi().intValue() == reg1) || (reg2 != null && instr.getFi().intValue() == reg2)) {
                    hazard = true;
                    break;
                }
            }
        }
        for (Instruction instr : inExecList) {
            if (instr.getFi() != null) {
                if ((reg1 != null && instr.getFi().intValue() == reg1) || (reg2 != null && instr.getFi().intValue() == reg2)) {
                    hazard = true;
                    break;
                }
            }
        }
        return hazard;
    }

    private boolean checkWAW(Integer reg, int lastIndex) {
        boolean hazard = false;

        for (int i = 0; i < lastIndex; i++) {
            Instruction instr = previousContext.preIssue.get(i);
            if (instr.getFi() != null) {
                if (reg != null && instr.getFi().intValue() == reg) {
                    hazard = true;
                    break;
                }
            }
        }

        for (Instruction instr : inExecList) {
            if (instr.getFi() != null) {
                if (reg != null && instr.getFi().intValue() == reg) {
                    hazard = true;
                    break;
                }
            }
        }
        return hazard;
    }

    private boolean checkWAR(Integer reg, int lastIndex) {
        boolean hazard = false;

        //check with earlier not issued instructions 
        for (int i = 0; i < lastIndex; i++) {
            Instruction instr = previousContext.preIssue.get(i);
            if ((instr.getFj() != null && instr.getFj().intValue() == reg) || (instr.getFk() != null && instr.getFk().intValue() == reg)) {
                hazard = true;
                break;
            }
        }

        return hazard;
    }

    boolean checkSW(Integer lastIndex) {
        boolean sw_exists = false;

        for (int i = 0; i < lastIndex; i++) {
            Instruction instr = previousContext.preIssue.get(i);
            if ("SW".equals(instr.getName())) {
                sw_exists = true;
                break;
            }
        }
        return sw_exists;
    }

    private void writeBack() {
        Instruction instr;

        if (previousContext.postALU.size() >= 1) {
            instr = currentContext.postALU.remove();
            updateRegister(instr);
            inExecList.remove(instr);
        }

        if (previousContext.postMEM.size() >= 1) {
            instr = currentContext.postMEM.remove();
            prevRegState.registers[instr.getFi()] = currentRegState.registers[instr.getFi()] = getMemoryData(instr.getAddress());
            inExecList.remove(instr);
        }
    }

    void updateRegister(Instruction instr) {
        if (instr.getFi() != null) {
            prevRegState.registers[instr.getFi()] = currentRegState.registers[instr.getFi()];
        }
        if (instr.getFj() != null) {
            prevRegState.registers[instr.getFj()] = currentRegState.registers[instr.getFj()];
        }
    }

    private boolean checkAndIssue(Instruction instr, int order) {
        // if issue two instructions in one cycle, check if there is WAW of WAR hazards
        if (order == 2) {
            Instruction lastInstr = currentContext.preALU.getFirst();
            Integer reg = instr.getFi();
            if (reg != null) {
                // WAR
                if ((lastInstr.getFj() != null && lastInstr.getFj().intValue() == reg)
                        || (lastInstr.getFk() != null && lastInstr.getFk().intValue() == reg)) {
                    return true;
                }
                // WAW
                if (lastInstr.getFi() != null && lastInstr.getFi().intValue() == reg) {
                    return true;
                }
            }
        }
        int lastIndex = previousContext.preIssue.indexOf(instr);
        boolean hasRAW = false;
        boolean hasWAW = false;
        boolean hasWAR = false;
        boolean hasSW = false;
        boolean issued = false;

        String name = instr.getName();
        if ("SW".equals(name)) {
            //For SW t is R and getFj() is R
            hasRAW = checkRAW(instr.getFj(), instr.getFk(), lastIndex);

            if (previousContext.preALU.size() < 2 && currentContext.preALU.size() < 2 && !hasRAW) {
//                previousContext.preIssue.remove(instr);
                currentContext.preIssue.remove(instr);
                currentContext.preALU.addLast(instr);
                issued = true;
            }
        } else if ("LW".equals(name)) {
            //For LW t is W and getFj() is R
            hasSW = checkSW(lastIndex);
            hasRAW = checkRAW(instr.getFj(), instr.getFk(), lastIndex);
            hasWAW = checkWAW(instr.getFi(), lastIndex);
            hasWAR = checkWAR(instr.getFi(), lastIndex);

            if (previousContext.preALU.size() < 2 && currentContext.preALU.size() < 2
                    && !hasRAW && !hasWAW && !hasWAR && !hasSW) {
//                previousContext.preIssue.remove(instr);
                currentContext.preIssue.remove(instr);
                currentContext.preALU.addLast(instr);
                issued = true;
            }
        } else if (!isBranchInstr(instr)) {
            hasRAW = checkRAW(instr.getFj(), instr.getFk(), lastIndex);
            hasWAW = checkWAW(instr.getFi(), lastIndex);
            hasWAR = checkWAR(instr.getFi(), lastIndex);

            if (previousContext.preALU.size() < 2 && currentContext.preALU.size() < 2 && !hasRAW && !hasWAW && !hasWAR) {
//                previousContext.preIssue.remove(instr);
                currentContext.preIssue.remove(instr);
                currentContext.preALU.addLast(instr);
                issued = true;
            }
        }

        if (issued) {
            inExecList.addLast(instr);
        }
        return issued;

    }

    private void issue() {
        int order = 1;

        //Create a copy of the buffer 
        List<Instruction> preIssueCopy = new ArrayList<>(previousContext.preIssue);

        for (Instruction instr : preIssueCopy) {
            if (checkAndIssue(instr, order)) {
                order++;
            }
            if (order == 3) {
                break;
            }
        }
    }

    private void execute() {
        Instruction instr;

        if (previousContext.preALU.size() >= 1) {
            instr = currentContext.preALU.remove();
            String name = instr.getName();
            if ("LW".equals(name) || "SW".equals(name)) { // calculate address only
                int address;
                switch (name) {
                    case "LW":
                        address = prevRegState.registers[instr.getFj()] + instr.getImmediate();
                        instr.setAddress(address);
                        currentContext.preMEM.addLast(instr);
                        break;
                    case "SW":
                        address = prevRegState.registers[instr.getFk()] + instr.getImmediate();
                        instr.setAddress(address);
                        currentContext.preMEM.addLast(instr);
                        break;
                    default:
                        break;
                }
            } else {
                executeInstruction(instr);
                currentContext.postALU.addLast(instr);
            }
        }

    }

    private void memory() {
        Instruction instr;
        if (previousContext.preMEM.size() > 0) {
            instr = currentContext.preMEM.remove();
            String name = instr.getName();
            int address = instr.getAddress();
            switch (name) {
                case "LW":
                    int readedData = getMemoryData(address);
                    instr.setReadedData(readedData);
                    currentContext.postMEM.addLast(instr);
                    break;
                case "SW":
                    saveToMemory(prevRegState.registers[instr.getFj()], address);
                    break;
                default:
                    break;
            }
        }
    }

    private void executeInstruction(Instruction instr) {
        switch (instr.getName()) {
            // category 2
            case "ADD":
                currentRegState.registers[instr.getFi()] = prevRegState.registers[instr.getFj()] + prevRegState.registers[instr.getFk()];
                break;
            case "SUB":
                currentRegState.registers[instr.getFi()] = prevRegState.registers[instr.getFj()] - prevRegState.registers[instr.getFk()];
                break;
            case "MUL":
                currentRegState.registers[instr.getFi()] = prevRegState.registers[instr.getFj()] * prevRegState.registers[instr.getFk()];
                break;
            case "AND":
                currentRegState.registers[instr.getFi()] = prevRegState.registers[instr.getFj()] & prevRegState.registers[instr.getFk()];
                break;
            case "OR":
                currentRegState.registers[instr.getFi()] = prevRegState.registers[instr.getFj()] | prevRegState.registers[instr.getFk()];
                break;
            case "XOR":
                currentRegState.registers[instr.getFi()] = prevRegState.registers[instr.getFj()] ^ prevRegState.registers[instr.getFk()];
                break;
            case "NOR":
                currentRegState.registers[instr.getFi()] = ~(prevRegState.registers[instr.getFj()] | prevRegState.registers[instr.getFk()]);
                break;
            // caetgory 3
            case "ADDI":
                currentRegState.registers[instr.getFi()] = prevRegState.registers[instr.getFj()] + instr.getImmediate();
                break;
            case "ANDI":
                currentRegState.registers[instr.getFi()] = prevRegState.registers[instr.getFj()] & instr.getImmediate();
                break;
            case "ORI":
                currentRegState.registers[instr.getFi()] = ~(prevRegState.registers[instr.getFj()] | instr.getImmediate());
                break;
            case "XORI":
                currentRegState.registers[instr.getFi()] = prevRegState.registers[instr.getFj()] ^ instr.getImmediate();
                break;
            default:
                break;
        }
    }


    public void addInstr(Instruction instr) {
        this.instrList.add(instr);
    }

    public void addData(Data data) {
        this.dataList.add(data);
    }

    private void saveToMemory(int value, int address) {
        int index = (address - dataAddress) / 4;
        if (index >= 16) {
            System.out.println("error");
        }
        dataList.set(index, new Data(value));
    }

    private int getMemoryData(int address) {
        Data data = dataList.get((address - dataAddress) / 4);
        return data.getValue();
    }

    public void printInstr(String outputFilePath) {
        try {
            FileWriter writer = new FileWriter(new File(outputFilePath));
            int position = startAddress;
            for (Instruction i : instrList) {
                writer.write(i.getContent() + "\t");
                writer.write(String.valueOf(position) + "\t");
                writer.write(i.getPrintValue() + "\n");
                position += 4;
            }
            for (Data data : dataList) {
                writer.write(data.getContent() + "\t");
                writer.write(position + "\t");
                writer.write(data.getValue() + "\n");
                position += 4;
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Instruction getInstrByAddress(int address) {
        int i = (address - startAddress) / 4;
        return instrList.get(i);
    }
}
