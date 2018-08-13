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

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.bonitoo.influxdb.reactive.InfluxDBReactive;
import io.bonitoo.influxdb.reactive.events.AbstractInfluxEvent;
import io.bonitoo.influxdb.reactive.events.AbstractWriteEvent;
import io.bonitoo.influxdb.reactive.events.BackpressureEvent;
import io.bonitoo.influxdb.reactive.events.QueryParsedResponseEvent;
import io.bonitoo.influxdb.reactive.events.UnhandledErrorEvent;
import io.bonitoo.influxdb.reactive.events.WriteErrorEvent;
import io.bonitoo.influxdb.reactive.events.WritePartialEvent;
import io.bonitoo.influxdb.reactive.events.WriteSuccessEvent;
import io.bonitoo.influxdb.reactive.events.WriteUDPEvent;
import io.bonitoo.influxdb.reactive.options.BatchOptionsReactive;
import io.bonitoo.influxdb.reactive.options.InfluxDBOptions;
import io.bonitoo.influxdb.reactive.options.QueryOptions;
import io.bonitoo.influxdb.reactive.options.WriteOptions;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import org.influxdb.InfluxDBException;
import org.influxdb.dto.BoundParameterQuery;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.impl.AbstractInfluxDB;
import org.influxdb.impl.InfluxDBResultMapper;
import org.influxdb.impl.TimeUtil;
import org.reactivestreams.Publisher;
import retrofit2.HttpException;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

/**
 * @author Jakub Bednar (bednar@github) (01/06/2018 10:37)
 */
public class InfluxDBReactiveImpl extends AbstractInfluxDB<InfluxDBServiceReactive> implements InfluxDBReactive {

    private static final Logger LOG = Logger.getLogger(InfluxDBReactiveImpl.class.getName());

    private final PublishProcessor<AbstractData> processor;
    private final PublishSubject<Object> eventPublisher;
    private final Disposable writeConsumer;

    private final InfluxDBOptions options;
    private final BatchOptionsReactive batchOptions;
    @Nullable
    private final WriteOptions defaultWriteOptions;
    private final QueryOptions defaultQueryOptions;

    private final InfluxDBResultMapper resultMapper;

    public InfluxDBReactiveImpl(@Nonnull final InfluxDBOptions options) {
        this(options, BatchOptionsReactive.DEFAULTS);
    }

    public InfluxDBReactiveImpl(@Nonnull final InfluxDBOptions options,
                                @Nonnull final BatchOptionsReactive batchOptions) {

        this(options, batchOptions, Schedulers.newThread(), Schedulers.computation(), Schedulers.trampoline(),
                Schedulers.trampoline());
    }

    InfluxDBReactiveImpl(@Nonnull final InfluxDBOptions options,
                         @Nonnull final BatchOptionsReactive batchOptions,
                         @Nonnull final Scheduler processorScheduler,
                         @Nonnull final Scheduler batchScheduler,
                         @Nonnull final Scheduler jitterScheduler,
                         @Nonnull final Scheduler retryScheduler) {

        super(options.getUrl(), options.getOkHttpClient(), InfluxDBServiceReactive.class, options.getResponseFormat());

        //
        // Options
        //
        this.options = options;
        this.batchOptions = batchOptions;
        if (options.getDatabase() == null) {
            this.defaultWriteOptions = null;
        } else {
            this.defaultWriteOptions = WriteOptions.builder()
                    .database(options.getDatabase())
                    .consistencyLevel(options.getConsistencyLevel())
                    .retentionPolicy(options.getRetentionPolicy())
                    .precision(options.getPrecision())
                    .build();
        }
        this.defaultQueryOptions = QueryOptions.builder().build();

        this.resultMapper = new InfluxDBResultMapper();

        this.eventPublisher = PublishSubject.create();
        this.processor = PublishProcessor.create();
        this.writeConsumer = this.processor
                //
                // Backpressure
                //
                .onBackpressureBuffer(
                        batchOptions.getBufferLimit(),
                        () -> publish(new BackpressureEvent()),
                        batchOptions.getBackpressureStrategy())
                .observeOn(processorScheduler)
                //
                // Batching
                //
                .window(batchOptions.getFlushInterval(),
                        TimeUnit.MILLISECONDS,
                        batchScheduler,
                        batchOptions.getBatchSize(),
                        true)
                //
                // Jitter interval
                //
                .compose(jitter(jitterScheduler))
                .doOnError(throwable -> publish(new UnhandledErrorEvent(throwable)))
                .subscribe(new WritePointsConsumer(retryScheduler));
    }

