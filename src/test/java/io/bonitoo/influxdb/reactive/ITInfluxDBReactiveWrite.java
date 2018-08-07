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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.bonitoo.influxdb.reactive.events.BackpressureEvent;
import io.bonitoo.influxdb.reactive.events.UnhandledErrorEvent;
import io.bonitoo.influxdb.reactive.events.WritePartialEvent;
import io.bonitoo.influxdb.reactive.events.WriteSuccessEvent;
import io.bonitoo.influxdb.reactive.impl.AbstractITInfluxDBReactiveTest;
import io.bonitoo.influxdb.reactive.options.BatchOptionsReactive;
import io.bonitoo.influxdb.reactive.options.WriteOptions;

import io.reactivex.BackpressureOverflowStrategy;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.influxdb.InfluxDBException;
import org.influxdb.InfluxDBMapperException;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jakub Bednar (bednar@github) (04/06/2018 10:06)
 */
@RunWith(JUnitPlatform.class)
class ITInfluxDBReactiveWrite extends AbstractITInfluxDBReactiveTest {

    @Test
    void write() {

        setUp(BatchOptionsReactive.builder().batchSize(1).build());

        H2OFeetMeasurement measurement1 = new H2OFeetMeasurement(
                "coyote_creek", 2.927, "below 3 feet", 1440046801L);

        H2OFeetMeasurement measurement2 = new H2OFeetMeasurement(
                "coyote_creek", 1.927, "below 2 feet", 1440049802L);

        // write
        influxDBReactive.writeMeasurements(Flowable.just(measurement1, measurement2));

        verifier.waitForResponse(2);

        // get from DB
        List<H2OFeetMeasurement> measurements = getMeasurements();
        assertThat(measurements.size()).isEqualTo(2);

        // measurement 1
        assertThat(measurements.get(0).getLocation()).isEqualTo("coyote_creek");
        assertThat(measurements.get(0).getLevel()).isEqualTo(2.927);
        assertThat(measurements.get(0).getDescription()).isEqualTo("below 3 feet");
        assertThat(measurements.get(0).getTime().toEpochMilli()).isEqualTo(1440046801L);

        // measurement 2
        assertThat(measurements.get(1).getLocation()).isEqualTo("coyote_creek");
        assertThat(measurements.get(1).getLevel()).isEqualTo(1.927);
        assertThat(measurements.get(1).getDescription()).isEqualTo("below 2 feet");
        assertThat(measurements.get(1).getTime().toEpochMilli()).isEqualTo(1440049802L);

        verifier.verifySuccess();
    }

    @Test
    void writeFail() {

        setUp(BatchOptionsReactive.builder().batchSize(1).build());

        Map<String, Object> fieldsToAdd = new HashMap<>();
        fieldsToAdd.put("level", null);

        Point point = Point.measurement("coyote_creek").fields(fieldsToAdd).build();

        // write
        influxDBReactive.writePoint(point);

        verifier.waitForResponse(1);

        verifier.verifyErrorResponse(1);
    }

    @Test
    void publishPattern() {

        setUp(BatchOptionsReactive.builder().batchSize(1).build());

        TestScheduler scheduler = new TestScheduler();

        // every 10 seconds publish new h2o level
        Flowable<H2OFeetMeasurement> observeWeather = Flowable.interval(10, TimeUnit.SECONDS, scheduler)
                .map(time -> {

                    double h2oLevel = time.doubleValue();
                    long timestamp = System.currentTimeMillis() + time;

                    return new H2OFeetMeasurement(
                            "coyote_creek", h2oLevel, "from ocean sensor", timestamp);
                });

        // write
        influxDBReactive.writeMeasurements(observeWeather);

        // 50 seconds to feature
        scheduler.advanceTimeBy(50, TimeUnit.SECONDS);

        verifier.waitForResponse(5);

        // get from DB
        List<H2OFeetMeasurement> measurements = getMeasurements();
        assertThat(measurements.size()).isEqualTo(5);

        assertThat(measurements.get(0).getLevel()).isEqualTo(0D);
        assertThat(measurements.get(1).getLevel()).isEqualTo(1D);
        assertThat(measurements.get(2).getLevel()).isEqualTo(2D);
        assertThat(measurements.get(3).getLevel()).isEqualTo(3D);
        assertThat(measurements.get(4).getLevel()).isEqualTo(4D);

        verifier.verifySuccess();
    }

