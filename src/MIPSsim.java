import model.Instruction;
import disassemb.Disassembler;
import simulator.PipelineSimulator;
import simulator.SimpleSimulator;
import simulator.Simulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * User: yichli
 */
public class MIPSsim {
    public static final String _disassembleFilePath = "./resource/disassembler.txt";
    public static final String _simulationFilePath = "./resource/simulation.txt";

    public static void main(String[] args) {

        Disassembler disassembler = new Disassembler();
        Simulator simulator;

        if (args.length > 1) {
            if ("1".equals(args[1])) {
                simulator = new SimpleSimulator();
            } else {
                simulator = new PipelineSimulator();
            }
        } else {
            simulator = new PipelineSimulator();
        }


        final String _inputFilePath = args[0];

        File file = new File(_inputFilePath);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                Instruction instruction = new Instruction(line);
                disassembler.disassemble(instruction);
                simulator.addInstr(instruction);
                if ("BREAK".equals(instruction.getName())) {
                    break;
                }
            }
            while ((line = reader.readLine()) != null) {
                simulator.addData(disassembler.parseData(line));
            }
            reader.close();

            simulator.printInstr(_disassembleFilePath);
            simulator.exec(_simulationFilePath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