    @Override
    protected void configureRetrofit(@Nonnull final Retrofit.Builder retrofitBuilder) {
        super.configureRetrofit(retrofitBuilder);

        retrofitBuilder.addCallAdapterFactory(RxJava2CallAdapterFactory.create());
    }

    @Override
    public <M> Maybe<M> writeMeasurement(@Nonnull final M measurement) {

        Objects.requireNonNull(measurement, "Measurement is required");
        Objects.requireNonNull(defaultWriteOptions, "Default WriteOptions are not defined. "
                + "Use write method with custom WriteOptions - #writeMeasurement(measurement, options).");

        return writeMeasurement(measurement, defaultWriteOptions);
    }

    @Override
    public <M> Maybe<M> writeMeasurement(@Nonnull final M measurement, @Nonnull final WriteOptions options) {

        Objects.requireNonNull(measurement, "Measurement is required");
        Objects.requireNonNull(options, "WriteOptions are required");

        return writeMeasurements(Flowable.just(measurement), options).firstElement();
    }

    @Override
    public <M> Flowable<M> writeMeasurements(@Nonnull final Iterable<M> measurements) {

        Objects.requireNonNull(measurements, "Measurements are required");
        Objects.requireNonNull(defaultWriteOptions, "Default WriteOptions are not defined. "
                + "Use write method with custom WriteOptions - #writeMeasurements(measurements, options).");

        return writeMeasurements(measurements, defaultWriteOptions);
    }

    @Override
    public <M> Flowable<M> writeMeasurements(@Nonnull final Iterable<M> measurements,
                                             @Nonnull final WriteOptions options) {

        Objects.requireNonNull(measurements, "Measurements are required");
        Objects.requireNonNull(options, "WriteOptions are required");

        return writeMeasurements(Flowable.fromIterable(measurements), options);
    }

    @Override
    public <M> Flowable<M> writeMeasurements(@Nonnull final Publisher<M> measurementStream) {

        Objects.requireNonNull(measurementStream, "Measurement stream is required");
        Objects.requireNonNull(defaultWriteOptions, "Default WriteOptions are not defined. "
                + "Use write method with custom WriteOptions - #writeMeasurements(measurementStream, options).");

        return writeMeasurements(measurementStream, defaultWriteOptions);
    }

    @Override
    public <M> Flowable<M> writeMeasurements(@Nonnull final Publisher<M> measurementStream,
                                             @Nonnull final WriteOptions options) {

        Objects.requireNonNull(measurementStream, "Measurement stream is required");
        Objects.requireNonNull(options, "WriteOptions are required");

        Flowable<M> emitting = Flowable.fromPublisher(measurementStream);
        writeDataPoints(emitting.map(measurement -> new MeasurementData<>(measurement, options)));

        return emitting;
    }

    @Override
    public Maybe<Point> writePoint(@Nonnull final Point point) {

        Objects.requireNonNull(point, "Point is required");
        Objects.requireNonNull(defaultWriteOptions, "Default WriteOptions are not defined. "
                + "Use write method with custom WriteOptions - #writePoint(point, options).");

        return writePoint(point, defaultWriteOptions);
    }

