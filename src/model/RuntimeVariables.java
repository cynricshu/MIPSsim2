package model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * User: yichli
 */
public class RuntimeVariables {
    public List<Instruction> preIssue = new ArrayList<>();
    public List<Instruction> preALU = new ArrayList<>();
    public List<Instruction> preMEM = new ArrayList<>();
    public List<Instruction> postMEM = new ArrayList<>();
    public List<Instruction> postALU = new ArrayList<>();

    public RuntimeVariables() {
    }

    public RuntimeVariables(RuntimeVariables runtimeVariables) {
        this.preIssue = new ArrayList<>(runtimeVariables.preIssue);
        this.preALU = new ArrayList<>(runtimeVariables.preALU);
        this.preMEM = new ArrayList<>(runtimeVariables.preMEM);
        this.postMEM = new ArrayList<>(runtimeVariables.postMEM);
        this.postALU = new ArrayList<>(runtimeVariables.postALU);
    }
}
