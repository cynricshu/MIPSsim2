package simulator;

import data.Data;
import data.Instruction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Cynric
 * Date: 15/5/18
 * Time: 13:56
 */


public abstract class Simulator {
    public static final int PRE_ISSUE_SIZE = 4;
    List<Instruction> instrList = new ArrayList<>();
    List<Data> dataList = new ArrayList<>();
    int startAddress = 128;
    int PC = startAddress;
    int cycle = 1;
    int dataAddress;
    BufferedWriter writer;
    boolean isBreak = false;


    abstract public void exec(String outputPath);


    void writeRegister(BufferedWriter writer, Integer[] R) {
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
                writer.append("\t" + R[index]);
            }
            writer.append("\n\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void writeData(BufferedWriter writer) {
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

    public void addInstr(Instruction instr) {
        this.instrList.add(instr);
    }

    public void addData(Data data) {
        this.dataList.add(data);
    }

    void saveToMemory(int value, int address) {
        int index = (address - dataAddress) / 4;
        if (index >= 16) {
            System.out.println("error");
        }
        dataList.set(index, new Data(value));
    }

    int getMemoryData(int address) {
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

    Instruction getInstrByAddress(int address) {
        int i = (address - startAddress) / 4;
        return instrList.get(i);
    }
}