    @Override
    public Maybe<Point> writePoint(@Nonnull final Point point, @Nonnull final WriteOptions options) {

        Objects.requireNonNull(point, "Point is required");
        Objects.requireNonNull(options, "WriteOptions are required");

        return writePoints(Flowable.just(point), options).firstElement();
    }

    @Override
    public Flowable<Point> writePoints(@Nonnull final Iterable<Point> points) {

        Objects.requireNonNull(points, "Points are required");
        Objects.requireNonNull(defaultWriteOptions, "Default WriteOptions are not defined. "
                + "Use write method with custom WriteOptions - #writePoints(points, options).");

        return writePoints(points, defaultWriteOptions);
    }

    @Override
    public Flowable<Point> writePoints(@Nonnull final Iterable<Point> points, @Nonnull final WriteOptions options) {

        Objects.requireNonNull(points, "Points are required");
        Objects.requireNonNull(options, "WriteOptions are required");

        return writePoints(Flowable.fromIterable(points), options);
    }

    @Override
    public Flowable<Point> writePoints(@Nonnull final Publisher<Point> pointStream) {

        Objects.requireNonNull(pointStream, "Point stream is required");
        Objects.requireNonNull(defaultWriteOptions, "Default WriteOptions are not defined. "
                + "Use write method with custom WriteOptions - #writePoints(pointStream, options).");

        return writePoints(pointStream, defaultWriteOptions);
    }

    @Override
    public Flowable<Point> writePoints(@Nonnull final Publisher<Point> pointStream,
                                       @Nonnull final WriteOptions options) {

        Objects.requireNonNull(pointStream, "Point stream is required");
        Objects.requireNonNull(options, "WriteOptions are required");

        Flowable<Point> emitting = Flowable.fromPublisher(pointStream);
        writeDataPoints(emitting.map(point -> new PointData(point, options)));

        return emitting;
    }

    @Nonnull
    @Override
    public Maybe<String> writeRecord(@Nonnull final String record) {

        Objects.requireNonNull(record, "Record is required");
        Objects.requireNonNull(defaultWriteOptions, "Default WriteOptions are not defined. "
                + "Use write method with custom WriteOptions - #writeRecord(record, options).");

        return writeRecord(record, defaultWriteOptions);
    }

    @Nonnull
    @Override
    public Maybe<String> writeRecord(@Nonnull final String record, @Nonnull final WriteOptions options) {

        Objects.requireNonNull(record, "Record is required");
        Objects.requireNonNull(options, "WriteOptions are required");

        return writeRecords(Flowable.just(record), options).firstElement();
    }

    @Nonnull
    @Override
    public Flowable<String> writeRecords(@Nonnull final Iterable<String> records) {

        Objects.requireNonNull(records, "Records are required");
        Objects.requireNonNull(defaultWriteOptions, "Default WriteOptions are not defined. "
                + "Use write method with custom WriteOptions - #writeRecord(records, options).");

        return writeRecords(records, defaultWriteOptions);
    }

    @Nonnull
    @Override
    public Flowable<String> writeRecords(@Nonnull final Iterable<String> records, @Nonnull final WriteOptions options) {

        Objects.requireNonNull(records, "Records are required");
        Objects.requireNonNull(options, "WriteOptions are required");

        return writeRecords(Flowable.fromIterable(records), options);
    }

    @Nonnull
    @Override
    public Flowable<String> writeRecords(@Nonnull final Publisher<String> recordStream) {

        Objects.requireNonNull(recordStream, "Record stream is required");
        Objects.requireNonNull(defaultWriteOptions, "Default WriteOptions are not defined. "
                + "Use write method with custom WriteOptions - #writeRecords(recordStream, options).");

        return writeRecords(recordStream, defaultWriteOptions);
    }

