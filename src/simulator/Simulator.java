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
 * Date: 15/5/24
 * Time: 13:56
 */


public class Simulator {
    public static final int PRE_ISSUE_SIZE = 4;
    public static final int PRE_ALU_SIZE = 2;
    public static final int PRE_MEM_SIZE = 1;
    public static final int POST_MEM_SIZE = 1;
    public static final int POST_ALU_SIZE = 1;
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

    public Simulator() {

    }

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
            printCycle(writer, PC);
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

    private void printCycle(BufferedWriter wbuf, int PC) {
        int ind;

        String strWaitingIns = new String("");
        if (waitInstr != null) {
            strWaitingIns = "[" + waitInstr.getPrintValue() + "]";
        }

        String strExecutedIns = new String("");
        if (execInstr != null) {
            strExecutedIns = "[" + execInstr.getPrintValue() + "]";
        }

        String strPreIssueBuf[] = new String[4];
        strPreIssueBuf[0] = new String("");
        strPreIssueBuf[1] = new String("");
        strPreIssueBuf[2] = new String("");
        strPreIssueBuf[3] = new String("");
        ind = 0;
        for (Instruction ins : previousContext.preIssue) {
            strPreIssueBuf[ind] = "[" + ins.getPrintValue() + "]";
            ind++;
        }

        String strPreALUBuf[] = new String[2];
        strPreALUBuf[0] = new String("");
        strPreALUBuf[1] = new String("");
        ind = 0;
        for (Instruction ins : previousContext.preALU) {
            strPreALUBuf[ind] = "[" + ins.getPrintValue() + "]";
            ind++;
        }

        String strPostALUBuf = new String("");
        if (previousContext.postALU.size() >= 1) {
            strPostALUBuf = "[" + previousContext.postALU.getFirst().getPrintValue() + "]";
        }


        String strPreMEM = new String("");
        if (previousContext.preMEM.size() >= 1) {
            strPreMEM = "[" + previousContext.preMEM.getFirst().getPrintValue() + "]";
        }


        String strPostMEM = new String("");
        if (previousContext.postMEM.size() >= 1) {
            strPostMEM = "[" + previousContext.postMEM.getFirst().getPrintValue() + "]";
        }

        try {
            wbuf.append("--------------------\n");
            //wbuf.write( String.format("Cycle:%d PC:%d", cycle, PC) );
            wbuf.append(String.format("Cycle:%d", cycle));
            wbuf.append("\n\n");
            wbuf.append("IF Unit:\n");
            wbuf.append(String.format("\tWaiting Instruction:%s\n", strWaitingIns));
            wbuf.append(String.format("\tExecuted Instruction:%s\n", strExecutedIns));

            wbuf.append("Pre-Issue Queue:\n");
            wbuf.append(String.format("\tEntry 0:%s\n", strPreIssueBuf[0]));
            wbuf.append(String.format("\tEntry 1:%s\n", strPreIssueBuf[1]));
            wbuf.append(String.format("\tEntry 2:%s\n", strPreIssueBuf[2]));
            wbuf.append(String.format("\tEntry 3:%s\n", strPreIssueBuf[3]));

            wbuf.append("Pre-ALU Queue:\n");
            wbuf.append(String.format("\tEntry 0:%s\n", strPreALUBuf[0]));
            wbuf.append(String.format("\tEntry 1:%s\n", strPreALUBuf[1]));
//        wbuf.write(String.format("Post-ALU Buffer:%s\n", strPostALUBuf));

//        wbuf.write("Pre-ALUB Queue:\n");
//        wbuf.write(String.format("\tEntry 0:%s\n", strPreALUBBuf[0]));
//        wbuf.write(String.format("\tEntry 1:%s\n", strPreALUBBuf[1]));
//        wbuf.write(String.format("Post-ALUB Buffer:%s\n", strPostALUBBuf));

            wbuf.append(String.format("Pre-MEM Queue:%s\n", strPreMEM));
            wbuf.append(String.format("Post-MEM Queue:%s\n", strPostMEM));
            wbuf.append(String.format("Post-ALU Queue:%s\n", strPostALUBuf));

            wbuf.append("\n");
            wbuf.append("Registers");
            writeRegister(wbuf);
            wbuf.append("Data");
            writeData(wbuf);
            wbuf.flush();
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
        boolean branch_inst;

        //if(sim_exit) return;
        waitInstr = null;
        execInstr = null;
        branch_inst = false;

        //Fetch the 1st instruction
        if (emptySlotSize >= 1) {
            //branch_inst = procFetch(empty_slots >= 2);
            branch_inst = procFetch(1);
        }

        //Fetch the 2nd instruction
        if (emptySlotSize >= 2 && !branch_inst) {
            branch_inst = procFetch(2);
        }

    }

    private boolean procFetch(int order) {
        boolean branch_inst = false;

        Instruction inst = getInstrByAddress(PC);

        if (inst.getName().equals("BREAK")) {
            isBreak = true;
            execInstr = inst;
            return true;
        }

//        if (order == 1) {
//            // If the next instruction following even a branch inst is a break
//            // then we stop the simulation.. do not fetch the branch..
//            Instruction nextINS = getInstrByAddress(PC + 4);
//            if (nextINS.getName().equals("BREAK")) {
//                isBreak = true;
//                //return false;
//            }
//        }

        if (isBranchInstr(inst) && !isBreak) {
            boolean branchHazard = false;
            branch_inst = true;

            if ("J".equals(inst.getName())) {
                PC = inst.getImmediate();
                waitInstr = null;
                execInstr = inst;
            } else {
                branchHazard = chk_RAW(inst.getFj(), inst.getFk(), previousContext.preIssue.size());

                for (Instruction instr : currentContext.preIssue) {
                    if (instr.getFi() != null) {
                        if ((inst.getFj() != null && instr.getFi().intValue() == inst.getFj())
                                || (inst.getFk() != null && instr.getFi().intValue() == inst.getFk())) {
                            branchHazard = true;
                            break;
                        }
                    }
                }
            }

            if (branchHazard) {
                stalled = true;
                waitInstr = inst;
                execInstr = null;
            } else {
                stalled = false;
                switch (inst.getName()) {
                    // catetory1
                    case "J":
                        PC = inst.getImmediate();  // TODO check if need to -4
                        break;
                    case "BEQ":
                        if (prevRegState.registers[inst.getFj()] == prevRegState.registers[inst.getFk()]) {
                            PC = PC + inst.getImmediate();
                        }
                        PC += 4;
                        break;
                    case "BGTZ":
                        if (prevRegState.registers[inst.getFj()] > 0) {
                            PC = PC + inst.getImmediate();
                        }
                        PC += 4;
                        break;
                }
                waitInstr = null;
                execInstr = inst;
            }
        }

        //check if the current instruction is branch, which would not have been executed and
        //the next instruction is BREAK
        if (!branch_inst && !stalled) {
            currentContext.preIssue.addLast(inst);
            PC += 4;
        } else if (isBranchInstr(inst) && isBreak) {
            execInstr = inst;
            PC += 4;
        }

        //check if the current instruction is branch, which would not have been executed and
        //the next instruction is BREAK
//	if( (INS.instType == OPCode.BRANCH && sim_exit) || (INS.opcode == OPCode.NOP)) {
//		executedIns = INS;
//		PC += 4;
//	}
//	else if(!(INS.opcode == OPCode.BREAK) && !branch_inst && !(INS.opcode == OPCode.NOP) && !stalled) {
//		currentState.preIssue.buffer.addLast(INS);
//		PC += 4;
//	}
        return branch_inst;
    }

    private boolean isBranchInstr(Instruction instruction) {
        String instrName = instruction.getName();
        if ("BGTZ".equals(instrName)) {
            return true;
        }
        if ("J".equals(instrName)) {
            return true;
        }
        if ("BEQ".equals(instrName)) {
            return true;
        }
        return false;
    }

    private boolean chk_RAW(Integer reg1, Integer reg2, int lastIndex) {
        boolean hazard = false;

        //check with earlier not issued instructions 
        for (int i = 0; i < lastIndex; i++) {
            Instruction instr = previousContext.preIssue.get(i);
            if (instr.getFi() != null) {
                if ((reg1 != null && instr.getFi().intValue() == reg1) || (reg2 != null && instr.getFi().intValue() == reg2)) {
                    hazard = true;
                    break;
                }
            }
        }
        if (!hazard) {
            // check with instructions in the pipeline
            for (Instruction instr : inExecList) {
                if (instr.getFi() != null) {
                    if ((reg1 != null && instr.getFi().intValue() == reg1) || (reg2 != null && instr.getFi().intValue() == reg2)) {
                        hazard = true;
                        break;
                    }
                }
            }
        }
        return hazard;
    }

    private boolean chk_WAW(Integer reg, int lastIndex) {
        boolean hazard = false;

        //check with earlier not issued instructions 
        for (int i = 0; i < lastIndex; i++) {
            Instruction instr = previousContext.preIssue.get(i);
            if (instr.getFi() != null) {
                if (reg != null && instr.getFi().intValue() == reg) {
                    hazard = true;
                    break;
                }
            }
        }

        if (!hazard) {
            // check with instructions in the pipeline
            for (Instruction instr : inExecList) {
                if (instr.getFi() != null) {
                    if (reg != null && instr.getFi().intValue() == reg) {
                        hazard = true;
                        break;
                    }
                }
            }
        }
        return hazard;
    }

    private boolean chk_WAR(Integer reg, int lastIndex) {
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

    boolean chk_SWinst(Integer lastIndex) {
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
        Instruction inst;

        // chk postALU Buffer
        if (previousContext.postALU.size() >= 1) {
            inst = currentContext.postALU.remove();
            updateRegister(inst);
            inExecList.remove(inst);
        }

        //chk postMEM Buffer
        if (previousContext.postMEM.size() >= 1) {
            inst = currentContext.postMEM.remove();
            prevRegState.registers[inst.getFi()] = currentRegState.registers[inst.getFi()] = getMemoryData(inst.getAddress());
            inExecList.remove(inst);
        }
    }

    void updateRegister(Instruction inst) {
        if (inst.getFi() != null) {
            prevRegState.registers[inst.getFi()] = currentRegState.registers[inst.getFi()];
        }
        if (inst.getFj() != null) {
            prevRegState.registers[inst.getFj()] = currentRegState.registers[inst.getFj()];
        }
    }

    private boolean checkAndIssue(Instruction inst, int order) {
        // if issue two instructions in one cycle, check if there is WAW of WAR hazards
        if (order == 2) {
            Instruction lastInstr = currentContext.preALU.getFirst();
            Integer reg = inst.getFi();
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
        int lastIndex = previousContext.preIssue.indexOf(inst);
        boolean rRAW = false;
        boolean rWAW = false;
        boolean rWAR = false;
        boolean rMEM = false;
        boolean issued = false;

        String name = inst.getName();
        if ("SW".equals(name)) {
            //For SW t is R and getFj() is R
            rRAW = chk_RAW(inst.getFj(), inst.getFk(), lastIndex);

            if (previousContext.preALU.size() < 2 && currentContext.preALU.size() < 2 && !rRAW) {
                previousContext.preIssue.remove(inst);
                currentContext.preIssue.remove(inst);
                currentContext.preALU.addLast(inst);
                issued = true;
            }
        } else if ("LW".equals(name)) {
            //For LW t is W and getFj() is R
            rMEM = chk_SWinst(lastIndex);
            rRAW = chk_RAW(inst.getFj(), inst.getFk(), lastIndex);
            rWAW = chk_WAW(inst.getFi(), lastIndex);
            rWAR = chk_WAR(inst.getFi(), lastIndex);

            if (previousContext.preALU.size() < 2 && currentContext.preALU.size() < 2
                    && !rRAW && !rWAW && !rWAR && !rMEM) {
                previousContext.preIssue.remove(inst);
                currentContext.preIssue.remove(inst);
                currentContext.preALU.addLast(inst);
                issued = true;
            }
        } else if (!isBranchInstr(inst)) {
            rRAW = chk_RAW(inst.getFj(), inst.getFk(), lastIndex);
            rWAW = chk_WAW(inst.getFi(), lastIndex);
            rWAR = chk_WAR(inst.getFi(), lastIndex);

            if (previousContext.preALU.size() < 2 && currentContext.preALU.size() < 2 && !rRAW && !rWAW && !rWAR) {
                previousContext.preIssue.remove(inst);
                currentContext.preIssue.remove(inst);
                currentContext.preALU.addLast(inst);
                issued = true;
            }
        }

        //If the instruction is issued then push it into the execution buffer as well to keep track of it
        if (issued) {
            inExecList.addLast(inst);
        }
        return issued;

    }

    private void issue() {
        int order = 1;

        //Create a copy of the buffer 
        List<Instruction> preIssueCopy = new ArrayList<>(previousContext.preIssue);

        for (Instruction inst : preIssueCopy) {
            if (checkAndIssue(inst, order)) {
                order++;
            }
            if (order == 3) {
                break;
            }
        }
    }

    private void execute() {
        Instruction inst;

        //Execute ALU Instructions if any.. 
        if (previousContext.preALU.size() >= 1) {
            inst = currentContext.preALU.remove();
            String name = inst.getName();
            if ("LW".equals(name) || "SW".equals(name)) {
                int address;
                switch (name) {
                    case "LW":
                        address = prevRegState.registers[inst.getFj()] + inst.getImmediate();
                        inst.setAddress(address);
                        currentContext.preMEM.addLast(inst);
                        break;
                    case "SW":
                        address = prevRegState.registers[inst.getFk()] + inst.getImmediate();
                        inst.setAddress(address);
                        currentContext.preMEM.addLast(inst);
                        break;
                    default:
                        break;
                }
            } else {
                executeInstruction(inst);
                currentContext.postALU.addLast(inst);
            }
        }

    }

    private void memory() {
        Instruction inst;
        if (previousContext.preMEM.size() > 0) {
            inst = currentContext.preMEM.remove();
            String name = inst.getName();
            int address = inst.getAddress();
            switch (name) {
                case "LW":
                    int readedData = getMemoryData(address);
                    inst.setReadedData(readedData);
                    currentContext.postMEM.addLast(inst);
                    break;
                case "SW":
                    saveToMemory(prevRegState.registers[inst.getFj()], address);
                    break;
                default:
                    break;
            }
        }
    }

    private void executeInstruction(Instruction inst) {
        switch (inst.getName()) {
            // category 2
            case "ADD":
                currentRegState.registers[inst.getFi()] = prevRegState.registers[inst.getFj()] + prevRegState.registers[inst.getFk()];
                break;
            case "SUB":
                currentRegState.registers[inst.getFi()] = prevRegState.registers[inst.getFj()] - prevRegState.registers[inst.getFk()];
                break;
            case "MUL":
                currentRegState.registers[inst.getFi()] = prevRegState.registers[inst.getFj()] * prevRegState.registers[inst.getFk()];
                break;
            case "AND":
                currentRegState.registers[inst.getFi()] = prevRegState.registers[inst.getFj()] & prevRegState.registers[inst.getFk()];
                break;
            case "OR":
                currentRegState.registers[inst.getFi()] = prevRegState.registers[inst.getFj()] | prevRegState.registers[inst.getFk()];
                break;
            case "XOR":
                currentRegState.registers[inst.getFi()] = prevRegState.registers[inst.getFj()] ^ prevRegState.registers[inst.getFk()];
                break;
            case "NOR":
                currentRegState.registers[inst.getFi()] = ~(prevRegState.registers[inst.getFj()] | prevRegState.registers[inst.getFk()]);
                break;
            // caetgory 3
            case "ADDI":
                currentRegState.registers[inst.getFi()] = prevRegState.registers[inst.getFj()] + inst.getImmediate();
                break;
            case "ANDI":
                currentRegState.registers[inst.getFi()] = prevRegState.registers[inst.getFj()] & inst.getImmediate();
                break;
            case "ORI":
                currentRegState.registers[inst.getFi()] = ~(prevRegState.registers[inst.getFj()] | inst.getImmediate());
                break;
            case "XORI":
                currentRegState.registers[inst.getFi()] = prevRegState.registers[inst.getFj()] ^ inst.getImmediate();
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
