package simulator;

import data.Data;
import data.Instruction;
import main.MIPSsim;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Cynric
 * Date: 15/5/24
 * Time: 13:56
 */


public class Simulator {
    List<Instruction> instrList = new ArrayList<>();
    List<Data> dataList = new ArrayList<>();
    int startAddress = 128;
    int position = startAddress;
    int cycle = 1;
    int dataAddress;
    int R[] = new int[32];
    boolean isBreak = false;
    FileWriter writer;

    public Simulator() {
        int i = 0;
        for (; i < R.length; i++) {
            R[i] = 0;
        }
        try {
            writer = new FileWriter(new File(MIPSsim._simulationFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void addInstr(Instruction instr) {
        this.instrList.add(instr);
    }

    public void addData(Data data) {
        this.dataList.add(data);
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
}
