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
package io.bonitoo.influxdb.reactive.options;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

import io.reactivex.BackpressureOverflowStrategy;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import org.influxdb.BatchOptions;
import org.influxdb.impl.Preconditions;

import static org.influxdb.BatchOptions.DEFAULT_BATCH_ACTIONS_LIMIT;
import static org.influxdb.BatchOptions.DEFAULT_BATCH_INTERVAL_DURATION;
import static org.influxdb.BatchOptions.DEFAULT_BUFFER_LIMIT;
import static org.influxdb.BatchOptions.DEFAULT_JITTER_INTERVAL_DURATION;

/**
 * BatchOptions are used to configure batching of individual data point writes into InfluxDB.
 *
 * @author Jakub Bednar (bednar@github) (04/06/2018 14:09)
 * @since 3.0.0
 */
@ThreadSafe
public final class BatchOptionsReactive {

    /**
     * Default configuration with values that are consistent with Telegraf.
     */
    public static final BatchOptionsReactive DEFAULTS = BatchOptionsReactive.builder().build();

    /**
     * Disabled batching.
     */
    public static final BatchOptionsReactive DISABLED = BatchOptionsReactive.disabled().build();

    private final int batchSize;
    private final int flushInterval;
    private final int jitterInterval;
    private final int retryInterval;
    private final int bufferLimit;
    private final Scheduler writeScheduler;
    private final BackpressureOverflowStrategy backpressureStrategy;

    /**
     * @return the number of data point to collect in batch
     * @see BatchOptionsReactive.Builder#batchSize(int)
     * @since 3.0.0
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * @return the time to wait at most (milliseconds)
     * @see BatchOptionsReactive.Builder#flushInterval(int) (int)
     * @since 3.0.0
     */
    public int getFlushInterval() {
        return flushInterval;
    }

    /**
     * @return batch flush interval jitter value (milliseconds)
     * @see BatchOptionsReactive.Builder#jitterInterval(int)
     * @since 3.0.0
     */
    public int getJitterInterval() {
        return jitterInterval;
    }


    /**
     * @return the time to wait before retry unsuccessful write (milliseconds)
     * @see BatchOptionsReactive.Builder#retryInterval(int)
     * @since 3.0.0
     */
    public int getRetryInterval() {
        return retryInterval;
    }

    /**
     * @return Maximum number of points stored in the retry buffer.
     * @see BatchOptionsReactive.Builder#bufferLimit(int)
     * @since 3.0.0
     */
    public int getBufferLimit() {
        return bufferLimit;
    }

    /**
     * @return Set the scheduler which is used for write data points.
     * @see BatchOptionsReactive.Builder#writeScheduler(Scheduler)
     */
    @Nonnull
    public Scheduler getWriteScheduler() {
        return writeScheduler;
    }

    /**
     * @return the strategy to deal with buffer overflow when using onBackpressureBuffer
     * @see BatchOptionsReactive.Builder#backpressureStrategy(BackpressureOverflowStrategy)
     * @since 3.0.0
     */
    @Nonnull
    public BackpressureOverflowStrategy getBackpressureStrategy() {
        return backpressureStrategy;
    }

    private BatchOptionsReactive(@Nonnull final Builder builder) {

        Objects.requireNonNull(builder, "BatchOptionsReactive.Builder is required");

        batchSize = builder.batchSize;
        flushInterval = builder.flushInterval;
        jitterInterval = builder.jitterInterval;
        retryInterval = builder.retryInterval;
        bufferLimit = builder.bufferLimit;
        writeScheduler = builder.writeScheduler;
        backpressureStrategy = builder.backpressureStrategy;
    }

    /**
     * Creates a builder instance.
     *
     * @return a builder
     * @since 3.0.0
     */
    @Nonnull
    public static BatchOptionsReactive.Builder builder() {
        return new BatchOptionsReactive.Builder();
    }

    /**
     * Creates a builder instance with disabled batching. The {@link BatchOptionsReactive#getBatchSize()} is set to 1
     * and {@link BatchOptionsReactive#getWriteScheduler()} is set to {@link Schedulers#io()}.
     *
     * @return a builder
     * @since 3.0.0
     */
    @Nonnull
    public static BatchOptionsReactive.Builder disabled() {
        return BatchOptionsReactive.builder().batchSize(1).writeScheduler(Schedulers.io());
    }

    /**
     * A builder for {@code BatchOptionsReactive}.
     *
     * @since 3.0.0
     */
    @NotThreadSafe
    public static class Builder {

