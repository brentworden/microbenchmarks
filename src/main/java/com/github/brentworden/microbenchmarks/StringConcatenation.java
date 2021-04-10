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
 * Collection of benchmarks that measure the throughput of concatenating strings using several
 * approaches.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(10)
public class StringConcatenation {

  @State(Scope.Benchmark)
  public static class StateStringParts {
    String[] parts;

    final Random rng = new Random();

    @Setup(Level.Iteration)
    public void onIterationSetup() {
      parts =
          new String[] {
            randomString(10),
            randomString(10),
            randomString(10),
            randomString(10),
            randomString(10),
          };
    }

    private String randomString(int length) {
      char[] c = new char[length];
      for (int i = 0; i < length; ++i) {
        c[i] = (char) ('a' + rng.nextInt('z' - 'a'));
      }
      return new String(c);
    }
  }

  @Benchmark
  public String concatenationUsingAddition(StateStringParts state) {
    return state.parts[0] + state.parts[1] + state.parts[2] + state.parts[3] + state.parts[4];
  }

  @Benchmark
  public String concatenationUsingStringBuilder(StateStringParts state) {
    StringBuilder b = new StringBuilder();
    b.append(state.parts[0])
        .append(state.parts[1])
        .append(state.parts[2])
        .append(state.parts[3])
        .append(state.parts[4]);
    return b.toString();
  }

  @Benchmark
  public String concatenationUsingStringBuilderWithExactInitialCapacity(StateStringParts state) {
    StringBuilder b = new StringBuilder(50);
    b.append(state.parts[0])
        .append(state.parts[1])
        .append(state.parts[2])
        .append(state.parts[3])
        .append(state.parts[4]);
    return b.toString();
  }

  @Benchmark
  public String concatenationUsingStringBuilderWithLargeInitialCapacity(StateStringParts state) {
    StringBuilder b = new StringBuilder(60);
    b.append(state.parts[0])
        .append(state.parts[1])
        .append(state.parts[2])
        .append(state.parts[3])
        .append(state.parts[4]);
    return b.toString();
  }

  @Benchmark
  public String concatenationUsingStringBuilderWithSmallInitialCapacity(StateStringParts state) {
    StringBuilder b = new StringBuilder(40);
    b.append(state.parts[0])
        .append(state.parts[1])
        .append(state.parts[2])
        .append(state.parts[3])
        .append(state.parts[4]);
    return b.toString();
  }
}
