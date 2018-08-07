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
import javax.annotation.Nonnull;

import io.bonitoo.influxdb.reactive.impl.AbstractITInfluxDBReactiveTest;
import io.bonitoo.influxdb.reactive.options.BatchOptionsReactive;
import io.bonitoo.influxdb.reactive.options.WriteOptions;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import org.assertj.core.api.Assertions;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * @author Jakub Bednar (bednar@github) (19/06/2018 14:29)
 */
@RunWith(JUnitPlatform.class)
class ITInfluxDBReactiveWriteUDP extends AbstractITInfluxDBReactiveTest {

    private final static int UDP_PORT = 8089;
    private final static String UDP_DATABASE = "udp";

    private WriteOptions udpWriteOptions;

    @BeforeEach
    void setUp() {

        super.setUp(BatchOptionsReactive.builder().batchSize(1).build());

        simpleQuery("CREATE DATABASE " + UDP_DATABASE);

        udpWriteOptions = WriteOptions.builder().udp(true, UDP_PORT).build();
    }

    @AfterEach
    void cleanUp() {
        simpleQuery("DROP DATABASE " + UDP_DATABASE);

        influxDBReactive.close();
    }

    @Test
    void writePointThroughUDP() {

        Point point = Point.measurement("h2o_feetPoint")
                .tag("location", "coyote_creek")
                .addField("water_level", 2.928)
                .addField("level description", "below 3 feet")
                .time(1440046800000000L, TimeUnit.NANOSECONDS)
                .build();

        influxDBReactive.writePoint(point, udpWriteOptions);

        assertSavedMeasurement("h2o_feetPoint");
    }

    @Test
    void writeMeasurementThroughUDP() {

        H2OFeetMeasurement measurement =
                new H2OFeetMeasurement("coyote_creek", 2.928, "below 3 feet", 1440046800L);

        influxDBReactive.writeMeasurement(measurement, udpWriteOptions);

        assertSavedMeasurement("h2o_feet");
    }

    @Test
    void writeRecordThroughUDP() {

        String record = "h2o_feetRecord,location=coyote_creek "
                + "level\\ description=\"below 3 feet\",water_level=2.928 1440046800000000";

        influxDBReactive.writeRecord(record, udpWriteOptions);

        assertSavedMeasurement("h2o_feetRecord");
    }

    private void assertSavedMeasurement(@Nonnull final String measurementName) {

        Assertions.assertThat(measurementName).isNotNull();

        verifier.waitForResponse(1);

        // wait 2 seconds
        Flowable.interval(2, TimeUnit.SECONDS, Schedulers.trampoline()).take(1).subscribe();

        influxDBReactive.query(new Query("select * from " + measurementName + " group by *", UDP_DATABASE))
                .test()
                .assertValueCount(1)
                .assertValue(result -> {

                    Assertions.assertThat(result.getError()).isNull();
                    Assertions.assertThat(result.getResults()).hasSize(1);
                    Assertions.assertThat(result.getResults().get(0).getSeries()).hasSize(1);
                    QueryResult.Series series = result.getResults().get(0).getSeries().get(0);

                    // tags
                    Assertions.assertThat(series.getName()).isEqualTo(measurementName);
                    Assertions.assertThat(series.getTags())
                            .hasEntrySatisfying("location",
                                    value -> Assertions.assertThat(value).isEqualTo("coyote_creek"));

                    // fields
                    Assertions.assertThat(series.getColumns())
                            .containsExactlyInAnyOrder("time", "level description", "water_level");
                    Assertions.assertThat(series.getValues()).hasSize(1);
                    Assertions.assertThat(series.getValues().get(0))
                            .containsExactlyInAnyOrder(1440046800000000.0, "below 3 feet", 2.928);

                    return true;
                });
    }
}

