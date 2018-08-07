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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.bonitoo.influxdb.reactive.events.UnhandledErrorEvent;
import io.bonitoo.influxdb.reactive.impl.AbstractInfluxDBReactiveTest;
import io.bonitoo.influxdb.reactive.options.BatchOptionsReactive;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.mockwebserver.MockResponse;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.influxdb.InfluxDBException;
import org.influxdb.InfluxDBMapperException;
import org.influxdb.dto.Point;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Jakub Bednar (bednar@github) (01/06/2018 10:56)
 */
@RunWith(JUnitPlatform.class)
class InfluxDBReactiveWriteTest extends AbstractInfluxDBReactiveTest {

    @BeforeEach
    void setUp() {
        BatchOptionsReactive build = BatchOptionsReactive
                .disabled()
                .writeScheduler(Schedulers.trampoline())
                .build();

        super.setUp(build);
    }

    @Test
    void writePoint() {

        influxDBServer.enqueue(new MockResponse());

        Point point = Point.measurement("h2o_feet")
                .tag("location", "coyote_creek")
                .addField("water_level", 2.927)
                .addField("level description", "below 3 feet")
                .time(1440046800, TimeUnit.NANOSECONDS)
                .build();

        // response
        Maybe<Point> pointMaybe = influxDBReactive.writePoint(point);
        pointMaybe.test()
                .assertSubscribed()
                .assertValue(point);

        // written point
        String expected = "h2o_feet,location=coyote_creek " +
                "level\\ description=\"below 3 feet\",water_level=2.927 1440046800";
        String actual = pointsBody();

        assertThat(actual).isEqualTo(expected);

        // One request
        Assertions.assertThat(influxDBServer.getRequestCount())
                .isEqualTo(1);

        // there is no exception
        verifier.verifySuccess();
    }

    @Test
    void writePointsIterable() {

        influxDBServer.enqueue(new MockResponse());
        influxDBServer.enqueue(new MockResponse());

        Point point1 = Point.measurement("h2o_feet")
                .tag("location", "coyote_creek")
                .addField("water_level", 2.927)
                .addField("level description", "below 3 feet")
                .time(1440046800, TimeUnit.NANOSECONDS)
                .build();

        Point point2 = Point.measurement("h2o_feet")
                .tag("location", "coyote_creek")
                .addField("water_level", 1.927)
                .addField("level description", "below 2 feet")
                .time(1440049800, TimeUnit.NANOSECONDS)
                .build();

        List<Point> points = new ArrayList<>();
        points.add(point1);
        points.add(point2);

        // response
        Flowable<Point> pointsFlowable = influxDBReactive.writePoints(points);
        pointsFlowable.test()
                .assertSubscribed()
                .assertValueAt(0, point1)
                .assertValueAt(1, point2);

        // written points
        String expected = "h2o_feet,location=coyote_creek " +
                "level\\ description=\"below 3 feet\",water_level=2.927 1440046800";
        String actual = pointsBody();
        assertThat(actual).isEqualTo(expected);

        expected = "h2o_feet,location=coyote_creek " +
                "level\\ description=\"below 2 feet\",water_level=1.927 1440049800";
        actual = pointsBody();
        assertThat(actual).isEqualTo(expected);

        // Two requests
        Assertions.assertThat(influxDBServer.getRequestCount())
                .isEqualTo(2);

        // there is no exception
        verifier.verifySuccess();
    }

    @Test
    void writePointsPublisher() {

        influxDBServer.enqueue(new MockResponse());
        influxDBServer.enqueue(new MockResponse());
        influxDBServer.enqueue(new MockResponse());

        Point point1 = Point.measurement("h2o_feet")
                .tag("location", "coyote_creek")
                .addField("water_level", 2.927)
                .addField("level description", "below 3 feet")
                .time(1440046800, TimeUnit.NANOSECONDS)
                .build();

        Point point2 = Point.measurement("h2o_feet")
                .tag("location", "coyote_creek")
                .addField("water_level", 1.927)
                .addField("level description", "below 2 feet")
                .time(1440049800, TimeUnit.NANOSECONDS)
                .build();

        Point point3 = Point.measurement("h2o_feet")
                .tag("location", "coyote_creek")
                .addField("water_level", 5.927)
                .addField("level description", "over 5 feet")
                .time(1440052800, TimeUnit.NANOSECONDS)
                .build();

        // response
        Flowable<Point> pointsFlowable = influxDBReactive.writePoints(Flowable.just(point1, point2, point3));
        pointsFlowable.test()
                .assertSubscribed()
                .assertValueAt(0, point1)
                .assertValueAt(1, point2)
                .assertValueAt(2, point3);

        // written points
        String expected = "h2o_feet,location=coyote_creek " +
                "level\\ description=\"below 3 feet\",water_level=2.927 1440046800";
        String actual = pointsBody();
        assertThat(actual).isEqualTo(expected);

        expected = "h2o_feet,location=coyote_creek " +
                "level\\ description=\"below 2 feet\",water_level=1.927 1440049800";
        actual = pointsBody();
        assertThat(actual).isEqualTo(expected);

        expected = "h2o_feet,location=coyote_creek " +
                "level\\ description=\"over 5 feet\",water_level=5.927 1440052800";
        actual = pointsBody();
        assertThat(actual).isEqualTo(expected);

        // Three requests
        Assertions.assertThat(influxDBServer.getRequestCount())
                .isEqualTo(3);

        // there is no exception
        verifier.verifySuccess();
    }

