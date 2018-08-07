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

import java.time.Instant;

import io.bonitoo.influxdb.reactive.impl.AbstractInfluxDBReactiveTest;
import io.bonitoo.influxdb.reactive.options.BatchOptionsReactive;

import io.reactivex.Flowable;
import okhttp3.mockwebserver.MockResponse;
import org.assertj.core.api.Assertions;
import org.influxdb.InfluxDBException;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.impl.TimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * @author Jakub Bednar (bednar@github) (11/06/2018 07:45)
 */
@RunWith(JUnitPlatform.class)
class InfluxDBReactiveQueryTest extends AbstractInfluxDBReactiveTest {

    @BeforeEach
    void setUp() {
        super.setUp(BatchOptionsReactive.DEFAULTS);
    }

    @Test
    void query() {

        Query query = new Query("select * from h2o_feet group by *", "reactive_database");
        String body = "{\"results\":[{\"statement_id\":0,\"series\":[{\"name\":\"h2o_feet\"," +
                "\"tags\":{\"location\":\"coyote_creek\"},\"columns\":[\"time\",\"level description\",\"water_level\"],"
                + "\"values\":[[\"1970-01-01T00:00:00.001Z\",\"below 3 feet\",2.927]]}]}]}";

        influxDBServer.enqueue(new MockResponse().setBody(body));

        Flowable<QueryResult> result = influxDBReactive.query(query);

        result.test()
                .assertValueCount(1)
                .assertValue(queryResult -> {

                    Assertions.assertThat(queryResult).isNotNull();
                    Assertions.assertThat(queryResult.getError()).isNull();
                    Assertions.assertThat(queryResult.getResults().size()).isEqualTo(1);
                    Assertions.assertThat(queryResult.getResults().get(0).getSeries().size()).isEqualTo(1);
                    Assertions.assertThat(queryResult.getResults().get(0).getError()).isNull();

                    QueryResult.Series series = queryResult.getResults().get(0).getSeries().get(0);
                    Assertions.assertThat(series.getName()).isEqualTo("h2o_feet");

                    // columns
                    Assertions.assertThat(series.getColumns())
                            .contains("time", "level description", "water_level");
                    // tags
                    Assertions.assertThat(series.getTags()).containsEntry("location", "coyote_creek");
                    // values
                    Assertions.assertThat(series.getValues().size()).isEqualTo(1);
                    Assertions.assertThat(series.getValues().get(0))
                            .contains("below 3 feet")
                            .contains(2.927d)
                            .contains(TimeUtil.toInfluxDBTimeFormat(1));

                    return true;
                });

    }

    @Test
    void queryToMeasurement() {

        Query query = new Query("select * from h2o_feet group by *", "reactive_database");
        String body = "{\"results\":[{\"statement_id\":0,\"series\":[{\"name\":\"h2o_feet\"," +
                "\"tags\":{\"location\":\"coyote_creek\"},\"columns\":[\"time\",\"level description\",\"water_level\"],"
                + "\"values\":[[\"1970-01-01T00:00:00.001Z\",\"below 3 feet\",2.927]]}]}]}";

        influxDBServer.enqueue(new MockResponse().setBody(body));

        Flowable<H2OFeetMeasurement> result = influxDBReactive.query(query, H2OFeetMeasurement.class);

        result.test().assertValueCount(1).assertValue(h2oFeetMeasurement -> {

            Assertions.assertThat(h2oFeetMeasurement.getLocation()).isEqualTo("coyote_creek");
            Assertions.assertThat(h2oFeetMeasurement.getLevel()).isEqualTo(2.927d);
            Assertions.assertThat(h2oFeetMeasurement.getDescription()).isEqualTo("below 3 feet");
            Assertions.assertThat(h2oFeetMeasurement.getTime()).isEqualTo(Instant.ofEpochMilli(1));

            return true;
        });
    }

    @Test
    void empty() {

        Query query = new Query("select * from not_exist group by *", "reactive_database");
        String body = "{\"results\":[{\"statement_id\":0}]}";

        influxDBServer.enqueue(new MockResponse().setBody(body));

        Flowable<QueryResult> result = influxDBReactive
                .query(query);

        result.test()
                .assertValueCount(1)
                .assertValue(queryResult -> {

                    Assertions.assertThat(queryResult).isNotNull();
                    Assertions.assertThat(queryResult.getError()).isNull();
                    Assertions.assertThat(queryResult.getResults().size()).isEqualTo(1);
                    Assertions.assertThat(queryResult.getResults().get(0).getError()).isNull();
                    Assertions.assertThat(queryResult.getResults().get(0).getSeries()).isNull();

                    return true;
                });
    }

    @Test
    void error() {

        Query query = new Query("select * ", "reactive_database");

        String errorMessage = "error parsing query: found EOF, expected FROM at line 1, char 9";
        influxDBServer.enqueue(createErrorResponse(errorMessage));

        Flowable<QueryResult> result = influxDBReactive.query(query);

        result.test()
                .assertValueCount(0)
                .assertError(InfluxDBException.class)
                .assertErrorMessage("error parsing query: found EOF, expected FROM at line 1, char 9");
    }
}

