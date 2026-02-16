Data Processing System: Concurrency Comparison
This project implements a parallel data processing system in two different programming languages: Java and Go. The application simulates multiple worker threads/goroutines retrieving tasks from a shared queue, processing them with a simulated delay, and logging results to a shared resource.

Features
Parallel Execution: Multiple workers process data simultaneously to improve throughput.
Thread Safety: * Java: Implemented using ReentrantLock and synchronized blocks.
Go: Implemented using channels and sync.WaitGroup (CSP model).
Error Handling: Robust handling of I/O exceptions and task processing failures.
Logging: Real-time console and file logging of worker status and processing results.

JAVA & GO Concurrency Models:
1.Java Implementation
The Java version follows the Shared Memory model.
Workers: Managed by an ExecutorService (Thread Pool).
Synchronization: Uses explicit locks to prevent race conditions when multiple threads access the SafeQueue.

2.Go Implementation
The Go version follows the Communicating Sequential Processes (CSP) model.
Workers: Lightweight goroutines.
Synchronization: Uses channels to pass data between the main process and workers, avoiding the need for explicit mutex locks in the business logic.