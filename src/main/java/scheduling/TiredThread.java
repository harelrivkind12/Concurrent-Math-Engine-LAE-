package scheduling;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class TiredThread extends Thread implements Comparable<TiredThread> {      //test

    private static final Runnable POISON_PILL = () -> {}; // Special task to signal shutdown

    private final int id; // Worker index assigned by the executor
    private final double fatigueFactor; // Multiplier for fatigue calculation

    private final AtomicBoolean alive = new AtomicBoolean(true); // Indicates if the worker should keep running

    // Single-slot handoff queue; executor will put tasks here
    private final BlockingQueue<Runnable> handoff = new ArrayBlockingQueue<>(1);

    private final AtomicBoolean busy = new AtomicBoolean(false); // Indicates if the worker is currently executing a task

    private final AtomicLong timeUsed = new AtomicLong(0); // Total time spent executing tasks
    private final AtomicLong timeIdle = new AtomicLong(0); // Total time spent idle
    private final AtomicLong idleStartTime = new AtomicLong(0); // Timestamp when the worker became idle

    public TiredThread(int id, double fatigueFactor) {
        if(fatigueFactor < 0.5 || fatigueFactor >= 1.5){
            throw new IllegalArgumentException("[TiredThread]: fatigue factor must be 0.5 <=x< 1.5");
        }
        this.id = id;
        this.fatigueFactor = fatigueFactor;
        this.idleStartTime.set(System.nanoTime());
        setName(String.format("FF=%.2f", fatigueFactor));
    }

    public int getWorkerId() {
        return id;
    }

    public double getFatigue() {
        return fatigueFactor * timeUsed.get();
    }

    public boolean isBusy() {
        return busy.get();
    }

    public long getTimeUsed() {
        return timeUsed.get();
    }

    public long getTimeIdle() {
        return timeIdle.get();
    }

    /**
     * Assign a task to this worker.
     * This method is non-blocking: if the worker is not ready to accept a task,
     * it throws IllegalStateException.
     */
    public void newTask(Runnable task) {
        if(isBusy() || !handoff.isEmpty()){
            throw new IllegalStateException("[newTask]: Thread is busy! cannot get new task!");
        }
        if(!alive.get()){
            throw new IllegalStateException("[newTask]: Thread is DEAD! cannot get new task!");
        }
        busy.set(true); //Once handed a task, set busy to true
        handoff.offer(task);
    }
 

    /**
     * Request this worker to stop after finishing current task.
     * Inserts a poison pill so the worker wakes up and exits.
     */
    public void shutdown() {
        try{
            handoff.put(POISON_PILL);
        }
        catch(InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
       while(alive.get()){
            try{
                Runnable curtask=handoff.take();
                busy.set(true);
                if(curtask==POISON_PILL){ //Shut down signal
                    alive.set(false); 
                    break;
                }
                timeIdle.addAndGet(System.nanoTime()-idleStartTime.get());
                curtask.run(); //set busy false and put into queue happens here
                
            }
            catch(InterruptedException e){
                Thread.currentThread().interrupt(); //Shouldn't happen in our project.
            }
       }
    }

    @Override
    public int compareTo(TiredThread o) {
        double res= getFatigue()-o.getFatigue();
        if(res < 0) return -1;
        else if(res > 0) return 1;
        else return 0;
    }

    public void setBusy(boolean val){ // allows wrapped task in
                                    // TiredExecutor to adjust busy value
        busy.set(val);
    }
    public void setTimes(long curStartTime){ //updates System time for thread
        long curStopTime=(System.nanoTime());
        long TaskDuration=curStopTime-curStartTime;
        timeUsed.addAndGet(TaskDuration);
        idleStartTime.set(System.nanoTime());
    }
}