    @Test
    void writeMeasurement() {

        influxDBServer.enqueue(new MockResponse());

        H2OFeetMeasurement measurement = new H2OFeetMeasurement(
                "coyote_creek", 2.927, "below 3 feet", 1440046800L);

        // response
        Maybe<H2OFeetMeasurement> measurementMaybe = influxDBReactive.writeMeasurement(measurement);
        measurementMaybe.test()
                .assertSubscribed()
                .assertValue(measurement);

        // written measurement
        String expected = "h2o_feet,location=coyote_creek " +
                "level\\ description=\"below 3 feet\",water_level=2.927 1440046800000000";
        String actual = pointsBody();

        assertThat(actual).isEqualTo(expected);

        // One request
        Assertions.assertThat(influxDBServer.getRequestCount())
                .isEqualTo(1);

        // there is no exception
        verifier.verifySuccess();
    }

    @Test
    void writeMeasurementWhichIsNotMappableToPoint() {

        influxDBServer.enqueue(new MockResponse());

        Maybe<Integer> writeMeasurement = influxDBReactive
                .writeMeasurement(15);

        // just emmit source
        writeMeasurement
                .test()
                .assertValue(15);

        // without any request
        Assertions.assertThat(influxDBServer.getRequestCount()).isEqualTo(0);
    }

    @Test
    void writeMeasurementsIterable() {

        influxDBServer.enqueue(new MockResponse());
        influxDBServer.enqueue(new MockResponse());

        H2OFeetMeasurement measurement1 = new H2OFeetMeasurement(
                "coyote_creek", 2.927, "below 3 feet", 1440046800L);

        H2OFeetMeasurement measurement2 = new H2OFeetMeasurement(
                "coyote_creek", 1.927, "below 2 feet", 1440049800L);

        List<H2OFeetMeasurement> measurements = new ArrayList<>();
        measurements.add(measurement1);
        measurements.add(measurement2);

        // response
        Flowable<H2OFeetMeasurement> measurementsFlowable = influxDBReactive.writeMeasurements(measurements);
        measurementsFlowable.test()
                .assertSubscribed()
                .assertValueAt(0, measurement1)
                .assertValueAt(1, measurement2);

        // written measurements
        String expected = "h2o_feet,location=coyote_creek " +
                "level\\ description=\"below 3 feet\",water_level=2.927 1440046800000000";
        String actual = pointsBody();
        assertThat(actual).isEqualTo(expected);

        expected = "h2o_feet,location=coyote_creek " +
                "level\\ description=\"below 2 feet\",water_level=1.927 1440049800000000";
        actual = pointsBody();
        assertThat(actual).isEqualTo(expected);

        // Two requests
        Assertions.assertThat(influxDBServer.getRequestCount())
                .isEqualTo(2);

        // there is no exception
        verifier.verifySuccess();
    }

    @Test
    void writeMeasurementsIterableWhichIsNotMappableToPoint() {

        influxDBServer.enqueue(new MockResponse());
        influxDBServer.enqueue(new MockResponse());

        TestObserver<UnhandledErrorEvent> listener = influxDBReactive.listenEvents(UnhandledErrorEvent.class).test();

        List<Object> measurements = Lists.newArrayList(
                H2OFeetMeasurement.createMeasurement(1),
                2,
                H2OFeetMeasurement.createMeasurement(3));

        Flowable<Object> writeMeasurements = influxDBReactive
                .writeMeasurements(measurements);

        // just emmit source
        writeMeasurements
                .test()
                .assertValueCount(3)
                .assertValueAt(0, measurements.get(0))
                .assertValueAt(1, measurements.get(1))
                .assertValueAt(2, measurements.get(2));

        // two request
        Assertions.assertThat(influxDBServer.getRequestCount()).isEqualTo(2);

        String body1 = "h2o_feet,location=coyote_creek level\\ description=\"feet 1\",water_level=1.0 1440046801000000";
        Assertions.assertThat(body1).isEqualTo(pointsBody());

        String body2 = "h2o_feet,location=coyote_creek level\\ description=\"feet 3\",water_level=3.0 1440046803000000";
        Assertions.assertThat(body2).isEqualTo(pointsBody());

        listener
                .assertValueCount(1)
                .assertValue(event -> {
                    
                    Assertions
                            .assertThat(event.getThrowable())
                            .isInstanceOf(InfluxDBException.class)
                            .hasMessage("Can not calculate InfluxDB Line Protocol for '2'")
                            .hasCauseInstanceOf(InfluxDBMapperException.class);

                    return true;
                });
    }

