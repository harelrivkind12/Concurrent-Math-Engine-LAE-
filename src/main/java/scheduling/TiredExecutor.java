package scheduling;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public TiredExecutor(int numThreads) {
        // TODO
        if (numThreads <= 0) {
            throw new IllegalArgumentException("Number of threads must be positive");
        }
        workers = new TiredThread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            TiredThread worker = new TiredThread(i, Math.random() + 0.5);
            workers[i] = worker;
            idleMinHeap.put(worker);
            worker.start();
        }
    }

    public void submit(Runnable task) {
        // TODO
    }

    public void submitAll(Iterable<Runnable> tasks) {
        // TODO: submit tasks one by one and wait until all finish
    }

    public void shutdown() throws InterruptedException {
        // TODO
        for(int i = 0; i<workers.length; i++){
            TiredThread worker = workers[i];
            worker.shutdown();
        }
        for(int i = 0; i<workers.length; i++){
            workers[i].join();
        }
    }

    public synchronized String getWorkerReport() {
        // TODO: return readable statistics for each worker
        String output = "";
        for(int i = 0;i<workers.length;i++){
            TiredThread worker = workers[i];
            output += "Worker " + i + ":\n";
            output += "\tFatigue: " + worker.getFatigue() + "\n";
            output += "\tTime Used (ns): " + worker.getTimeUsed() + "\n";
            output += "\tTime Idle (ns): " + worker.getTimeIdle() + "\n";
            output += "\tIs Busy: " + worker.isBusy() + "\n";
        }
        return output;
    }

    private void updateWorkers(){
        for(int i = 0; i < workers.length; i++){ //Handles fetching non-busy workers
            if(workers[i].getState().equals(Thread.State.TERMINATED)){
                throw new IllegalThreadStateException("[updateWorkers]: A thread has crashed and been terminated."); 
            }
            if(!workers[i].isBusy() && !idleMinHeap.contains(workers[i])){
                idleMinHeap.put(workers[i]);
                inFlight.decrementAndGet();
            }
            
        }
    }
}
