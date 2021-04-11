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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
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
 * Collection of benchmarks that measure the throughput of writing JSON to a byte array using an
 * ObjectMapper.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(10)
public class ObjectMapperWriteObjectAsBytes {

  /**
   * State used by the benchmarks to hold onto an {@link ObjectMapper} and a test object so their
   * construction is not considered part of the test.
   */
  @State(Scope.Benchmark)
  public static class StateJson {

    static final ObjectMapper objectMapper;

    static {
      objectMapper = new ObjectMapper();

      objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
      objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    Map<String, String> rawObject;

    final Random rng = new Random();

    @Setup(Level.Iteration)
    public void onIterationSetup() throws JsonProcessingException {
      Map<String, String> dictionary = new LinkedHashMap<>();
      int size = rng.nextInt(40) + 10;
      for (int i = 0; i < size; ++i) {
        int keySize = rng.nextInt(5) + 5;
        String key = randomString(keySize);
        int valueSize = rng.nextInt(15) + 5;
        String value = randomString(valueSize);
        dictionary.put(key, value);
      }

      rawObject = dictionary;
    }

    private String randomString(int length) {
      char[] c = new char[length];
      for (int i = 0; i < length; ++i) {
        c[i] = (char) ('a' + rng.nextInt('z' - 'a'));
      }
      return new String(c);
    }
  }

  /** Benchmark that measures the throughput of writing an object directly to a byte array. */
  @Benchmark
  public byte[] writeBytes(StateJson state) throws JsonProcessingException {
    return StateJson.objectMapper.writeValueAsBytes(state.rawObject);
  }

  /**
   * Benchmark that measures the throughput of writing an object to a String that is then converted
   * to a byte array.
   */
  @Benchmark
  public byte[] writeStringConvertToBytes(StateJson state) throws JsonProcessingException {
    String jsonString = StateJson.objectMapper.writeValueAsString(state.rawObject);
    return jsonString.getBytes(StandardCharsets.UTF_8);
  }
}