    @Test
    void writeMeasurementsPublisher() {

        influxDBServer.enqueue(new MockResponse());
        influxDBServer.enqueue(new MockResponse());
        influxDBServer.enqueue(new MockResponse());

        H2OFeetMeasurement measurement1 = new H2OFeetMeasurement(
                "coyote_creek", 2.927, "below 3 feet", 1440046800L);

        H2OFeetMeasurement measurement2 = new H2OFeetMeasurement(
                "coyote_creek", 1.927, "below 2 feet", 1440049800L);

        H2OFeetMeasurement measurement3 = new H2OFeetMeasurement(
                "coyote_creek", 5.927, "over 5 feet", 1440052800L);

        // response
        Flowable<H2OFeetMeasurement> measurementsFlowable = influxDBReactive
                .writeMeasurements(Flowable.just(measurement1, measurement2, measurement3));

        measurementsFlowable
                .test()
                .assertSubscribed()
                .assertValueAt(0, measurement1)
                .assertValueAt(1, measurement2)
                .assertValueAt(2, measurement3);

        // written measurements
        String expected = "h2o_feet,location=coyote_creek " +
                "level\\ description=\"below 3 feet\",water_level=2.927 1440046800000000";
        String actual = pointsBody();
        assertThat(actual).isEqualTo(expected);

        expected = "h2o_feet,location=coyote_creek " +
                "level\\ description=\"below 2 feet\",water_level=1.927 1440049800000000";
        actual = pointsBody();
        assertThat(actual).isEqualTo(expected);

        expected = "h2o_feet,location=coyote_creek " +
                "level\\ description=\"over 5 feet\",water_level=5.927 1440052800000000";
        actual = pointsBody();
        assertThat(actual).isEqualTo(expected);

        // Three requests
        Assertions.assertThat(influxDBServer.getRequestCount())
                .isEqualTo(3);

        // there is no exception
        verifier.verifySuccess();
    }

    @Test
    void writeRecord() {

        influxDBServer.enqueue(new MockResponse());

        String record =
                "h2o_feet,location=coyote_creek level\\ description=\"below 3 feet\",water_level=2.927 1440046800";

        // response
        Maybe<String> recordMaybe = influxDBReactive.writeRecord(record);
        recordMaybe.test()
                .assertSubscribed()
                .assertValue(record);

        String actual = pointsBody();

        assertThat(actual).isEqualTo(record);

        // One request
        Assertions.assertThat(influxDBServer.getRequestCount())
                .isEqualTo(1);

        // there is no exception
        verifier.verifySuccess();
    }

    @Test
    void writeRecordsIterable() {

        influxDBServer.enqueue(new MockResponse());
        influxDBServer.enqueue(new MockResponse());

        String record1 = "h2o_feet,location=coyote_creek " +
                "level\\ description=\"below 3 feet\",water_level=2.927 1440046800";

        String record2 = "h2o_feet,location=coyote_creek " +
                "level\\ description=\"below 2 feet\",water_level=1.927 1440049800";

        List<String> records = new ArrayList<>();
        records.add(record1);
        records.add(record2);

        // response
        Flowable<String> recordsFlowable = influxDBReactive.writeRecords(records);
        recordsFlowable.test()
                .assertSubscribed()
                .assertValueAt(0, record1)
                .assertValueAt(1, record2);

        // written measurements
        String actual = pointsBody();
        assertThat(actual).isEqualTo(record1);

        actual = pointsBody();
        assertThat(actual).isEqualTo(record2);

        // Two requests
        Assertions.assertThat(influxDBServer.getRequestCount())
                .isEqualTo(2);

        // there is no exception
        verifier.verifySuccess();
    }

    @Test
    void writeRecordsPublisher() {

        influxDBServer.enqueue(new MockResponse());
        influxDBServer.enqueue(new MockResponse());
        influxDBServer.enqueue(new MockResponse());

        String record1 = "h2o_feet,location=coyote_creek " +
                "level\\ description=\"below 3 feet\",water_level=2.927 1440046800";

        String record2 = "h2o_feet,location=coyote_creek " +
                "level\\ description=\"below 2 feet\",water_level=1.927 1440049800";

        String record3 = "h2o_feet,location=coyote_creek " +
                "level\\ description=\"over 5 feet\",water_level=5.927 1440052800";

        // response
        Flowable<String> recordsFlowable = influxDBReactive
                .writeRecords(Flowable.just(record1, record2, record3));

        recordsFlowable
                .test()
                .assertSubscribed()
                .assertValueAt(0, record1)
                .assertValueAt(1, record2)
                .assertValueAt(2, record3);

        // written measurements
        String actual = pointsBody();
        assertThat(actual).isEqualTo(record1);

        actual = pointsBody();
        assertThat(actual).isEqualTo(record2);

        actual = pointsBody();
        assertThat(actual).isEqualTo(record3);

        // Three requests
        Assertions.assertThat(influxDBServer.getRequestCount())
                .isEqualTo(3);

        // there is no exception
        verifier.verifySuccess();
    }
}