    @Test
    void batchingOrderForJitter() {

        // after 5 data points or 10 seconds + 5 seconds jitter interval
        BatchOptionsReactive batchOptions = BatchOptionsReactive.disabled()
                .batchSize(2)
                .flushInterval(10_000)
                .jitterInterval(5_000)
                .build();

        setUp(batchOptions);

        H2OFeetMeasurement measurement1 = new H2OFeetMeasurement(
                "coyote_creek1", 0.927, "below 1 feet", null);

        H2OFeetMeasurement measurement2 = new H2OFeetMeasurement(
                "coyote_creek2", 1.927, "below 2 feet", null);

        H2OFeetMeasurement measurement3 = new H2OFeetMeasurement(
                "coyote_creek3", 2.927, "below 3 feet", null);

        H2OFeetMeasurement measurement4 = new H2OFeetMeasurement(
                "coyote_creek4", 3.927, "below 4 feet", null);

        List<H2OFeetMeasurement> measurements1 = new ArrayList<>();
        measurements1.add(measurement1);
        measurements1.add(measurement2);

        List<H2OFeetMeasurement> measurements2 = new ArrayList<>();
        measurements2.add(measurement3);
        measurements2.add(measurement4);

        // publish measurement
        influxDBReactive.writeMeasurements(measurements1);
        influxDBReactive.writeMeasurements(measurements2);

        verifier.waitForResponse(2);

        // get from DB
        List<H2OFeetMeasurement> measurements = getMeasurements();
        assertThat(measurements.size()).isEqualTo(4);

        // measurement 1
        H2OFeetMeasurement measurement1DB = measurements.get(0);
        assertThat(measurement1DB.getLocation()).isEqualTo("coyote_creek1");
        assertThat(measurement1DB.getLevel()).isEqualTo(0.927);
        assertThat(measurement1DB.getDescription()).isEqualTo("below 1 feet");
        assertThat(measurement1DB.getTime().toEpochMilli()).isNotNull();

        // measurement 2
        H2OFeetMeasurement measurement2DB = measurements.get(1);
        assertThat(measurement2DB.getLocation()).isEqualTo("coyote_creek2");
        assertThat(measurement2DB.getLevel()).isEqualTo(1.927);
        assertThat(measurement2DB.getDescription()).isEqualTo("below 2 feet");
        assertThat(measurement2DB.getTime().toEpochMilli()).isNotNull();

        // measurement 3
        H2OFeetMeasurement measurement3DB = measurements.get(2);
        assertThat(measurement3DB.getLocation()).isEqualTo("coyote_creek3");
        assertThat(measurement3DB.getLevel()).isEqualTo(2.927);
        assertThat(measurement3DB.getDescription()).isEqualTo("below 3 feet");
        assertThat(measurement3DB.getTime().toEpochMilli()).isNotNull();

        // measurement 4
        H2OFeetMeasurement measurement4DB = measurements.get(3);
        assertThat(measurement4DB.getLocation()).isEqualTo("coyote_creek4");
        assertThat(measurement4DB.getLevel()).isEqualTo(3.927);
        assertThat(measurement4DB.getDescription()).isEqualTo("below 4 feet");
        assertThat(measurement4DB.getTime().toEpochMilli()).isNotNull();

        // same order as writes => timestamp in order
        Assertions.assertThat(measurement1DB.getTime()).isBeforeOrEqualTo(measurement2DB.getTime());
        Assertions.assertThat(measurement2DB.getTime()).isBefore(measurement3DB.getTime());
        Assertions.assertThat(measurement3DB.getTime()).isBeforeOrEqualTo(measurement4DB.getTime());

        verifier.verifySuccess();
    }

    @Test
    void backpressure() {

        setUp(BatchOptionsReactive.builder().build());

        Flowable<Point> map = Flowable
                .range(0, 20_000).map(index ->
                        Point.measurement("h2o_feet")
                                .tag("location", "coyote_creek" + index)
                                .addField("water_level", index)
                                .addField("level description", index + " feet")
                                .time(index, TimeUnit.NANOSECONDS)
                                .build());

        influxDBReactive.writePoints(map);
        influxDBReactive.close();

        // was backpressure
        Long backpressureCount = verifier
                .verifyBackpressure();

        // wait for response
        verifier.waitForClose();

        // measurements + backpressure = 20 000
        List<H2OFeetMeasurement> measurements = getMeasurements();
        Assertions.assertThat(backpressureCount + measurements.size()).isEqualTo(20_000);

        verifier.verifySuccess();
    }

