package spl.lae;

import parser.*;
import memory.*;
import scheduling.*;

import java.util.ArrayList; //imported for code structure
import java.util.List;

public class LinearAlgebraEngine {

    private SharedMatrix leftMatrix = new SharedMatrix();
    private SharedMatrix rightMatrix = new SharedMatrix();
    private TiredExecutor executor;

    public LinearAlgebraEngine(int numThreads) {
        // TODO: create executor with given thread count
        executor = new TiredExecutor(numThreads);
    }

    public ComputationNode run(ComputationNode computationRoot) {
        // TODO: resolve computation tree step by step until final matrix is produced
        try{
            while(computationRoot.getNodeType() != ComputationNodeType.MATRIX){
                ComputationNode compNode = computationRoot.findResolvable(); 
                compNode.associativeNesting();
                compNode = compNode.findResolvable();
                loadAndCompute(compNode);
                compNode.resolve(leftMatrix.readRowMajor());
            }
        }
        finally{
            try{
                System.out.println(getWorkerReport());
                executor.shutdown();
            }
            catch(InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }
        return computationRoot;
    }

    public void loadAndCompute(ComputationNode node) {
        // TODO: load operand matrices
        // TODO: create compute tasks & submit tasks to executor
        ComputationNodeType nodeType = node.getNodeType();
        List<ComputationNode> listNode = node.getChildren();
        List<Runnable> toSubmit = new ArrayList<>();
        if(nodeType.equals(ComputationNodeType.ADD)){
            leftMatrix.loadRowMajor(listNode.getFirst().getMatrix());
            rightMatrix.loadRowMajor(listNode.getLast().getMatrix());
            toSubmit = createAddTasks();
        }
        if(nodeType.equals(ComputationNodeType.MULTIPLY)){
            leftMatrix.loadRowMajor(listNode.getFirst().getMatrix());
            rightMatrix.loadColumnMajor(listNode.getLast().getMatrix());
            toSubmit = createMultiplyTasks();
        }
        if(nodeType.equals(ComputationNodeType.TRANSPOSE)){
            leftMatrix.loadRowMajor(listNode.getFirst().getMatrix());
            toSubmit = createTransposeTasks();
        }
        if(nodeType.equals(ComputationNodeType.NEGATE)){
            leftMatrix.loadRowMajor(listNode.getFirst().getMatrix());
            toSubmit = createNegateTasks();
        }
        executor.submitAll(toSubmit);
    }

    public List<Runnable> createAddTasks() {
        // TODO: return tasks that perform row-wise addition
        List<Runnable> addOutput = new ArrayList<>();
        // if(leftMatrix.length() != rightMatrix.length()){
        //     throw new IllegalArgumentException("[createAddTasks]: Matrix lengths don't match");
        // }
        for(int i = 0; i < leftMatrix.length(); i++){
            SharedVector left = leftMatrix.get(i);
            SharedVector right = rightMatrix.get(i);
            Runnable addRun = () -> {left.add(right);};
            addOutput.add(addRun);
        }
        return addOutput;
    }
 
    public List<Runnable> createMultiplyTasks() {
        // TODO: return tasks that perform row × matrix multiplication
        List<Runnable> mulOutput = new ArrayList<>();
        for(int i = 0; i < leftMatrix.length(); i++){
            SharedVector left = leftMatrix.get(i);
            Runnable mulRun = () -> {left.vecMatMul(rightMatrix);};
            mulOutput.add(mulRun);
        }
        return mulOutput;
    }

    public List<Runnable> createNegateTasks() {
        // TODO: return tasks that negate rows
        List<Runnable> negateOutput = new ArrayList<>();
        for(int i = 0; i < leftMatrix.length(); i++){
            SharedVector negateVector = leftMatrix.get(i);
            Runnable negRun = () -> {negateVector.negate();};
            negateOutput.add(negRun);
        }
        return negateOutput;
    }

    public List<Runnable> createTransposeTasks() {
        // TODO: return tasks that transpose rows
        List<Runnable> transOutput = new ArrayList<>();
        for(int i = 0; i < leftMatrix.length(); i++){
            SharedVector transVector = leftMatrix.get(i);
            Runnable transRun = () -> {transVector.transpose();};
            transOutput.add(transRun);
        }
        return transOutput;
    }

    public String getWorkerReport() {
        // TODO: return summary of worker activity
        return executor.getWorkerReport();
    }


}
