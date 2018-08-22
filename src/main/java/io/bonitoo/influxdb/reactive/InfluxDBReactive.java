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

import io.bonitoo.influxdb.reactive.events.AbstractInfluxEvent;
import io.bonitoo.influxdb.reactive.options.QueryOptions;
import io.bonitoo.influxdb.reactive.options.WriteOptions;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.annotations.Experimental;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.reactivestreams.Publisher;

/**
 * Proof-of-concept API.
 * <p>
 * Not Implemented:
 * <ul>
 * <li>Backpressure - wait</li>
 * </ul>
 *
 * @author Jakub Bednar (bednar@github) (29/05/2018 14:58)
 * @since 1.0.0
 */
@Experimental
public interface InfluxDBReactive {

    /**
     * Default Retention policy.
     */
    public static final String DEFAULT_RETENTION_POLICY = "autogen";

    /**
     * Default Consistency Level for Write Operations.
     */
    public static final InfluxDB.ConsistencyLevel DEFAULT_CONSISTENCY_LEVEL = InfluxDB.ConsistencyLevel.ONE;

    /**
     * Default Time Precision for Write Operations.
     */
    public static final TimeUnit DEFAULT_PRECISION = TimeUnit.NANOSECONDS;

    /**
     * Write a single Measurement to the default database.
     *
     * @param measurement The measurement to write
     * @param <M>         The type of the measurement (POJO)
     * @return {@link Maybe} emitting the saved measurement.
     */
    <M> Maybe<M> writeMeasurement(@Nonnull final M measurement);

    /**
     * Write a single Measurement to the default database.
     *
     * @param measurement The measurement to write
     * @param <M>         The type of the measurement (POJO)
     * @param options     the configuration of the write
     * @return {@link Maybe} emitting the saved measurement.
     */
    <M> Maybe<M> writeMeasurement(@Nonnull final M measurement, @Nonnull final WriteOptions options);

    /**
     * Write a bag of Measurements to the default database.
     *
     * @param measurements The measurements to write
     * @param <M>          The type of the measurement (POJO)
     * @return {@link Flowable} emitting the saved measurements.
     */
    <M> Flowable<M> writeMeasurements(@Nonnull final Iterable<M> measurements);

    /**
     * Write a bag of Measurements to the default database.
     *
     * @param measurements The measurements to write
     * @param <M>          The type of the measurement (POJO)
     * @param options      the configuration of the write
     * @return {@link Flowable} emitting the saved measurements.
     */
    <M> Flowable<M> writeMeasurements(@Nonnull final Iterable<M> measurements, @Nonnull final WriteOptions options);

    /**
     * Write a stream of Measurements to the default database.
     *
     * @param measurementStream The stream of measurements to write
     * @param <M>               The type of the measurement (POJO)
     * @return {@link Flowable} emitting the saved measurements.
     */
    <M> Flowable<M> writeMeasurements(@Nonnull final Publisher<M> measurementStream);

    /**
     * Write a stream of Measurements to the default database.
     *
     * @param measurementStream The stream of measurements to write
     * @param <M>               The type of the measurement (POJO)
     * @param options           the configuration of the write
     * @return {@link Flowable} emitting the saved measurements.
     */
    <M> Flowable<M> writeMeasurements(@Nonnull final Publisher<M> measurementStream,
                                      @Nonnull final WriteOptions options);

    /**
     * Write a single Point to the default database.
     *
     * @param point The point to write
     * @return {@link Maybe} emitting the saved point.
     */
    Maybe<Point> writePoint(@Nonnull final Point point);

    /**
     * Write a single Point to the default database.
     *
     * @param point   The point to write
     * @param options the configuration of the write
     * @return {@link Maybe} emitting the saved point.
     */
    Maybe<Point> writePoint(@Nonnull final Point point, @Nonnull final WriteOptions options);

    /**
     * Write a bag of Points to the default database.
     *
     * @param points The points to write
     * @return {@link Flowable} emitting the saved points.
     */
    Flowable<Point> writePoints(@Nonnull final Iterable<Point> points);

