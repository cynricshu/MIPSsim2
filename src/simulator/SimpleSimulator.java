package simulator;

import model.Instruction;
import model.Register;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * User: yichli
 */
public class SimpleSimulator extends Simulator {
    Integer R[] = new Register().registers;

    public void exec(String outputPath) {
        dataAddress = startAddress + instrList.size() * 4;
        int oldPosition;
        String name;

        try {
            writer = new BufferedWriter(new FileWriter(outputPath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (!isBreak) {
            oldPosition = PC;
            Instruction instruction = getInstrByAddress(PC);
            name = instruction.getName();
            try {
                Method method = this.getClass().getDeclaredMethod(name, Instruction.class);
                method.invoke(this, instruction);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            printCycleResult(writer, oldPosition);
            PC += 4;
            cycle++;
        }
        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // start category 1 -------------------
    private void J(Instruction instruction) {
        int targetAddr = instruction.getArgs()[0] - 4;
        this.PC = targetAddr;
    }

    private void BEQ(Instruction instruction) {
        int[] args = instruction.getArgs();
        if (R[args[0]] == R[args[1]]) {
            PC += args[2];
        }
    }

    private void BGTZ(Instruction instruction) {
        int[] args = instruction.getArgs();
        if (R[args[0]] > 0) {
            PC += args[1];
        }
    }

    private void BREAK(Instruction instruction) {
        this.isBreak = true;
    }

    private void SW(Instruction instruction) {
        int[] args = instruction.getArgs();
        saveToMemory(R[args[1]], R[args[0]] + args[2]);
    }

    private void LW(Instruction instruction) {
        int[] args = instruction.getArgs();
        R[args[1]] = getMemoryData(R[args[0]] + args[2]);
    }

    // end category 1 -------------------

    private void ADD(Instruction instruction) {
        int[] args = instruction.getArgs();
        R[args[0]] = R[args[1]] + R[args[2]];
    }

    private void ADDI(Instruction instruction) {
        int[] args = instruction.getArgs();
        R[args[0]] = R[args[1]] + args[2];
    }

    private void SUB(Instruction instruction) {
        int[] args = instruction.getArgs();
        R[args[0]] = R[args[1]] - R[args[2]];
    }

    private void MUL(Instruction instruction) {
        int[] args = instruction.getArgs();
        R[args[0]] = R[args[1]] * R[args[2]];
    }

    private void AND(Instruction instruction) {
        int[] args = instruction.getArgs();
        R[args[0]] = R[args[1]] & R[args[2]];
    }

    private void ANDI(Instruction instruction) {
        int[] args = instruction.getArgs();
        R[args[0]] = R[args[1]] & args[2];
    }

    private void OR(Instruction instruction) {
        int[] args = instruction.getArgs();
        R[args[0]] = R[args[1]] | R[args[2]];
    }

    private void ORI(Instruction instruction) {
        int[] args = instruction.getArgs();
        R[args[0]] = R[args[1]] | args[2];
    }

    private void XOR(Instruction instruction) {
        int[] args = instruction.getArgs();
        R[args[0]] = R[args[1]] ^ R[args[2]];
    }

    private void XORI(Instruction instruction) {
        int[] args = instruction.getArgs();
        R[args[0]] = R[args[1]] ^ args[2];
    }

    private void NOR(Instruction instruction) {
        int[] args = instruction.getArgs();
        R[args[0]] = ~(R[args[1]] | R[args[2]]);
    }

    void printCycleResult(BufferedWriter writer, int oldPosition) {
        String hyphen = "--------------------";
        try {
            writer.append(hyphen + "\n");
            writer.append("Cycle:" + cycle + "\t" + oldPosition + "\t" + getInstrByAddress(oldPosition).getPrintValue());
            writer.append("\n\n");
            writer.append("Registers");
            writeRegister(writer, R);
            writer.append("Data");
            writeData(writer);
            writer.append("\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