    @Test
    void backpressureEvent() {

        setUp(BatchOptionsReactive.builder().build());

        TestObserver<BackpressureEvent> listener = influxDBReactive
                .listenEvents(BackpressureEvent.class)
                .test();

        Flowable<Point> map = Flowable
                .range(0, 20_000).map(index ->
                        Point.measurement("h2o_feet")
                                .tag("location", "coyote_creek" + index)
                                .addField("water_level", index)
                                .addField("level description", index + " feet")
                                .time(index, TimeUnit.NANOSECONDS)
                                .build());

        influxDBReactive.writePoints(map);
        influxDBReactive.close();

        // wait for response
        verifier.waitForClose();

        // was call backpressure event
        Assertions.assertThat(listener.valueCount()).isGreaterThan(0);
        verifier.verifySuccess();
    }

    @Test
    void backpressureErrorStrategy() {

        BatchOptionsReactive build = BatchOptionsReactive
                .builder()
                .backpressureStrategy(BackpressureOverflowStrategy.ERROR)
                .build();

        setUp(build);

        Flowable<Point> map = Flowable
                .range(0, 20_000).map(index ->
                        Point.measurement("h2o_feet")
                                .tag("location", "coyote_creek" + index)
                                .addField("water_level", index)
                                .addField("level description", index + " feet")
                                .time(index, TimeUnit.NANOSECONDS)
                                .build());

        influxDBReactive.writePoints(map);
        influxDBReactive.close();

        // wait for response
        verifier.waitForClose();

        Assertions.assertThat(getMeasurements().size()).isLessThan(20_000);
        verifier.verifyNoBackpressure();
    }

    @Test
    void withoutBackpressure() {

        setUp(BatchOptionsReactive.builder().bufferLimit(20_000).build());

        Flowable<Point> map = Flowable
                .range(0, 20_000).map(index ->
                        Point.measurement("h2o_feet")
                                .tag("location", "coyote_creek" + index)
                                .addField("water_level", index)
                                .addField("level description", index + " feet")
                                .time(index, TimeUnit.NANOSECONDS)
                                .build());

        influxDBReactive.writePoints(map);
        influxDBReactive.close();

        // wait for response
        verifier.waitForClose();

        // measurements + backpressure = 20 000
        List<H2OFeetMeasurement> measurements = getMeasurements();
        Assertions.assertThat(measurements.size()).isEqualTo(20_000);

        verifier.verifySuccess();
    }

    @Test
    void partialWrite() {

        setUp(BatchOptionsReactive.builder().batchSize(2).build());

        String record1 = "h2o_feet,location=coyote_creek " +
                "level\\ description=\"below 3 feet\",water_level=2.927 1440046800";

        String record2 = "h2o_feet,location=coyote_creek 1440049800";

        TestObserver<WritePartialEvent> listener = influxDBReactive
                .listenEvents(WritePartialEvent.class)
                .test();

        influxDBReactive.writeRecords(Flowable.just(record1, record2));

        // wait for response
        verifier.waitForResponse(1);
        verifier.verifyErrorResponse(1);

        influxDBReactive.close();
        verifier.waitForClose();

        listener
                .assertValueCount(1)
                .assertValue(event -> {

                    Assertions.assertThat(event.getDataPoints()).containsExactlyInAnyOrder(record1, record2);

                    Assertions.assertThat(event.getException())
                            .hasMessageStartingWith("partial write:")
                            .hasMessageContaining("'h2o_feet,location=coyote_creek 1440049800': invalid field format");

                    return true;
                });

        Assertions.assertThat(getMeasurements()).hasSize(1);
    }

