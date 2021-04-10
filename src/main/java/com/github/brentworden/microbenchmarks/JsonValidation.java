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
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
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
 * Collection of benchmarks that measure the throughput of validating JSON structure using Jackson.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(10)
public class JsonValidation {

  @State(Scope.Benchmark)
  public static class _State {
    static final JsonFactory jsonFactory;

    static final ObjectMapper objectMapper;

    static {
      objectMapper = new ObjectMapper();

      objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      objectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
      objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
      objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

      jsonFactory = objectMapper.getFactory();
    }

    String jsonString;

    String jsonStringInvalidBeginning;

    String jsonStringInvalidEnding;

    String jsonStringInvalidMiddle;

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

      jsonString = new ObjectMapper().writeValueAsString(dictionary);
      jsonStringInvalidBeginning = makeInvalidAtBeginning(jsonString);
      jsonStringInvalidEnding = makeInvalidAtEnding(jsonString);
      jsonStringInvalidMiddle = makeInvalidInMiddle(jsonString);
    }

    private String makeInvalidAtBeginning(String jsonString) {
      return jsonString.substring(1);
    }

    private String makeInvalidAtEnding(String jsonString) {
      return jsonString.substring(0, jsonString.length() - 1);
    }

    private String makeInvalidInMiddle(String jsonString) {
      return "[" + jsonString + jsonString + "]";
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
  public boolean validationUsingJsonParser_InvalidBeginning(_State state) {
    return validateUsingJsonParser(state.jsonStringInvalidBeginning, _State.jsonFactory);
  }

  @Benchmark
  public boolean validationUsingJsonParser_InvalidEnding(_State state) {
    return validateUsingJsonParser(state.jsonStringInvalidEnding, _State.jsonFactory);
  }

  @Benchmark
  public boolean validationUsingJsonParser_InvalidMiddle(_State state) {
    return validateUsingJsonParser(state.jsonStringInvalidMiddle, _State.jsonFactory);
  }

  @Benchmark
  public boolean validationUsingJsonParser_Valid(_State state) {
    return validateUsingJsonParser(state.jsonString, _State.jsonFactory);
  }

  @Benchmark
  public boolean validationUsingObjectMapperReadTree_InvalidBeginning(_State state) {
    return validateUsingObjectMapperReadTree(state.jsonStringInvalidBeginning, _State.objectMapper);
  }

  @Benchmark
  public boolean validationUsingObjectMapperReadTree_InvalidEnding(_State state) {
    return validateUsingObjectMapperReadTree(state.jsonStringInvalidEnding, _State.objectMapper);
  }

  @Benchmark
  public boolean validationUsingObjectMapperReadTree_InvalidMiddle(_State state) {
    return validateUsingObjectMapperReadTree(state.jsonStringInvalidMiddle, _State.objectMapper);
  }

  @Benchmark
  public boolean validationUsingObjectMapperReadTree_Valid(_State state) {
    return validateUsingObjectMapperReadTree(state.jsonString, _State.objectMapper);
  }

  private boolean validateUsingJsonParser(String jsonString, JsonFactory jsonFactory) {
    try (JsonParser parser = jsonFactory.createParser(jsonString)) {
      while (parser.nextToken() != null) {}
      return true;
    } catch (IOException ex) {
      return false;
    }
  }

  private boolean validateUsingObjectMapperReadTree(String jsonString, ObjectMapper objectMapper) {
    try {
      objectMapper.readTree(jsonString);
      return true;
    } catch (IOException ex) {
      return false;
    }
  }
}
