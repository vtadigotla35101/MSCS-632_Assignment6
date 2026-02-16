import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// 1. Task Object
class Task {
    private final int id;
    private final String data;

    public Task(int id, String data) {
        this.id = id;
        this.data = data;
    }

    public String process() {
        // Simulate computational work
        try {
            Thread.sleep(100); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Task " + id + " processed: " + data.toUpperCase();
    }
    
    public int getId() { return id; }
}

// 2. Thread-Safe Queue (Shared Resource)
class SafeQueue {
    private final Queue<Task> queue = new LinkedList<>();
    private final Lock lock = new ReentrantLock();

    public void addTask(Task t) {
        lock.lock();
        try {
            queue.add(t);
        } finally {
            lock.unlock();
        }
    }

    public Task getTask() {
        lock.lock();
        try {
            return queue.poll(); // Returns null if empty
        } finally {
            lock.unlock();
        }
    }
    
    public boolean isEmpty() {
        lock.lock();
        try {
            return queue.isEmpty();
        } finally {
            lock.unlock();
        }
    }
}

// 3. Worker Thread
class Worker implements Runnable {
    private final int workerId;
    private final SafeQueue taskQueue;
    private final String outputFile;

    public Worker(int id, SafeQueue queue, String file) {
        this.workerId = id;
        this.taskQueue = queue;
        this.outputFile = file;
    }

    @Override
    public void run() {
        System.out.println("Worker " + workerId + " started.");
        
        try {
            while (true) {
                Task task = taskQueue.getTask();
                if (task == null) break; // Exit loop if queue is empty

                try {
                    String result = task.process();
                    logResult(result);
                } catch (Exception e) {
                    System.err.println("Worker " + workerId + " error processing task " + task.getId() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Worker " + workerId + " encountered critical error: " + e.getMessage());
        } finally {
            System.out.println("Worker " + workerId + " finished.");
        }
    }

    // Synchronized file writing to prevent race conditions on output
    private void logResult(String result) {
        synchronized (System.out) { // Using System.out lock as a proxy for file lock demo
            try (PrintWriter out = new PrintWriter(new FileWriter(outputFile, true))) {
                out.println("Worker " + workerId + ": " + result);
            } catch (IOException e) {
                System.err.println("Error writing to file: " + e.getMessage());
            }
        }
    }
}

// 4. Main Application
public class JavaDataProcessor {
    public static void main(String[] args) {
        SafeQueue sharedQueue = new SafeQueue();
        String filename = "java_output.txt";
        int totalTasks = 20;
        int threadCount = 4;

        // Load Queue
        System.out.println("Loading tasks...");
        for (int i = 1; i <= totalTasks; i++) {
            sharedQueue.addTask(new Task(i, "data_item_" + i));
        }

        // Start Workers using Executor
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 1; i <= threadCount; i++) {
            executor.execute(new Worker(i, sharedQueue, filename));
        }

        executor.shutdown();
        try {
            // Wait for all tasks to finish
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("All tasks completed in " + (endTime - startTime) + "ms. Check " + filename);
    }
}