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
package io.bonitoo.influxdb.reactive;

import io.bonitoo.influxdb.reactive.impl.AbstractInfluxDBReactiveTest;
import io.bonitoo.influxdb.reactive.options.BatchOptionsReactive;

import okhttp3.mockwebserver.MockResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * @author Jakub Bednar (bednar@github) (14/06/2018 12:04)
 */
@RunWith(JUnitPlatform.class)
class InfluxDBReactiveTest extends AbstractInfluxDBReactiveTest {

    @BeforeEach
    void setUp() {
        super.setUp(BatchOptionsReactive.DEFAULTS);
    }

    @Test
    void close() {

        Assertions.assertThat(influxDBReactive.isClosed()).isEqualTo(false);

        influxDBReactive.close();

        Assertions.assertThat(influxDBReactive.isClosed()).isEqualTo(true);
    }

    @Test
    void gzip() {

        // default disabled
        Assertions.assertThat(influxDBReactive.isGzipEnabled()).isEqualTo(false);

        // enable
        influxDBReactive.enableGzip();
        Assertions.assertThat(influxDBReactive.isGzipEnabled()).isEqualTo(true);

        // disable
        influxDBReactive.disableGzip();
        Assertions.assertThat(influxDBReactive.isGzipEnabled()).isEqualTo(false);
    }

    @Test
    void ping() {

        influxDBServer.enqueue(new MockResponse().setHeader("X-Influxdb-Version", "v1.5.2"));

        influxDBReactive
                .ping()
                .test()
                .assertValueCount(1)
                .assertValue(pong -> {

                    Assertions.assertThat(pong.getVersion()).isEqualTo("v1.5.2");
                    Assertions.assertThat(pong.isGood()).isTrue();
                    Assertions.assertThat(pong.getResponseTime()).isGreaterThan(0);

                    return true;
                });
    }

    @Test
    void version() {

        influxDBServer.enqueue(new MockResponse().setHeader("X-Influxdb-Version", "v1.5.2"));

        influxDBReactive
                .version()
                .test()
                .assertValueCount(1)
                .assertValue("v1.5.2");
    }
}
