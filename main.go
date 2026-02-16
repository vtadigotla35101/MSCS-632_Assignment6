package main

import (
	"fmt"
	"log"
	"os"
	"strings"
	"sync"
	"time"
)

// 1. Task Structure
type Task struct {
	ID   int
	Data string
}

// Process simulates work and returns a result string
func (t Task) Process() string {
	time.Sleep(100 * time.Millisecond) // Simulate delay
	return fmt.Sprintf("Task %d processed: %s", t.ID, strings.ToUpper(t.Data))
}

// 2. Worker Function
// Consumes tasks from 'jobs' channel, writes to 'results' channel
func worker(id int, jobs <-chan Task, results chan<- string, wg *sync.WaitGroup) {
	defer wg.Done() // Ensure we signal completion even if a panic occurs
	log.Printf("Worker %d started\n", id)

	for task := range jobs {
		// Error handling simulation (recover from panic if processing fails)
		func() {
			defer func() {
				if r := recover(); r != nil {
					log.Printf("Worker %d recovered from error on task %d: %v\n", id, task.ID, r)
				}
			}()

			// Process the task
			output := task.Process()
			results <- fmt.Sprintf("Worker %d: %s", id, output)
		}()
	}
	log.Printf("Worker %d finished\n", id)
}

// 3. File Writer
// Runs as a separate goroutine to handle file I/O safely
func resultWriter(filename string, results <-chan string, done chan<- bool) {
	f, err := os.Create(filename)
	if err != nil {
		log.Fatalf("Error creating file: %v", err)
		done <- false
		return
	}
	defer f.Close()

	for res := range results {
		_, err := f.WriteString(res + "\n")
		if err != nil {
			log.Printf("File write error: %v", err)
		}
	}
	done <- true
}

func main() {
	const numJobs = 20
	const numWorkers = 4
	filename := "go_output.txt"

	// 4. Concurrency Management via Channels
	jobs := make(chan Task, numJobs)
	results := make(chan string, numJobs)
	var wg sync.WaitGroup

	// Start File Writer Goroutine
	writeDone := make(chan bool)
	go resultWriter(filename, results, writeDone)

	// Start Worker Pool
	for w := 1; w <= numWorkers; w++ {
		wg.Add(1)
		go worker(w, jobs, results, &wg)
	}

	// 5. Load the Queue (Channel)
	log.Println("Main: Sending tasks...")
	for i := 1; i <= numJobs; i++ {
		jobs <- Task{ID: i, Data: fmt.Sprintf("data_item_%d", i)}
	}
	close(jobs) // Closes the channel so workers know when to stop

	// Wait for workers to finish
	wg.Wait()
	
	// Close results channel so writer knows to stop
	close(results)
	
	// Wait for writer to finish
	<-writeDone

	log.Printf("All tasks completed. Check %s\n", filename)
}