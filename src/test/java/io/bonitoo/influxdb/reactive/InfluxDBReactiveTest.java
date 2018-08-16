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
import org.influxdb.InfluxDB;
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
        super.setUp(BatchOptionsReactive.DISABLED);
    }

    @Test
    void close() {

        Assertions.assertThat(influxDBReactive.isClosed()).isEqualTo(false);

        InfluxDBReactive influxDBReactive = this.influxDBReactive.close();
        Assertions.assertThat(influxDBReactive).isEqualTo(this.influxDBReactive);

        Assertions.assertThat(this.influxDBReactive.isClosed()).isEqualTo(true);
    }

    @Test
    void gzip() {

        // default disabled
        Assertions.assertThat(influxDBReactive.isGzipEnabled()).isEqualTo(false);

        // enable
        InfluxDBReactive influxDBReactive = this.influxDBReactive.enableGzip();
        Assertions.assertThat(this.influxDBReactive.isGzipEnabled()).isEqualTo(true);
        Assertions.assertThat(influxDBReactive).isEqualTo(this.influxDBReactive);

        // disable
        influxDBReactive = this.influxDBReactive.disableGzip();
        Assertions.assertThat(this.influxDBReactive.isGzipEnabled()).isEqualTo(false);
        Assertions.assertThat(influxDBReactive).isEqualTo(this.influxDBReactive);
    }

    @Test
    void gzipHeader() throws InterruptedException {

        String record = "h2o_feet,location=coyote_creek " +
                "level\\ description=\"below 3 feet\",water_level=2.927 1440046800";

        influxDBServer.enqueue(new MockResponse());
        influxDBServer.enqueue(new MockResponse());

        // Disabled GZIP
        influxDBReactive.disableGzip();
        influxDBReactive.writeRecord(record);
        // GZIP header IS NOT set
        Assertions.assertThat(influxDBServer.takeRequest().getHeader("Content-Encoding")).isNull();

        // Enabled GZIP
        influxDBReactive.enableGzip();
        influxDBReactive.writeRecord(record);
        // GZIP header IS set
        Assertions.assertThat(influxDBServer.takeRequest().getHeader("Content-Encoding")).isEqualTo("gzip");
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

    @Test
    void logLevel() {

        InfluxDBReactive influxDBReactive = this.influxDBReactive.setLogLevel(InfluxDB.LogLevel.BASIC);

        Assertions.assertThat(influxDBReactive).isEqualTo(this.influxDBReactive);
    }

    @Test
    void logLevelIsRequired() {

        //noinspection ConstantConditions
        Assertions.assertThatThrownBy(() -> influxDBReactive.setLogLevel(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("InfluxDB.LogLevel is required");
    }
}
