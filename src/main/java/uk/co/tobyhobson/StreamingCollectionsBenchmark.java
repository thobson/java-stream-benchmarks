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
import java.util.concurrent.TimeUnit;

/**
 * Benchmark results using Java 8 Streams. The benchmark is performed using the JMH profiling framework.
 * Note: it's important to return the results of the calculations to prevent the JVM from dropping dead code
 * which would give us crazy results.
 *
 * @author Toby Hobson toby.hobson at gmail.com
 * @see <a href="http://java-performance.info/jmh/">JMH Introduction</a>
 */
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 10) // Run the code 5 times before we start to measure
@Measurement(iterations = 10) // Run the code 10 times and get the average values

// This is important. By default JMH runs benchmarks using multiple threads but we want to disable this
// as we are timing multi threaded code.
@Threads(1)

// For more accurate results each benchmark should be executed in a new JVM but in this case it makes little
// difference. See the JMH documentation
@Fork(0)
public class StreamingCollectionsBenchmark {

    private static final int COLLECTION_SIZE = 1_000_000;

    List<Integer> linkedListValues, arrayListValues;
    int[] arrayValues;

    // During setup we calculate the expected sum using a reliable for loop. We can then enable assertions (java -ea)
    // to verify that our algorithms actually work!
    long expectedCount;


    /**
     * Creates an array, LinkedList and ArrayList with 1 million random Integers. The same numbers are used across
     * each collection type i.e. arrayValues[10] == arrayListValues.get(10) == linkedListValues.get(10)
     */
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

        for (int value: arrayListValues) {
            expectedCount += value;
        }

    }

    /**
     * A naive implementation which uses a stream with reduce() to sum the values
     * @return total of all linkedList values
     */
    @Benchmark
    public long linkedListStream() {
        long sum = linkedListValues.stream().reduce((l, r) -> l + r).get();
        assert sum == expectedCount;
        return sum;
    }

    /**
     * Same as linkedListStream() but using a parallel stream
     * @return total of all linkedList values
     */
    @Benchmark
    public long parallelLinkedListStream() {
        long sum = linkedListValues.parallelStream().reduce((l, r) -> l + r).get();
        assert sum == expectedCount;
        return sum;
    }

    /**
     * A faster stream implementation which uses an ArrayList
     * @return total of all arrayList values
     */
    @Benchmark
    public long arrayListStream() {
        long sum = arrayListValues.stream().reduce((l, r) -> l + r).get();
        assert sum == expectedCount;
        return sum;
    }

    /**
     * Parallel version of arrayListStream()
     * @return total of all arrayList values
     */
    @Benchmark
    public long parallelArrayListStream() {
        long sum = arrayListValues.parallelStream().mapToLong(Integer::intValue).sum();
        assert sum == expectedCount;
        return sum;
    }

}