        private int batchSize = DEFAULT_BATCH_ACTIONS_LIMIT;
        private int flushInterval = DEFAULT_BATCH_INTERVAL_DURATION;
        private int jitterInterval = DEFAULT_JITTER_INTERVAL_DURATION;
        private int retryInterval = DEFAULT_BATCH_INTERVAL_DURATION;
        private int bufferLimit = DEFAULT_BUFFER_LIMIT;
        private Scheduler writeScheduler = Schedulers.trampoline();
        private BackpressureOverflowStrategy backpressureStrategy = BackpressureOverflowStrategy.DROP_OLDEST;

        /**
         * Set the number of data point to collect in batch.
         *
         * @param batchSize the number of data point to collect in batch
         * @return {@code this}
         * @since 3.0.0
         */
        @Nonnull
        public Builder batchSize(final int batchSize) {
            Preconditions.checkPositiveNumber(batchSize, "batchSize");
            this.batchSize = batchSize;
            return this;
        }

        /**
         * Set the time to wait at most (milliseconds).
         *
         * @param flushInterval the time to wait at most (milliseconds).
         * @return {@code this}
         * @since 3.0.0
         */
        @Nonnull
        public Builder flushInterval(final int flushInterval) {
            Preconditions.checkPositiveNumber(flushInterval, "flushInterval");
            this.flushInterval = flushInterval;
            return this;
        }

        /**
         * Jitters the batch flush interval by a random amount. This is primarily to avoid
         * large write spikes for users running a large number of client instances.
         * ie, a jitter of 5s and flush duration 10s means flushes will happen every 10-15s.
         *
         * @param jitterInterval (milliseconds)
         * @return {@code this}
         * @since 3.0.0
         */
        @Nonnull
        public Builder jitterInterval(final int jitterInterval) {
            Preconditions.checkNotNegativeNumber(jitterInterval, "jitterInterval");
            this.jitterInterval = jitterInterval;
            return this;
        }

        /**
         * Set the the time to wait before retry unsuccessful write (milliseconds).
         *
         * @param retryInterval the time to wait before retry unsuccessful write
         * @return {@code this}
         * @since 3.0.0
         */
        @Nonnull
        public Builder retryInterval(final int retryInterval) {
            Preconditions.checkPositiveNumber(retryInterval, "retryInterval");
            this.retryInterval = retryInterval;
            return this;
        }

        /**
         * The client maintains a buffer for failed writes so that the writes will be retried later on. This may
         * help to overcome temporary network problems or InfluxDB load spikes.
         * When the buffer is full and new points are written, oldest entries in the buffer are lost.
         * <p>
         * To disable this feature set buffer limit to a value smaller than {@link BatchOptions#getActions}
         *
         * @param bufferLimit maximum number of points stored in the retry buffer
         * @return {@code this}
         * @since 3.0.0
         */
        @Nonnull
        public Builder bufferLimit(final int bufferLimit) {
            Preconditions.checkNotNegativeNumber(bufferLimit, "bufferLimit");
            this.bufferLimit = bufferLimit;
            return this;
        }

        /**
         * Set the scheduler which is used for write data points. It is useful for disabling batch writes or
         * for tuning the performance. Default value is {@link Schedulers#trampoline()}.
         *
         * @param writeScheduler the scheduler which is used for write data points.
         * @return {@code this}
         * @since 3.0.0
         */
        @Nonnull
        public Builder writeScheduler(@Nonnull final Scheduler writeScheduler) {

            Objects.requireNonNull(writeScheduler, "Write scheduler is required");

            this.writeScheduler = writeScheduler;
            return this;
        }

        /**
         * Set the strategy to deal with buffer overflow when using onBackpressureBuffer.
         *
         * @param backpressureStrategy the strategy to deal with buffer overflow when using onBackpressureBuffer.
         *                             Default {@link BackpressureOverflowStrategy#DROP_OLDEST};
         * @return {@code this}
         * @since 3.0.0
         */
        @Nonnull
        public Builder backpressureStrategy(@Nonnull final BackpressureOverflowStrategy backpressureStrategy) {
            Objects.requireNonNull(backpressureStrategy, "Backpressure Overflow Strategy is required");
            this.backpressureStrategy = backpressureStrategy;
            return this;
        }

        /**
         * Build an instance of BatchOptionsReactive.
         *
         * @return {@code BatchOptionsReactive}
         */
        @Nonnull
        public BatchOptionsReactive build() {


            return new BatchOptionsReactive(this);
        }
    }
}
