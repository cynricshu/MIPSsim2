package simulator;

import data.Instruction;

import java.util.LinkedList;

/**
 * User: Cynric
 * Date: 15/5/24
 * Time: 14:59
 */
public class PipelineContext {
    public LinkedList<Instruction> preIssue = new LinkedList<>();
    public LinkedList<Instruction> preALU = new LinkedList<>();
    public LinkedList<Instruction> preMEM = new LinkedList<>();
    public LinkedList<Instruction> postMEM = new LinkedList<>();
    public LinkedList<Instruction> postALU = new LinkedList<>();

    public PipelineContext() {
    }

    public PipelineContext(PipelineContext pipelineContext) {
        this.preIssue = new LinkedList<>(pipelineContext.preIssue);
        this.preALU = new LinkedList<>(pipelineContext.preALU);
        this.preMEM = new LinkedList<>(pipelineContext.preMEM);
        this.postMEM = new LinkedList<>(pipelineContext.postMEM);
        this.postALU = new LinkedList<>(pipelineContext.postALU);
    }
}
