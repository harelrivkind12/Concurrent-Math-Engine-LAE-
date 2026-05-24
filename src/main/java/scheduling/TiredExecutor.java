package scheduling;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public TiredExecutor(int numThreads) {
        if(numThreads <= 0){
            throw new IllegalArgumentException("[TiredExecutor]: Number of threads must be positive!");
        }
        workers = new TiredThread[numThreads];
        for(int i = 0; i < workers.length; i++){
            TiredThread thread = new TiredThread(i, Math.random()+0.5); // range [0.5,1.5)
            workers[i] = thread;
            idleMinHeap.put(thread);
            thread.start(); //Kickstarting each thread
        }
    }

    public void submit(Runnable task) {
        // TODO
        checkCrash();
        try{
            TiredThread worker = idleMinHeap.take();
            Runnable wrappedTask = () -> { //wraps the task
                try{
                    long curStartTime=(System.nanoTime());
                    task.run();
                    worker.setTimes(curStartTime);
                    worker.setBusy(false);
                    idleMinHeap.put(worker);
                }
                catch(Exception e){
                    System.err.print(e.getMessage());
                    inFlight.set(Integer.MIN_VALUE + workers.length + 1); //flag for handling crashing gracefully
                    throw e;
                }
                finally{
                    inFlight.decrementAndGet();
                    synchronized(this){
                        this.notifyAll();
                    }
                }
            };
            inFlight.incrementAndGet();
            worker.newTask(wrappedTask);
        }
        catch(InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    public void submitAll(Iterable<Runnable> tasks){
        // TODO: submit tasks one by one and wait until all finish
        Iterator<Runnable> iter = tasks.iterator();
        while(iter.hasNext()){
            checkCrash();
            this.submit(iter.next());
        }
        synchronized(this){
            while(inFlight.get() > 0){
                checkCrash();
                try{
                    wait();   
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }    
            }
            checkCrash();
        }
    }

    public void shutdown() throws InterruptedException {
        // TODO
        for(int i = 0; i <workers.length;i++){ //Shuts down all workers one after the other
            workers[i].shutdown();                  
        }
        for(int i = 0; i < workers.length;i++){
            workers[i].join(); //Waits for all workers to terminate
        }
    }

    public synchronized String getWorkerReport() {
        // TODO: return readable statistics for each worker
        String output = "";
        for(int i = 0; i < workers.length; i++){ //All readable statistics for each worker
            TiredThread cur = workers[i];
            output += "Worker " + i + ":\n";
            output += "\tFatigue: " + cur.getFatigue() + "\n";
            output += "\tTime Used (ns): " + cur.getTimeUsed() + "\n";
            output += "\tTime Idle (ns): " + cur.getTimeIdle() + "\n";
            output += "\tIs Busy: " + cur.isBusy() + "\n";
        }
        output += "Overall Worker Fairness: " + calculateFairnessScore() + "\n";
        return output;
    }

    private void checkCrash(){
        if(inFlight.get() < 0){
            throw new IllegalThreadStateException("[checkCrash]: A thread has crashed");
        }
    }

    public double calculateFairnessScore() {
        if (workers == null || workers.length == 0) {
            return 0.0;
        }
        double totalFatigue = 0;
        for (TiredThread worker : workers) {
            totalFatigue += worker.getFatigue();
        }
        double averageFatigue = totalFatigue / workers.length;
        double sumOfSquaredDeviations = 0;
        for (TiredThread worker : workers) {
            double deviation = worker.getFatigue() - averageFatigue;
            sumOfSquaredDeviations += Math.pow(deviation, 2);
        }
        return sumOfSquaredDeviations;
    }
}