    @Nonnull
    @Override
    public Flowable<String> writeRecords(@Nonnull final Publisher<String> recordStream,
                                         @Nonnull final WriteOptions options) {

        Objects.requireNonNull(recordStream, "Record stream is required");
        Objects.requireNonNull(options, "WriteOptions are required");

        Flowable<String> emitting = Flowable.fromPublisher(recordStream);
        writeDataPoints(emitting.map(record -> new RecordData(record, options)));

        return emitting;
    }

    @Override
    public <M> Flowable<M> query(@Nonnull final Query query, @Nonnull final Class<M> measurementType) {

        Objects.requireNonNull(query, "Query is required");
        Objects.requireNonNull(measurementType, "Measurement type is required");

        return query(Flowable.just(query), measurementType);
    }

    @Override
    public <M> Flowable<M> query(@Nonnull final Query query,
                                 @Nonnull final Class<M> measurementType,
                                 @Nonnull final QueryOptions queryOptions) {

        Objects.requireNonNull(query, "Query is required");
        Objects.requireNonNull(measurementType, "Measurement type is required");
        Objects.requireNonNull(queryOptions, "QueryOptions is required");

        return query(Flowable.just(query), measurementType, queryOptions);
    }

    @Override
    public <M> Flowable<M> query(@Nonnull final Publisher<Query> query, @Nonnull final Class<M> measurementType) {

        Objects.requireNonNull(query, "Query publisher is required");
        Objects.requireNonNull(measurementType, "Measurement type is required");

        return query(query, measurementType, defaultQueryOptions);
    }

    @Override
    public <M> Flowable<M> query(@Nonnull final Publisher<Query> query,
                                 @Nonnull final Class<M> measurementType,
                                 @Nonnull final QueryOptions queryOptions) {

        Objects.requireNonNull(query, "Query publisher is required");
        Objects.requireNonNull(measurementType, "Measurement type is required");
        Objects.requireNonNull(queryOptions, "QueryOptions is required");

        return query(query, queryOptions)
                .filter(queryResult -> queryResult.getResults() != null)
                .map(queryResult -> resultMapper.toPOJO(queryResult, measurementType, queryOptions.getPrecision()))
                .concatMap(Flowable::fromIterable);
    }

    @Override
    public Flowable<QueryResult> query(@Nonnull final Query query) {

        Objects.requireNonNull(query, "Query is required");

        return query(Flowable.just(query));
    }

    @Override
    public Flowable<QueryResult> query(@Nonnull final Query query, @Nonnull final QueryOptions queryOptions) {

        Objects.requireNonNull(query, "Query is required");
        Objects.requireNonNull(queryOptions, "QueryOptions is required");

        return query(Flowable.just(query), queryOptions);
    }

    @Override
    public Flowable<QueryResult> query(@Nonnull final Publisher<Query> queryStream) {

        Objects.requireNonNull(queryStream, "Query publisher is required");

        return query(queryStream, defaultQueryOptions);
    }

    @Override
    public Flowable<QueryResult> query(@Nonnull final Publisher<Query> queryStream,
                                       @Nonnull final QueryOptions queryOptions) {

        Objects.requireNonNull(queryStream, "Query publisher is required");
        Objects.requireNonNull(queryOptions, "QueryOptions is required");

        return Flowable.fromPublisher(queryStream).concatMap((Function<Query, Publisher<QueryResult>>) query -> {

            //
            // Parameters
            //
            String username = this.options.getUsername();
            String password = this.options.getPassword();
            String database = query.getDatabase();

            String precision = TimeUtil.toTimePrecision(queryOptions.getPrecision());

            int chunkSize = queryOptions.getChunkSize();
            String rawQuery = query.getCommandWithUrlEncoded();

            String params = "";
            if (query instanceof BoundParameterQuery) {
                params = ((BoundParameterQuery) query).getParameterJsonWithUrlEncoded();
            }
            return influxDBService
                    .query(username, password, database, precision, chunkSize, rawQuery, params)
                    .flatMap(
                            // success response
                            this::chunkReader,
                            // error response
                            throwable -> Observable.error(buildExceptionForThrowable(throwable)),
                            // end of response
                            Observable::empty)
                    .toFlowable(BackpressureStrategy.BUFFER);
        });
    }

