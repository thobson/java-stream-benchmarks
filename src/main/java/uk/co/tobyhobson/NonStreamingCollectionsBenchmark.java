/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
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

@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
@Fork(0)
@Threads(1)
public class NonStreamingCollectionsBenchmark {

    private static final int COLLECTION_SIZE = 1_000_000;

    List<Integer> linkedListValues, arrayListValues;
    int[] arrayValues;
    long expectedCount;
    ExecutorService executorService;

//    @Param({"-1", "1", "2", "3", "4"})
    public int numThreads = 3;

    @Setup
    public void setup() {
        linkedListValues = new LinkedList<>();
        arrayListValues = new ArrayList<>();
        arrayValues = new int[COLLECTION_SIZE];

        Random random = new Random(System.currentTimeMillis());
        for (int i=0; i<COLLECTION_SIZE; i++) {
            int randomValue = random.nextInt(10);
            linkedListValues.add(randomValue);
            arrayListValues.add(randomValue);
            arrayValues[i] = randomValue;
        }

        for (int value: arrayValues) {
            expectedCount += value;
        }

        executorService = Executors.newFixedThreadPool(numThreads);

    }

    @TearDown
    public void tearDown() {
        executorService.shutdown();
    }

    @Benchmark
    public long testPrimitiveLoop() {
        long sum = 0;
        for (int arrayValue : arrayValues) {
            sum += arrayValue;
        }
        assert sum == expectedCount;
        return sum;
    }

    @Benchmark
    public long testArrayBackedPrimitiveLoop() {
        long sum = 0;
        for (int arrayValue : arrayValues) {
            sum += arrayValue;
        }
        assert sum == expectedCount;
        return sum;
    }

    @Benchmark
    public long testParallelPrimitiveLoop() throws InterruptedException {
        AtomicLong totalSum = new AtomicLong(0);
        AtomicInteger totalInvocationCount = new AtomicInteger(0);
        int threadCount = numThreads != -1 ? numThreads : Runtime.getRuntime().availableProcessors();
        int chunkSize = arrayListValues.size() / threadCount;
        List<List<Integer>> chunks = new ArrayList<>();

        for (int j=0; j<threadCount; j++) {
            List<Integer> chunk = arrayListValues.subList(j * chunkSize, (j * chunkSize) + chunkSize);
            chunks.add(chunk);
        }

        Thread[] threads = new Thread[threadCount];
        for (int j=0; j<threadCount; j++) {
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

        for (int j=0; j<threadCount; j++) {
            try {
                threads[j].join();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        for (int i=arrayValues.length-1; i>=totalInvocationCount.get(); i--) {
            totalSum.getAndAdd(arrayValues[i]);
        }

        assert totalSum.get() == expectedCount;
        return totalSum.get();
    }

    @Benchmark
    public long testParallelPrimitiveLoopSharedCollection() throws InterruptedException {
        AtomicLong totalSum = new AtomicLong(0);
        AtomicInteger totalInvocationCount = new AtomicInteger(0);
        int threadCount = numThreads != -1 ? numThreads : Runtime.getRuntime().availableProcessors();
        int chunkSize = arrayValues.length / threadCount;

        Thread[] threads = new Thread[threadCount];
        for (int j=0; j<threadCount; j++) {
            final int k = j;

            Thread t = new Thread(() -> {
                int startPosition = k * chunkSize;
                int endPosition = startPosition + chunkSize;
                int localSum = 0;
                int invocationCount = 0;
                for (int index = startPosition; index < endPosition; index++) {
                    int arrayValue = arrayValues[index];
                    localSum += arrayValue;
                    invocationCount++;
                }
                totalSum.getAndAdd(localSum);
                totalInvocationCount.getAndAdd(invocationCount);
            });

            threads[j] = t;
            t.start();
        }

        for (int j=0; j<threadCount; j++) {
            try {
                threads[j].join();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        for (int i=arrayValues.length-1; i>=totalInvocationCount.get(); i--) {
            totalSum.getAndAdd(arrayValues[i]);
        }

        assert totalSum.get() == expectedCount;
        return totalSum.get();
    }

    @Benchmark
    public long testThreadPool() throws InterruptedException {
        AtomicInteger totalInvocationCount = new AtomicInteger(0);
        AtomicLong totalSum = new AtomicLong(0);
        int threadCount = numThreads != -1 ? numThreads : Runtime.getRuntime().availableProcessors();
        int chunkSize = arrayValues.length / threadCount;

        CountDownLatch latch = new CountDownLatch(numThreads);
        for (int j=0; j<threadCount; j++) {
            final int k = j;

            Runnable r = () -> {
                int startPosition = k * chunkSize;
                int endPosition = startPosition + chunkSize;
                int localSum = 0;
                int invocationCount = 0;
                for (int index = startPosition; index < endPosition; index++) {
                    int arrayValue = arrayValues[index];
                    localSum += arrayValue;
                    invocationCount++;
                }
                totalSum.addAndGet(localSum);
                totalInvocationCount.getAndAdd(invocationCount);
                latch.countDown();
            };

            executorService.submit(r);
        }

        latch.await();

        for (int i=arrayValues.length-1; i>=totalInvocationCount.get(); i--) {
            totalSum.getAndAdd(arrayValues[i]);
        }

        assert totalSum.get() == expectedCount;
        return totalSum.get();
    }

}