    /**
     * Write a bag of Points to the default database.
     *
     * @param points  The points to write
     * @param options the configuration of the write
     * @return {@link Flowable} emitting the saved points.
     */
    Flowable<Point> writePoints(@Nonnull final Iterable<Point> points, @Nonnull final WriteOptions options);

    /**
     * Write a stream of Points to the default database.
     *
     * @param pointStream The stream of points to write
     * @return {@link Flowable} emitting the saved points.
     */
    Flowable<Point> writePoints(@Nonnull final Publisher<Point> pointStream);

    /**
     * Write a stream of Points to the default database.
     *
     * @param pointStream The stream of points to write
     * @param options     the configuration of the write
     * @return {@link Flowable} emitting the saved points.
     */
    Flowable<Point> writePoints(@Nonnull final Publisher<Point> pointStream, @Nonnull final WriteOptions options);

    /**
     * Write data point in InfluxDB Line Protocol into database.
     *
     * @param record the data point to write
     * @return {@link Flowable} emitting the saved data points.
     */
    @Nonnull
    Maybe<String> writeRecord(@Nonnull final String record);

    /**
     * Write data point in InfluxDB Line Protocol into database.
     *
     * @param options the configuration of the write
     * @param record  the data point to write
     * @return {@link Flowable} emitting the saved data points.
     */
    @Nonnull
    Maybe<String> writeRecord(@Nonnull final String record, @Nonnull final WriteOptions options);

    /**
     * Write a bag of data points in InfluxDB Line Protocol into database.
     *
     * @param records the data points to write
     * @return {@link Flowable} emitting the saved data points.
     */
    @Nonnull
    Flowable<String> writeRecords(@Nonnull final Iterable<String> records);

    /**
     * Write a bag of data points in InfluxDB Line Protocol into database.
     *
     * @param options the configuration of the write
     * @param records the data points to write
     * @return {@link Flowable} emitting the saved data points.
     */
    @Nonnull
    Flowable<String> writeRecords(@Nonnull final Iterable<String> records, @Nonnull final WriteOptions options);

    /**
     * Write a a stream of data points in InfluxDB Line Protocol into database.
     *
     * @param recordStream the stream of data points to write
     * @return {@link Flowable} emitting the saved data points.
     */
    @Nonnull
    Flowable<String> writeRecords(@Nonnull final Publisher<String> recordStream);

    /**
     * Write a a stream of data points in InfluxDB Line Protocol into database.
     *
     * @param recordStream the stream of data points to write
     * @param options      the configuration of the write
     * @return {@link Flowable} emitting the saved data points.
     */
    @Nonnull
    Flowable<String> writeRecords(@Nonnull final Publisher<String> recordStream, @Nonnull final WriteOptions options);

    /**
     * Execute a query against a default database.
     *
     * @param query           the query to execute.
     * @param measurementType The type of the measurement (POJO)
     * @param <M>             The type of the measurement (POJO)
     * @return {@link Flowable} emitting a List of Series mapped to {@code measurementType} which are matched the query
     * or {@link Flowable#empty()} if none found.
     */
    <M> Flowable<M> query(@Nonnull final Query query, @Nonnull final Class<M> measurementType);

    /**
     * Execute a query against a default database.
     *
     * @param query           the query to execute.
     * @param measurementType The type of the measurement (POJO)
     * @param <M>             The type of the measurement (POJO)
     * @param queryOptions    the configuration of the query
     * @return {@link Flowable} emitting a List of Series mapped to {@code measurementType} which are matched the query
     * or {@link Flowable#empty()} if none found.
     */
    <M> Flowable<M> query(@Nonnull final Query query,
                          @Nonnull final Class<M> measurementType,
                          @Nonnull final QueryOptions queryOptions);

    /**
     * Execute a query against a default database.
     *
     * @param query           the query to execute. Uses the first emitted element to perform the find-query.
     * @param measurementType The type of the measurement (POJO)
     * @param <M>             The type of the measurement (POJO)
     * @return {@link Flowable} emitting a List of Series mapped to {@code measurementType} which are matched the query
     * or {@link Flowable#empty()} if none found.
     */
    <M> Flowable<M> query(@Nonnull final Publisher<Query> query, @Nonnull final Class<M> measurementType);

