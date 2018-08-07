/*
 * The MIT License
 * Copyright © 2018
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.bonitoo.influxdb.reactive.impl;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.bonitoo.influxdb.reactive.InfluxDBReactive;
import io.bonitoo.influxdb.reactive.options.BatchOptionsReactive;
import io.bonitoo.influxdb.reactive.options.InfluxDBOptions;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.schedulers.TestScheduler;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;

/**
 * @author Jakub Bednar (bednar@github) (05/06/2018 07:04)
 */
public abstract class AbstractInfluxDBReactiveTest {

    protected InfluxDBReactive influxDBReactive;
    protected InfluxDBReactiveVerifier verifier;

    protected Scheduler batchScheduler;
    protected Scheduler jitterScheduler;
    protected Scheduler retryScheduler;

    protected MockWebServer influxDBServer;

    protected void setUp(@Nonnull final BatchOptionsReactive batchOptions) {
        setUp(batchOptions, new TestScheduler(), new TestScheduler(), new TestScheduler());
    }

    protected void setUp(@Nonnull final BatchOptionsReactive batchOptions,
                         @Nonnull final Scheduler batchScheduler,
                         @Nonnull final Scheduler jitterScheduler,
                         @Nonnull final Scheduler retryScheduler) {

        Objects.requireNonNull(batchOptions, "BatchOptionsReactive is required");

        influxDBServer = new MockWebServer();
        try {
            influxDBServer.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        InfluxDBOptions options = InfluxDBOptions.builder()
                .url(influxDBServer.url("/").url().toString())
                .username("admin")
                .password("password")
                .database("weather")
                .build();

        this.batchScheduler = batchScheduler;
        this.jitterScheduler = jitterScheduler;
        this.retryScheduler = retryScheduler;

        influxDBReactive = new InfluxDBReactiveImpl(options, batchOptions,
                Schedulers.trampoline(), this.batchScheduler, this.jitterScheduler,
                this.retryScheduler);

        verifier = new InfluxDBReactiveVerifier(influxDBReactive);

    }

    protected void advanceTimeBy(int i, @Nonnull final Scheduler scheduler) {
        ((TestScheduler) scheduler).advanceTimeBy(i, TimeUnit.SECONDS);
    }

    @AfterEach
    void cleanUp() throws IOException {
        influxDBReactive.close();
        influxDBServer.shutdown();
    }

    @Nonnull
    protected String pointsBody() {

        Buffer sink;
        try {
            sink = influxDBServer.takeRequest().getBody();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return sink.readUtf8();
    }

    @Nonnull
    protected MockResponse createErrorResponse(@Nullable final String influxDBError) {

        String body = String.format("{\"error\":\"%s\"}", influxDBError);

        return new MockResponse()
                .setResponseCode(400)
                .addHeader("X-Influxdb-Error", influxDBError)
                .setBody(body);
    }
}
