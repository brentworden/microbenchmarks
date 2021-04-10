/*
 * Copyright (c) 2021, Brent Worden
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
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
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.brentworden.microbenchmarks;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Collection of benchmarks that measure the throughput of iterating the values of a {@link HashMap}
 * using various techniques.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(10)
public class HashMapValueIteration {

  @State(Scope.Benchmark)
  public static class HashMapAccessState {
    final Map<Integer, Integer> map = new HashMap<>();

    final Random rng = new Random();

    int total = 0;

    @Setup(Level.Trial)
    public void onSetup() {
      map.clear();
      for (int i = 0; i < 50; ++i) {
        map.put(Integer.valueOf(i), Integer.valueOf(i));
      }
    }
  }

  /**
   * Benchmark that measures the throughput of iterating the values of a {@link HashMap} using the
   * {@link Map#entrySet} collection and calling the {@link Map.Entry#getValue} method on each
   * entry.
   */
  @Benchmark
  public Integer iterateEntries(HashMapAccessState state) {
    Integer value = null;
    for (Map.Entry<Integer, Integer> entry : state.map.entrySet()) {
      value = entry.getValue();
    }
    return value;
  }

  /**
   * Benchmark that measures the throughput of iterating the values of a {@link HashMap} using the
   * {@link Map#keys} collection and calling the {@link Map#get} method for each key.
   */
  @Benchmark
  public Integer iterateKeys(HashMapAccessState state) {
    Integer value = null;
    for (Integer key : state.map.keySet()) {
      value = state.map.get(key);
    }
    return value;
  }

  /**
   * Benchmark that measures the throughput of iterating the values of a {@link HashMap} using the
   * {@link Map#values} collection directly.
   */
  @Benchmark
  public Integer iterateValues(HashMapAccessState state) {
    Integer value = null;
    for (Integer key : state.map.values()) {
      value = key;
    }
    return value;
  }
}
