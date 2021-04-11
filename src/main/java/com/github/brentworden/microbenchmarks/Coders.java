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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import org.apache.avro.reflect.Nullable;
import org.apache.beam.repackaged.core.org.apache.commons.lang3.RandomStringUtils;
import org.apache.beam.sdk.coders.AtomicCoder;
import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.BooleanCoder;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.coders.CoderException;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.coders.VarIntCoder;
import org.openjdk.jmh.annotations.AuxCounters;
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
 * Collection of benchmarks that measure the throughput of encoding and decoding using various
 * Apache Beam {@link Coder} implementations.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(10)
public class Coders {

  /**
   * State used by the benchmarks to hold onto {@link Coder} instances so their construction is not
   * considered part of the test.
   */
  @State(Scope.Benchmark)
  public static class StateCoders {
    Coder<TestObject> avroCoder;

    Coder<TestObject> customCoder;

    Coder<TestObject> serializableCoder;

    @Setup(Level.Trial)
    public void onTrialSetup() {
      avroCoder = AvroCoder.of(TestObject.class);
      customCoder = TestObjectCoder.of();
      serializableCoder = SerializableCoder.of(TestObject.class);
    }
  }

  /**
   * State used by the benchmarks to hold onto {@link TestObject} instances so their construction is
   * not considered part of the test.
   */
  @State(Scope.Thread)
  @AuxCounters(AuxCounters.Type.EVENTS)
  public static class StateTestObjects {
    public int bytes;

    TestObject originalTestObject;

    @Setup(Level.Iteration)
    public void onIterationSetup() {
      originalTestObject = new TestObject();
      originalTestObject.setIntValue((int) (Math.random() * 100000.0));
      originalTestObject.setNonNullableStringValue(
          RandomStringUtils.randomAlphabetic((int) (Math.random() * 100.0) + 1));
      if (Math.random() > 0.125) {
        originalTestObject.setNullableStringValue(
            RandomStringUtils.randomAlphabetic((int) (Math.random() * 100.0) + 1));
      } else {
        originalTestObject.setNullableStringValue(null);
      }
    }
  }

  /**
   * The test object type that will serve as the object being encoded and decoded with the various
   * {@link Coder} implementations.
   */
  public static class TestObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private int intValue;

    private String nonNullableStringValue = "";

    @Nullable private String nullableStringValue;

    public int getIntValue() {
      return intValue;
    }

    public String getNonNullableStringValue() {
      return nonNullableStringValue;
    }

    @Nullable
    public String getNullableStringValue() {
      return nullableStringValue;
    }

    public void setIntValue(int value) {
      this.intValue = value;
    }

    public void setNonNullableStringValue(String value) {
      this.nonNullableStringValue = value;
    }

    @Nullable
    public void setNullableStringValue(String value) {
      this.nullableStringValue = value;
    }
  }

  /** The custom {@link Coder} used by the benchmarks. */
  public static class TestObjectCoder extends AtomicCoder<TestObject> {

    private static final BooleanCoder BOOLEAN_CODER = BooleanCoder.of();

    private static final TestObjectCoder INSTANCE = new TestObjectCoder();

    private static final VarIntCoder INT_CODER = VarIntCoder.of();

    private static final long serialVersionUID = 1L;

    private static final StringUtf8Coder STRING_CODER = StringUtf8Coder.of();

    public static TestObjectCoder of() {
      return INSTANCE;
    }

    @Override
    public TestObject decode(InputStream inStream) throws CoderException, IOException {
      return null;
    }

    @Override
    public void encode(TestObject value, OutputStream outStream)
        throws CoderException, IOException {
      INT_CODER.encode(Integer.valueOf(value.intValue), outStream);
      STRING_CODER.encode(value.nonNullableStringValue, outStream);
      if (value.nullableStringValue == null) {
        BOOLEAN_CODER.encode(Boolean.FALSE, outStream);
      } else {
        BOOLEAN_CODER.encode(Boolean.TRUE, outStream);
        STRING_CODER.encode(value.nullableStringValue, outStream);
      }
    }
  }

  /** Benchmark that measures the throughput of encoding and decoding using an {@link AvroCoder}. */
  @Benchmark
  public TestObject avroCoder(StateCoders coders, StateTestObjects testObject) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);
    coders.avroCoder.encode(testObject.originalTestObject, bos);

    byte[] buffer = bos.toByteArray();

    ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
    TestObject newTestObject = coders.avroCoder.decode(bis);

    testObject.bytes = buffer.length;

    return newTestObject;
  }

  /**
   * Benchmark that measures the throughput of encoding and decoding using a custom {@link Coder}.
   */
  @Benchmark
  public TestObject customCoder(StateCoders coders, StateTestObjects testObject)
      throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);
    coders.customCoder.encode(testObject.originalTestObject, bos);

    byte[] buffer = bos.toByteArray();

    ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
    TestObject newTestObject = coders.customCoder.decode(bis);

    testObject.bytes = buffer.length;

    return newTestObject;
  }

  /**
   * Benchmark that measures the throughput of encoding and decoding using a {@link
   * SerializableCoder}.
   */
  @Benchmark
  public TestObject serializableCoder(StateCoders coders, StateTestObjects testObject)
      throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);
    coders.serializableCoder.encode(testObject.originalTestObject, bos);

    byte[] buffer = bos.toByteArray();

    ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
    TestObject newTestObject = coders.serializableCoder.decode(bis);

    testObject.bytes = buffer.length;

    return newTestObject;
  }
}