    @Test
    void writeToDifferentDatabases() {

        setUp(BatchOptionsReactive.builder().batchSize(4).build());

        TestObserver<WriteSuccessEvent> listener = influxDBReactive
                .listenEvents(WriteSuccessEvent.class)
                .test();

        simpleQuery("CREATE database " + DATABASE_NAME + "_1");
        simpleQuery("CREATE database " + DATABASE_NAME + "_2");

        Point point1_1 = Point.measurement("test_measurement1")
                .tag("tag", "1")
                .addField("field", 1)
                .build();
        Point point1_2 = Point.measurement("test_measurement1")
                .tag("tag", "2")
                .addField("field", 2)
                .build();
        List<Point> points1 = new ArrayList<>();
        points1.add(point1_1);
        points1.add(point1_2);

        Point point2_1 = Point.measurement("test_measurement2")
                .tag("tag", "1")
                .addField("field", 1)
                .build();
        Point point2_2 = Point.measurement("test_measurement2")
                .tag("tag", "2")
                .addField("field", 2)
                .build();
        List<Point> points2 = new ArrayList<>();
        points2.add(point2_1);
        points2.add(point2_2);

        influxDBReactive.writePoints(points1, WriteOptions.builder().database(DATABASE_NAME + "_1").build());
        influxDBReactive.writePoints(points2, WriteOptions.builder().database(DATABASE_NAME + "_2").build());

        verifier.waitForResponse(2);
        verifier.verifySuccessResponse(2);
        verifier.verifySuccess();

        //
        // Assert by listener
        //
        List<String> databases = new ArrayList<>();
        listener.assertValueCount(2)
                .assertValueAt(0, event -> {
                    Assertions.assertThat(event.getDataPoints().size()).isEqualTo(2);
                    databases.add(event.getWriteOptions().getDatabase());
                    return true;
                })
                .assertValueAt(1, event -> {
                    Assertions.assertThat(event.getDataPoints().size()).isEqualTo(2);
                    databases.add(event.getWriteOptions().getDatabase());
                    return true;
                });

        Assertions.assertThat(databases).contains(DATABASE_NAME + "_1", DATABASE_NAME + "_2");

        //
        // Assert by query
        //
        influxDBReactive
                .query(new Query("select * from test_measurement1", DATABASE_NAME + "_1"))
                .test()
                .assertValueCount(1).assertValue(result -> {

            List<List<Object>> values = result.getResults().get(0).getSeries().get(0).getValues();
            Assertions.assertThat(values.size()).isEqualTo(2);

            return true;
        });

        influxDBReactive
                .query(new Query("select * from test_measurement2", DATABASE_NAME + "_2"))
                .test()
                .assertValueCount(1).assertValue(result -> {

            List<List<Object>> values = result.getResults().get(0).getSeries().get(0).getValues();
            Assertions.assertThat(values.size()).isEqualTo(2);

            return true;
        });

        simpleQuery("DROP database " + DATABASE_NAME + "_1");
        simpleQuery("DROP database " + DATABASE_NAME + "_2");
    }

    @Test
    void enableBatchingOneConnectionCount() throws InterruptedException {

        setUp(BatchOptionsReactive.builder().batchSize(5).build());

        writeMeasurementsInThreads();

        Assertions.assertThat(okHttpClient.connectionPool().connectionCount()).isEqualTo(1);
    }

    @Test
    void disableBatchingMoreConnectionCount() throws InterruptedException {

        setUp(BatchOptionsReactive.DISABLED);

        writeMeasurementsInThreads();

        Assertions.assertThat(okHttpClient.connectionPool().connectionCount()).isGreaterThan(1);
    }

    @Test
    void writeNotMappableMeasurements() {

        setUp(BatchOptionsReactive.builder().batchSize(5).build());

        // Listeners
        TestObserver<UnhandledErrorEvent> errorHandler = influxDBReactive
                .listenEvents(UnhandledErrorEvent.class).test();

        TestObserver<WriteSuccessEvent> successHandler = influxDBReactive
                .listenEvents(WriteSuccessEvent.class).test();

        H2OFeetMeasurement h2oMeasurement1 = H2OFeetMeasurement.createMeasurement(1);
        H2OFeetMeasurement h2oMeasurement2 = H2OFeetMeasurement.createMeasurement(3);

        // write data
        influxDBReactive.writeMeasurements(Lists.newArrayList(h2oMeasurement1, 2, h2oMeasurement2));

        verifier.waitForResponse(1);

        errorHandler
                .assertValueCount(1)
                .assertValue(event -> {
                    
                    Assertions
                            .assertThat(event.getThrowable())
                            .isInstanceOf(InfluxDBException.class)
                            .hasMessage("Can not calculate InfluxDB Line Protocol for '2'")
                            .hasCauseInstanceOf(InfluxDBMapperException.class);
                    return true;
                });

        successHandler.assertValueCount(1).assertValue(event -> {

            Assertions.assertThat(event.getDataPoints())
                    .hasSize(2)
                    .containsExactlyInAnyOrder(h2oMeasurement1, h2oMeasurement2);

            return true;
        });

        List<H2OFeetMeasurement> h2oMeasurements = getMeasurements();
        Assertions.assertThat(h2oMeasurements).hasSize(2)
                .containsExactlyInAnyOrder(h2oMeasurement1, h2oMeasurement2);
    }

    private void writeMeasurementsInThreads() throws InterruptedException {

        ExecutorService scheduler = Executors.newFixedThreadPool(20);

        Flowable
                .range(0, 1000)
                .map(index -> (Callable<Maybe<H2OFeetMeasurement>>) () ->
                        influxDBReactive.writeMeasurement(H2OFeetMeasurement.createMeasurement(index)))
                .subscribe(scheduler::submit);

        Thread.sleep(5_000);

        influxDBReactive.close();
        verifier.waitForClose();
    }
}