    /**
     * Execute a query against a default database.
     *
     * @param query           the query to execute. Uses the first emitted element to perform the find-query.
     * @param measurementType The type of the measurement (POJO)
     * @param <M>             The type of the measurement (POJO)
     * @param queryOptions    the configuration of the query
     * @return {@link Flowable} emitting a List of Series mapped to {@code measurementType} which are matched the query
     * or {@link Flowable#empty()} if none found.
     */
    <M> Flowable<M> query(@Nonnull final Publisher<Query> query,
                          @Nonnull final Class<M> measurementType,
                          @Nonnull final QueryOptions queryOptions);

    /**
     * Execute a query against a default database.
     *
     * @param query the query to execute.
     * @return {@link Single} emitting a List of Series which matched the query or
     * {@link Flowable#empty()} if none found.
     */
    Flowable<QueryResult> query(@Nonnull final Query query);

    /**
     * Execute a query against a default database.
     *
     * @param query   the query to execute.
     * @param queryOptions the configuration of the query
     * @return {@link Single} emitting a List of Series which matched the query or
     * {@link Flowable#empty()} if none found.
     */
    Flowable<QueryResult> query(@Nonnull final Query query, @Nonnull final QueryOptions queryOptions);

    /**
     * Execute a query against a default database.
     *
     * @param queryStream the query to execute. Uses the first emitted element to perform the find-query.
     * @return {@link Single} emitting a List of Series which matched the query or
     * {@link Flowable#empty()} if none found.
     */
    Flowable<QueryResult> query(@Nonnull final Publisher<Query> queryStream);

    /**
     * Execute a query against a default database.
     *
     * @param queryStream the query to execute. Uses the first emitted element to perform the find-query.
     * @param queryOptions     the configuration of the query
     * @return {@link Single} emitting a List of Series which matched the query or
     * {@link Flowable#empty()} if none found.
     */
    Flowable<QueryResult> query(@Nonnull final Publisher<Query> queryStream, @Nonnull final QueryOptions queryOptions);

    /**
     * Listen the events produced by {@link InfluxDBReactive}.
     *
     * @param eventType type of event to listen
     * @param <T>       type of event to listen
     * @return lister for {@code eventType} events
     */
    @Nonnull
    <T extends AbstractInfluxEvent> Observable<T> listenEvents(@Nonnull Class<T> eventType);

    /**
     * Ping this he connected InfluxDB Server.
     *
     * @return the response of the ping execution.
     */
    Maybe<Pong> ping();

    /**
     * Return the version of the connected InfluxDB Server.
     *
     * @return the version String, otherwise unknown.
     */
    @Nonnull
    Maybe<String> version();

    /**
     * Set the LogLevel which is used for REST related actions.
     *
     * @param logLevel the LogLevel to set.
     * @return the InfluxDBReactive instance to be able to use it in a fluent manner.
     */
    InfluxDBReactive setLogLevel(@Nonnull final InfluxDB.LogLevel logLevel);

    /**
     * Enable Gzip compress for http request body.
     *
     * @return the InfluxDBReactive instance to be able to use it in a fluent manner.
     */
    @Nonnull
    InfluxDBReactive enableGzip();

    /**
     * Disable Gzip compress for http request body.
     *
     * @return the InfluxDBReactive instance to be able to use it in a fluent manner.
     */
    @Nonnull
    InfluxDBReactive disableGzip();

    /**
     * Returns whether Gzip compress for http request body is enabled.
     *
     * @return true if gzip is enabled.
     */
    boolean isGzipEnabled();

    /**
     * Close thread for asynchronous batch writes.
     *
     * @return the InfluxDBReactive instance to be able to use it in a fluent manner.
     */
    @Nonnull
    InfluxDBReactive close();

    /**
     * @return {@link Boolean#TRUE} if all metrics are flushed
     */
    boolean isClosed();
}
