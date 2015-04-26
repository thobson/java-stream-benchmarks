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

import java.util.*;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 10)
@Measurement(iterations = 10)
@Threads(1)
@Fork(5)
public class StreamingCollectionsBenchmark {

    private static final int COLLECTION_SIZE = 1_000_000;

    List<Integer> linkedListValues, arrayListValues;
    int[] arrayValues;
    long expectedCount;

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

//    @Benchmark
//    public long linkedListStream() {
//        long sum = linkedListValues.stream().reduce((l, r) -> l + r).get();
//        assert sum == expectedCount;
//        return sum;
//    }
//
//    @Benchmark
//    public long parallelLinkedListStream() {
//        long sum = linkedListValues.parallelStream().reduce((l, r) -> l + r).get();
//        assert sum == expectedCount;
//        return sum;
//    }
//
//    @Benchmark
//    public long arrayListStream() {
//        long sum = arrayListValues.stream().reduce((l, r) -> l + r).get();
//        assert sum == expectedCount;
//        return sum;
//    }
//
//    @Benchmark
//    public long parallelArrayListStream() {
//        long sum = arrayListValues.parallelStream().mapToLong(Integer::intValue).sum();
//        assert sum == expectedCount;
//        return sum;
//    }

}