    @Override
    @Nonnull
    public <T extends AbstractInfluxEvent> Observable<T> listenEvents(@Nonnull final Class<T> eventType) {

        Objects.requireNonNull(eventType, "EventType is required");

        return eventPublisher.ofType(eventType);
    }

    @Override
    public Maybe<Pong> ping() {

        SubscribeHandler onSubscribe = new SubscribeHandler();

        return influxDBService
                .ping()
                .doOnSubscribe(onSubscribe)
                .map(response -> createPong(onSubscribe.subscribeTime, response));
    }

    @Nonnull
    @Override
    public Maybe<String> version() {
        return ping().map(Pong::getVersion);
    }

    @Nonnull
    @Override
    public InfluxDBReactive enableGzip() {
        this.gzipRequestInterceptor.enable();
        return this;
    }

    @Nonnull
    @Override
    public InfluxDBReactive disableGzip() {
        this.gzipRequestInterceptor.disable();
        return this;
    }

    @Override
    public boolean isGzipEnabled() {
        return this.gzipRequestInterceptor.isEnabled();
    }


    @Nonnull
    @Override
    public InfluxDBReactive close() {

        LOG.log(Level.INFO, "Flushing any cached metrics before shutdown.");

        try {
            processor.onComplete();
            eventPublisher.onComplete();
        } finally {
            super.destroy();
        }

        return this;
    }

    @Override
    public boolean isClosed() {
        return writeConsumer.isDisposed();
    }

    private <DP extends AbstractData> void writeDataPoints(@Nonnull final Publisher<DP> pointStream) {

        Objects.requireNonNull(pointStream, "Point stream is required");

        Flowable.fromPublisher(pointStream)
                .subscribe(processor::onNext, throwable -> publish(new UnhandledErrorEvent(throwable)));
    }

    @Nonnull
    private <T extends AbstractData> FlowableTransformer<Flowable<T>, Flowable<T>> jitter(
            @Nonnull final Scheduler scheduler) {

        Objects.requireNonNull(scheduler, "Jitter scheduler is required");

        return source -> {

            //
            // source without jitter
            //
            if (batchOptions.getJitterInterval() <= 0) {
                return source;
            }

            //
            // Add jitter => dynamic delay
            //
            return source.delay((Function<Flowable<T>, Flowable<Long>>) pointFlowable -> {

                int delay = jitterDelay();

                LOG.log(Level.FINEST, "Generated Jitter dynamic delay: {0}", delay);

                return Flowable.timer(delay, TimeUnit.MILLISECONDS, scheduler);
            });
        };
    }

    private final class WritePointsConsumer implements Consumer<Flowable<AbstractData>> {

        private final Scheduler retryScheduler;

        private WritePointsConsumer(@Nonnull final Scheduler retryScheduler) {

            Objects.requireNonNull(retryScheduler, "RetryScheduler is required");

            this.retryScheduler = retryScheduler;
        }

        @Override
        public void accept(final Flowable<AbstractData> flowablePoints) {

            flowablePoints
                    //
                    // Group by key - same database, same retention policy...
                    //
                    .groupBy(AbstractData::getWriteOptions)
                    .subscribeOn(batchOptions.getWriteScheduler())
                    .subscribe(group -> {

                        WriteOptions writeOptions = group.getKey();
                        group
                                .toList()
                                .filter(dataPoints -> !dataPoints.isEmpty())
                                .subscribe(
                                        dataPoints -> writeDataPoints(writeOptions, dataPoints),
                                        throwable -> publish(new UnhandledErrorEvent(throwable)));
                    }, throwable -> publish(new UnhandledErrorEvent(throwable)));

        }

