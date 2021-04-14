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

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Collection of benchmarks that measure the throughput of accessing a lazy initialized singleton
 * that is created using several approaches.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(10)
public class LazyInitializeSingleton {

  public static class AtomicReferenceProvider {
    private static String createInstance(String defaultValue) {
      return defaultValue;
    }

    final AtomicReference<String> atomic = new AtomicReference<>(null);

    public String getInstance(String defaultValue) {
      return atomic.updateAndGet(x -> x == null ? createInstance(defaultValue) : x);
    }
  }

  public static class DoubleCheckedLockingProvider {
    private static String createInstance(String defaultValue) {
      return defaultValue;
    }

    private volatile String instance;

    public String getInstance(String defaultValue) {
      String localRef = instance;
      if (localRef == null) {
        synchronized (this) {
          localRef = instance;
          if (localRef == null) {
            instance = localRef = createInstance(defaultValue);
          }
        }
      }
      return localRef;
    }
  }

  @State(Scope.Benchmark)
  public static class StateProviders {
    AtomicReferenceProvider atomic = new AtomicReferenceProvider();
    DoubleCheckedLockingProvider locking = new DoubleCheckedLockingProvider();
    String nextValue = INITIAL_VALUE;

    @TearDown(Level.Iteration)
    public void onIterationTearDown() {
      nextValue = UUID.randomUUID().toString();
    }
  }

  private static final String INITIAL_VALUE = UUID.randomUUID().toString();

  @Benchmark
  public boolean lazyInitializeUsingAnAtomicReference(StateProviders state) {
    String instance = state.atomic.getInstance(state.nextValue);
    return instance.equals(INITIAL_VALUE);
  }

  @Benchmark
  public boolean lazyInitializeUsingDoubleCheckedLocking(StateProviders state) {
    String instance = state.locking.getInstance(state.nextValue);
    return instance.equals(INITIAL_VALUE);
  }
}
