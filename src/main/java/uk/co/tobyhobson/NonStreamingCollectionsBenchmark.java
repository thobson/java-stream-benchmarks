/*
 * (C) Copyright 2015 Toby Hobson (https://www.tobyhobson.co.uk/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package uk.co.tobyhobson;

import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Benchmark results using Arrays, Lists, Loops and Threads. See also StreamingCollectionsBenchmark and
 * the JMH documentation to understand the class annotations used.
 *
 * @author Toby Hobson toby.hobson at gmail.com
 * @see StreamingCollectionsBenchmark
 */
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Threads(1)
@Fork(0)
public class NonStreamingCollectionsBenchmark {

    private static final int COLLECTION_SIZE = 1_000_000;

    List<Integer> linkedListValues, arrayListValues;
    int[] arrayValues;
    long expectedCount;
    ExecutorService executorService; // A thread pool implementation

    /**
     * JMH offers parametrised benchmarks which results in multiple runs of each benchmark.
     * We use this parameter to see how well the code performs with various threading options.
     * Adjust this to suit your own environment.
     *
     * -1 means use the OS reported processor count - Runtime.getRuntime().availableProcessors(). Note this may be
     * double what you expect if your CPU supports hyper threading e.g. I have a quad core i7 machine but the
     * OS thinks I have 8 processors
     */
    @Param({"-1", "1", "2", "3", "4", "5", "6", "7", "8"})
    public int numThreads;

    @Setup
    public void setup() {
        linkedListValues = new LinkedList<>();
        arrayListValues = new ArrayList<>();
        arrayValues = new int[COLLECTION_SIZE];

        Random random = new Random(System.currentTimeMillis());
        for (int i = 0; i < COLLECTION_SIZE; i++) {
            int randomValue = random.nextInt(10);
            linkedListValues.add(randomValue);
            arrayListValues.add(randomValue);
            arrayValues[i] = randomValue;
        }

        for (int value : arrayValues) {
            expectedCount += value;
        }

        if (numThreads == -1)
            executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        else
            executorService = Executors.newFixedThreadPool(numThreads);

    }

    @TearDown
    public void tearDown() {
        executorService.shutdown();
    }

    /**
     * The most basic (but quite performant) implementation. It uses a single threaded for-loop to iterate through
     * each element in the raw array
     *
     * @return total of all array values
     */
    @Benchmark
    public long primitiveLoop() {
        long sum = 0;
        for (int arrayValue : arrayValues) {
            sum += arrayValue;
        }
        assert sum == expectedCount;
        return sum;
    }

