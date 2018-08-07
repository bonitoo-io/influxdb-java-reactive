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

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

import io.bonitoo.influxdb.reactive.impl.AbstractInfluxDBReactiveTest;
import io.bonitoo.influxdb.reactive.options.BatchOptionsReactive;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.mockwebserver.MockResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * @author Jakub Bednar (bednar@github) (05/06/2018 07:04)
 */
@RunWith(JUnitPlatform.class)
class InfluxDBReactiveWriteBatchingTest extends AbstractInfluxDBReactiveTest {

    @Test
    void flushByActions() {

        BatchOptionsReactive batchOptions = BatchOptionsReactive.disabled()
                .batchSize(5)
                .flushInterval(1_000_000)
                .writeScheduler(Schedulers.trampoline())
                .build();

        setUp(batchOptions);

        influxDBServer.enqueue(new MockResponse());

        Flowable<H2OFeetMeasurement> measurements = Flowable.just(
                createMeasurement(1),
                createMeasurement(2),
                createMeasurement(3),
                createMeasurement(4));

        influxDBReactive.writeMeasurements(measurements);

        // only 4 data points
        Assertions.assertThat(influxDBServer.getRequestCount())
                .isEqualTo(0);

        // the fifth action => store to InfluxDB
        influxDBReactive.writeMeasurement(createMeasurement(5));

        // was call remote API
        Assertions.assertThat(influxDBServer.getRequestCount())
                .isEqualTo(1);

        // check content
        String expectedContent = Stream.of(
                "h2o_feet,location=coyote_creek level\\ description=\"feet 1\",water_level=1.0 1440046801000000",
                "h2o_feet,location=coyote_creek level\\ description=\"feet 2\",water_level=2.0 1440046802000000",
                "h2o_feet,location=coyote_creek level\\ description=\"feet 3\",water_level=3.0 1440046803000000",
                "h2o_feet,location=coyote_creek level\\ description=\"feet 4\",water_level=4.0 1440046804000000",
                "h2o_feet,location=coyote_creek level\\ description=\"feet 5\",water_level=5.0 1440046805000000")
                .collect(Collectors.joining("\n"));

        Assertions.assertThat(pointsBody()).isEqualTo(expectedContent);

        // there is no exception
        verifier.verifySuccess();
    }

    @Test
    void flushByDuration() {

        // after 10 batchSize or 1000 seconds
        BatchOptionsReactive batchOptions = BatchOptionsReactive.disabled()
                .batchSize(10)
                .flushInterval(1_000_000)
                .writeScheduler(Schedulers.trampoline())
                .build();

        setUp(batchOptions);

        influxDBServer.enqueue(new MockResponse());

        Flowable<H2OFeetMeasurement> measurements = Flowable.just(
                createMeasurement(1),
                createMeasurement(2),
                createMeasurement(3),
                createMeasurement(4));

        influxDBReactive.writeMeasurements(measurements);

        // the fifth measurement
        influxDBReactive.writeMeasurement(createMeasurement(5));

        // without call remote api
        Assertions.assertThat(influxDBServer.getRequestCount())
                .isEqualTo(0);

        // 1_000 seconds to feature
        advanceTimeBy(1_000, batchScheduler);

        // was call remote API
        Assertions.assertThat(influxDBServer.getRequestCount())
                .isEqualTo(1);

        // check content
        String expectedContent = Stream.of(
                "h2o_feet,location=coyote_creek level\\ description=\"feet 1\",water_level=1.0 1440046801000000",
                "h2o_feet,location=coyote_creek level\\ description=\"feet 2\",water_level=2.0 1440046802000000",
                "h2o_feet,location=coyote_creek level\\ description=\"feet 3\",water_level=3.0 1440046803000000",
                "h2o_feet,location=coyote_creek level\\ description=\"feet 4\",water_level=4.0 1440046804000000",
                "h2o_feet,location=coyote_creek level\\ description=\"feet 5\",water_level=5.0 1440046805000000")
                .collect(Collectors.joining("\n"));

        Assertions.assertThat(pointsBody()).isEqualTo(expectedContent);

        // there is no exception
        verifier.verifySuccess();
    }

    /**
     * @see Flowable#timeout(long, TimeUnit)
     */
    @Test
    void jitterInterval() {

        // after 5 batchSize or 10 seconds + 5 seconds jitter interval
        BatchOptionsReactive batchOptions = BatchOptionsReactive.disabled()
                .batchSize(5)
                .flushInterval(10_000)
                .jitterInterval(5_000)
                .writeScheduler(Schedulers.trampoline())
                .build();

        setUp(batchOptions);

        influxDBServer.enqueue(new MockResponse());

        // publish measurement
        influxDBReactive.writeMeasurement(createMeasurement(150));

        // move time to feature by 10 seconds - flush interval elapsed
        advanceTimeBy(10, batchScheduler);

        // without call remote api
        Assertions.assertThat(influxDBServer.getRequestCount())
                .isEqualTo(0);

        // move time to feature by 5 seconds - jitter interval elapsed
        advanceTimeBy(6, jitterScheduler);

        // was call remote API
        Assertions.assertThat(influxDBServer.getRequestCount())
                .isEqualTo(1);

        // check content

        String expected =
                "h2o_feet,location=coyote_creek level\\ description=\"feet 150\",water_level=150.0 1440046950000000";

        Assertions
                .assertThat(pointsBody())
                .isEqualTo(expected);

        // there is no exception
        verifier.verifySuccess();
    }

    @Test
    void flushBeforeClose() {

        // after 5 batchSize
        BatchOptionsReactive batchOptions = BatchOptionsReactive
                .disabled()
                .batchSize(5)
                .writeScheduler(Schedulers.trampoline())
                .build();

        setUp(batchOptions);

        influxDBServer.enqueue(new MockResponse());

        influxDBReactive.writeMeasurement(createMeasurement(1));

        // only 1 action
        Assertions.assertThat(influxDBServer.getRequestCount())
                .isEqualTo(0);

        // close InfluxDBReactive
        influxDBReactive.close();

        // was call remote API
        Assertions.assertThat(influxDBServer.getRequestCount())
                .isEqualTo(1);

        // check content

        String expected = "h2o_feet,location=coyote_creek "
                + "level\\ description=\"feet 1\",water_level=1.0 1440046801000000";

        Assertions
                .assertThat(pointsBody())
                .isEqualTo(expected);

        // there is no exception
        verifier.verifySuccess();
    }

    @Nonnull
    private H2OFeetMeasurement createMeasurement(@Nonnull final Integer index) {

        return H2OFeetMeasurement.createMeasurement(index);
    }
}
