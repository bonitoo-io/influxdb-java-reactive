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

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

import io.bonitoo.influxdb.reactive.H2OFeetMeasurement;
import io.bonitoo.influxdb.reactive.InfluxDBReactive;
import io.bonitoo.influxdb.reactive.options.BatchOptionsReactive;
import io.bonitoo.influxdb.reactive.options.InfluxDBOptions;

import okhttp3.OkHttpClient;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.junit.jupiter.api.AfterEach;

/**
 * @author Jakub Bednar (bednar@github) (05/06/2018 09:14)
 */
public abstract class AbstractITInfluxDBReactive {

    private static final Logger LOG = Logger.getLogger(AbstractITInfluxDBReactive.class.getName());

    protected static final String DATABASE_NAME = "reactive_database";

    protected InfluxDBReactive influxDBReactive;
    protected InfluxDBReactiveVerifier verifier;
    protected OkHttpClient okHttpClient;

    protected void setUp(@Nonnull final BatchOptionsReactive batchOptions) {
        setUp(batchOptions, InfluxDB.ResponseFormat.JSON);
    }

    protected void setUp(@Nonnull final BatchOptionsReactive batchOptions,
                         @Nonnull final InfluxDB.ResponseFormat responseFormat) {

        Objects.requireNonNull(batchOptions, "BatchOptionsReactive is required");
        Objects.requireNonNull(responseFormat, "InfluxDB.ResponseFormat is required");

        String influxdbIP = System.getenv().getOrDefault("INFLUXDB_IP", "127.0.0.1");
        String influxdbPort = System.getenv().getOrDefault("INFLUXDB_PORT_API", "8086");

        InfluxDBOptions options = InfluxDBOptions.builder()
                .url("http://" + influxdbIP + ":" + influxdbPort)
                .username("admin")
                .password("admin")
                .database(DATABASE_NAME)
                .precision(TimeUnit.NANOSECONDS)
                .responseFormat(responseFormat)
                .build();

        influxDBReactive = new InfluxDBReactiveWrapper(options, batchOptions);
        verifier = new InfluxDBReactiveVerifier(influxDBReactive);

        simpleQuery("CREATE DATABASE " + DATABASE_NAME);

        verifier.reset();
    }

    @AfterEach
    void cleanUp() {
        simpleQuery("DROP DATABASE " + DATABASE_NAME);

        influxDBReactive.close();
    }

    protected void simpleQuery(@Nonnull final String simpleQuery) {

        Objects.requireNonNull(simpleQuery, "SimpleQuery is required");
        QueryResult result = influxDBReactive.query(new Query(simpleQuery, null)).blockingSingle();

        LOG.log(Level.FINEST, "Simple query: {0} result: {1}", new Object[]{simpleQuery, result});
    }

    @Nonnull
    protected List<H2OFeetMeasurement> getMeasurements() {
        return getMeasurements(DATABASE_NAME);
    }

    @Nonnull
    private List<H2OFeetMeasurement> getMeasurements(@Nonnull final String databaseName) {

        Objects.requireNonNull(databaseName, "Database name is required");

        Query reactive_database = new Query("select * from h2o_feet group by *", databaseName);

        return influxDBReactive.query(reactive_database, H2OFeetMeasurement.class).toList().blockingGet();
    }

    private class InfluxDBReactiveWrapper extends InfluxDBReactiveImpl {

        private InfluxDBReactiveWrapper(@Nonnull final InfluxDBOptions options,
                                        @Nonnull final BatchOptionsReactive batchOptions) {

            super(options, batchOptions);

            AbstractITInfluxDBReactive.this.okHttpClient = (OkHttpClient) this.retrofit.callFactory();
        }
    }
}