    /**
     * A multi threaded version of the for-loop. It splits the array into n smaller arrays where n = numThreads
     * and then starts n threads to process each chunk. An AtomicInteger is shared by all threads and each thread
     * calculates the sum of it's chunk and then adds to the AtomicInteger.
     *
     * A cleaner implementation would probably use Java Futures to avoid the shared variable
     *
     * @return total of the array values
     */
    @Benchmark
    public long parallelPrimitiveLoop() {
        final AtomicLong totalSum = new AtomicLong(0);
        final int threadCount = Runtime.getRuntime().availableProcessors();
        final int chunkSize = arrayValues.length / threadCount;
        final int remainder = arrayValues.length % chunkSize;
        int[][] chunks = new int[threadCount][chunkSize];

        for (int j = 0; j < threadCount; j++) {
            int[] chunk = new int[chunkSize];
            if (j == threadCount -1)
                System.arraycopy(arrayValues, j * chunkSize, chunk, 0, chunkSize + remainder);
            else
                System.arraycopy(arrayValues, j * chunkSize, chunk, 0, chunkSize);
            chunks[j] = chunk;
        }

        Thread[] threads = new Thread[threadCount];
        for (int j = 0; j < threadCount; j++) {
            int[] chunk = chunks[j];
            Thread t = new Thread(() -> {
                long localSum = 0;
                for (int arrayValue : chunk) {
                    localSum += arrayValue;
                }
                totalSum.getAndAdd(localSum);
            });
            threads[j] = t;
            t.start();
        }

        for (int j = 0; j < threadCount; j++) {
            try {
                threads[j].join();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        assert totalSum.get() == expectedCount;
        return totalSum.get();
    }

    /**
     * Similar to the parallelPrimitiveLoop() however it uses an ArrayList of Integers instead
     * of a raw array of ints (primitives). The split is much more efficient because the JVM can simply
     * reassign object references instead of needing the physically copy the values
     *
     * As with parallelPrimitiveLoop() A cleaner implementation would probably use Java Futures to avoid the shared variable
     *
     * @return total of the arrayList values
     */
    @Benchmark
    public long parallelListLoop() {
        AtomicLong totalSum = new AtomicLong(0);
        AtomicInteger totalInvocationCount = new AtomicInteger(0);
        final int threadCount = numThreads != -1 ? numThreads : Runtime.getRuntime().availableProcessors();
        final int chunkSize = arrayListValues.size() / threadCount;
        final int remainder = arrayValues.length % chunkSize;
        List<List<Integer>> chunks = new ArrayList<>();

        for (int j = 0; j < threadCount; j++) {
            List<Integer> chunk;
            if ( j == threadCount - 1)
                chunk = arrayListValues.subList(j * chunkSize, (j * chunkSize) + (chunkSize + remainder));
            else
                chunk = arrayListValues.subList(j * chunkSize, (j * chunkSize) + chunkSize);
            chunks.add(chunk);
        }

        Thread[] threads = new Thread[threadCount];
        for (int j = 0; j < threadCount; j++) {
            List<Integer> chunk = chunks.get(j);
            Thread t = new Thread(() -> {
                long localSum = 0;
                int invocationCount = 0;
                for (int arrayValue : chunk) {
                    localSum += arrayValue;
                    invocationCount++;
                }
                totalInvocationCount.getAndAdd(invocationCount);
                totalSum.getAndAdd(localSum);
            });
            threads[j] = t;
            t.start();
        }

        for (int j = 0; j < threadCount; j++) {
            try {
                threads[j].join();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        for (int i = arrayValues.length - 1; i >= totalInvocationCount.get(); i--) {
            totalSum.getAndAdd(arrayValues[i]);
        }

        assert totalSum.get() == expectedCount;
        return totalSum.get();
    }

    /**
     * Instead of copying the array into n chunks we share the array across n threads. Each thread works on it's own
     * section of the array. Splitting an array of 1 million entries is an expensive operation which this algorithm
     * avoids.
     *
     * A cleaner implementation would probably use Java Futures to avoid the shared variable
     *
     * @return total of the array values
     */
    @Benchmark
    public long sharedStateThreads() {
        final AtomicLong totalSum = new AtomicLong(0);
        final int threadCount = numThreads != -1 ? numThreads : Runtime.getRuntime().availableProcessors();
        final int chunkSize = arrayValues.length / threadCount;
        final int remainder = arrayValues.length % chunkSize;

        Thread[] threads = new Thread[threadCount];
        for (int j = 0; j < threadCount; j++) {
            final int k = j;

            Thread t = new Thread(() -> {
                int startPosition = k * chunkSize;
                int endPosition = 0;
                if (k == threadCount -1)
                    endPosition = startPosition + (chunkSize + remainder);
                else
                    endPosition = startPosition + chunkSize;
                int localSum = 0;
                for (int index = startPosition; index < endPosition; index++) {
                    int arrayValue = arrayValues[index];
                    localSum += arrayValue;
                }
                totalSum.getAndAdd(localSum);
            });

            threads[j] = t;
            t.start();
        }

        for (int j = 0; j < threadCount; j++) {
            try {
                threads[j].join();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        assert totalSum.get() == expectedCount;
        return totalSum.get();
    }

    /**
     * Similar to the sharedStateThreads() however this algorithm uses a preexisting thread pool (ExecutorService)
     * creating threads for short lived operations is expensive and best avoided. This algorithm is therefore the most
     * performant of all as it:
     *
     * 1. Uses a raw array of primitive types (avoid autoboxing and the collections overhead)
     * 2. Shares the array among the threads thus avoiding the expensive split operation
     * 3. Reuses existing threads to avoid the overhead of thread creation
     *
     * A cleaner implementation would probably use Java Futures to avoid the shared variable
     *
     * @return total of the array values
     */
    @Benchmark
    public long sharedStateThreadPool() {
        final AtomicLong totalSum = new AtomicLong(0);
        final int threadCount = numThreads != -1 ? numThreads : Runtime.getRuntime().availableProcessors();
        final int chunkSize = arrayValues.length / threadCount;
        final int remainder = arrayValues.length % chunkSize;

        // We need some means to know when the threads have done their work. With raw threads we can join them
        // but we don't have that option with an ExecutorService so we use a countdown latch. Again a cleaner
        // implementation would use Futures/Callable i.e. executorService.invokeAll(tasks)
        CountDownLatch latch;
        if (numThreads == -1)
            latch = new CountDownLatch(Runtime.getRuntime().availableProcessors());
        else
            latch = new CountDownLatch(numThreads);

        for (int j = 0; j < threadCount; j++) {
            final int k = j;

            Runnable r = () -> {
                int startPosition = k * chunkSize;
                int endPosition = 0;
                if (k == threadCount - 1)
                    endPosition = startPosition + chunkSize + remainder;
                else
                    endPosition = startPosition + chunkSize;
                int localSum = 0;
                for (int index = startPosition; index < endPosition; index++) {
                    int arrayValue = arrayValues[index];
                    localSum += arrayValue;
                }
                totalSum.addAndGet(localSum);
                latch.countDown();
            };

            executorService.submit(r);
        }

        try {
            latch.await();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        assert totalSum.get() == expectedCount;
        return totalSum.get();
    }

}