        private void writeDataPoints(@Nonnull final WriteOptions writeOptions,
                                     @Nonnull final List<AbstractData> dataPoints) {

            Objects.requireNonNull(writeOptions, "WriteOptions are required");
            Objects.requireNonNull(dataPoints, "DatePoints are required");

            //
            // Data which are not parsable to InfluxDB Line Protocol
            //
            List<AbstractData> notParsable = new ArrayList<>();

            //
            // Success action
            //
            Action success = () -> {
                List<?> points = toDataPoints(dataPoints, notParsable);

                AbstractWriteEvent event;
                if (writeOptions.isUdpEnable()) {
                    event = new WriteUDPEvent(points, writeOptions);
                } else {

                    event = new WriteSuccessEvent(points, writeOptions);
                }

                publish(event);
            };

            //
            // Fail action
            //
            Consumer<Throwable> fail = throwable -> {

                // HttpException is handled in retryHandler
                if (throwable instanceof HttpException) {
                    return;
                }

                publish(new UnhandledErrorEvent(throwable));
            };

            //
            // Data => InfluxDB Line Protocol
            //

            String body = dataPoints.stream()
                    .map(data -> {
                        try {
                            return data.lineProtocol();
                        } catch (Exception e) {
                            //
                            // Data are not parsable to InfluxDB Line Protocol
                            //
                            notParsable.add(data);

                            String errorMessage = String
                                    .format("Can not calculate InfluxDB Line Protocol for '%s'", data.getData());

                            publish(new UnhandledErrorEvent(new InfluxDBException(errorMessage, e)));

                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("\n"));

            if (body.isEmpty()) {

                String message = "The points {0} are parsed to empty request body => skip call InfluxDB server.";

                LOG.log(Level.FINE, message, dataPoints);

                return;
            }

            //
            // InfluxDB Line Protocol => to Request Body
            //
            RequestBody requestBody = RequestBody.create(options.getMediaType(), body);

            //
            // Parameters
            //
            String username = options.getUsername();
            String password = options.getPassword();
            String database = writeOptions.getDatabase();

            String retentionPolicy = writeOptions.getRetentionPolicy();
            String precision = TimeUtil.toTimePrecision(writeOptions.getPrecision());
            String consistencyLevel = writeOptions.getConsistencyLevel().value();

            Completable completable;
            if (writeOptions.isUdpEnable()) {

                completable = Completable.fromAction(
                        () -> writeRecordsThroughUDP(writeOptions.getUdpPort(), body));

            } else {

                Callable<List<Object>> points = () -> toDataPoints(dataPoints, notParsable);

                completable = influxDBService.writePoints(
                        username, password, database,
                        retentionPolicy, precision, consistencyLevel,
                        requestBody)
                        //
                        // Retry strategy
                        //
                        .retryWhen(retryHandler(retryScheduler, writeOptions, points));
            }

            completable.subscribe(success, fail);
        }
    }

    /**
     * The retry handler that tries to retry a write if it failed previously and
     * the reason of the failure is not permanent.
     *
     * @param retryScheduler for scheduling retry write
     * @param writeOptions   options for write to InfluxDB
     * @param points         to write to InfluxDB
     * @return the retry handler
     */
    @Nonnull
    private Function<Flowable<Throwable>, Publisher<?>> retryHandler(@Nonnull final Scheduler retryScheduler,
                                                                     @Nonnull final WriteOptions writeOptions,
                                                                     @Nonnull final Callable<List<Object>> points) {

        Objects.requireNonNull(points, "Points are required");
        Objects.requireNonNull(writeOptions, "WriteOptions are required");
        Objects.requireNonNull(retryScheduler, "RetryScheduler is required");

        return errors -> errors.flatMap(throwable -> {

            if (throwable instanceof HttpException) {

                InfluxDBException influxDBException = buildExceptionForThrowable(throwable);

                List<Object> dataPoints = points.call();

                //
                // Partial Write => skip retry
                //
                if (influxDBException.getMessage().startsWith("partial write")) {
                    publish(new WritePartialEvent(dataPoints, writeOptions, influxDBException));

                    return Flowable.error(throwable);
                }

                publish(new WriteErrorEvent(dataPoints, writeOptions, influxDBException));

                //
                // Retry request
                //
                if (influxDBException.isRetryWorth()) {

                    int retryInterval = batchOptions.getRetryInterval() + jitterDelay();

                    return Flowable.just("notify").delay(retryInterval, TimeUnit.MILLISECONDS, retryScheduler);
                }
            }

            //
            // This type of throwable is not able to retry
            //
            return Flowable.error(throwable);
        });
    }

    @Nonnull
    private List<Object> toDataPoints(@Nonnull final List<AbstractData> points,
                                      @Nonnull final List<AbstractData> notParsable) {

        Objects.requireNonNull(points, "Points are required");
        Objects.requireNonNull(notParsable, "Not parsable points are required");

        return points.stream()
                .filter(dataPoint -> !notParsable.contains(dataPoint))
                .map(AbstractData::getData)
                .collect(Collectors.toList());
    }

    private int jitterDelay() {

        return (int) (Math.random() * batchOptions.getJitterInterval());
    }

    @Nonnull
    private Observable<QueryResult> chunkReader(@Nonnull final ResponseBody body) {

        Objects.requireNonNull(body, "ResponseBody is required");

        return Observable.create(subscriber -> {

            boolean isCompleted = false;
            try {
                BufferedSource source = body.source();

                Supplier<QueryResult> queryResultSupplier = chunkProccesor.chunkSupplier(source);

                //
                // Subscriber is not disposed && source has data => parse
                //
                while (!subscriber.isDisposed()) {


                    QueryResult queryResult = queryResultSupplier.get();
                    if (queryResult != null) {

                        subscriber.onNext(queryResult);
                        publish(new QueryParsedResponseEvent(source, queryResult));
                    } else {
                        // query result is null => exhausted source
                        break;
                    }
                }
            } catch (Exception e) {

                //
                // Socket close by remote server or end of data
                //
                if (isEOFException(e)) {
                    isCompleted = true;
                    subscriber.onComplete();
                } else {
                    throw new InfluxDBException(e);
                }
            }

            //if response end we get here
            if (!isCompleted) {
                subscriber.onComplete();
            }

            body.close();
        });
    }

    private <T extends AbstractInfluxEvent> void publish(@Nonnull final T event) {

        Objects.requireNonNull(event, "Event is required");

        event.logEvent();
        eventPublisher.onNext(event);
    }

    private class SubscribeHandler implements Consumer<Disposable> {

        private Long subscribeTime;

        @Override
        public void accept(final Disposable disposable) {
            subscribeTime = System.currentTimeMillis();
        }
    }

    @Nonnull
    private InfluxDBException buildExceptionForThrowable(@Nonnull final Throwable throwable) {

        Objects.requireNonNull(throwable, "Throwable is required");

        if (throwable instanceof HttpException) {

            Response<?> response = ((HttpException) throwable).response();
            String errorHeader = response.headers().get("X-Influx-Error");
            // build from error header
            if (errorHeader != null) {
                return InfluxDBException.buildExceptionFromErrorMessage(errorHeader);
            }
            // build from error body
            try (ResponseBody errorBody = response.errorBody()) {
                if (errorBody != null) {
                    return InfluxDBException.buildExceptionForErrorState(errorBody.string());
                }
            } catch (IOException e) {
                return new InfluxDBException(e);
            }
        }

        return new InfluxDBException(throwable);
    }

    private boolean isEOFException(@Nullable final Throwable e) {

        if (e == null) {
            return false;
        } else if (e.getMessage().contains("Socket closed") || e instanceof EOFException) {
            return true;
        } else {
            return isEOFException(e.getCause());
        }
    }
}
