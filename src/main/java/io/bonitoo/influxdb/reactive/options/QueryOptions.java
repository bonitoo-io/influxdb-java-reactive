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
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

import org.influxdb.InfluxDB;
import org.influxdb.impl.Preconditions;

/**
 * QueryOptions are used to configure query data from the InfluxDB.
 *
 * @author Jakub Bednar (bednar@github) (11/06/2018 14:02)
 * @since 3.0.0
 */
@ThreadSafe
public final class QueryOptions {

    private static final int DEFAULT_CHUNK_SIZE = 10_000;

    private final int chunkSize;
    private final TimeUnit precision;

    /**
     * Default configuration: chunk_size = 10_000.
     */
    public static final QueryOptions DEFAULTS = QueryOptions.builder().build();

    private QueryOptions(@Nonnull final Builder builder) {

        Objects.requireNonNull(builder, "QueryOptions.Builder is required");

        chunkSize = builder.chunkSize;
        precision = builder.precision;
    }

    /**
     * @return the number of QueryResults to process in one chunk.
     * @see QueryOptions.Builder#chunkSize(int)
     * @since 3.0.0
     */
    public int getChunkSize() {
        return chunkSize;
    }

    /**
     * @return the time unit of the results.
     * @see QueryOptions.Builder#precision(TimeUnit) (int)
     * @since 3.0.0
     */
    @Nonnull
    public TimeUnit getPrecision() {
        return precision;
    }

    /**
     * Creates a builder instance.
     *
     * @return a builder
     * @since 3.0.0
     */
    @Nonnull
    public static QueryOptions.Builder builder() {
        return new QueryOptions.Builder();
    }

    /**
     * A builder for {@code QueryOptions}.
     *
     * @since 3.0.0
     */
    @NotThreadSafe
    public static class Builder {

        private int chunkSize = DEFAULT_CHUNK_SIZE;
        private TimeUnit precision = InfluxDB.DEFAULT_PRECISION;

        /**
         * Set the number of QueryResults to process in one chunk.
         *
         * @param chunkSize the number of QueryResults to process in one chunk.
         * @return {@code this}
         * @since 3.0.0
         */
        @Nonnull
        public Builder chunkSize(final int chunkSize) {
            Preconditions.checkPositiveNumber(chunkSize, "chunkSize");
            this.chunkSize = chunkSize;
            return this;
        }

        /**
         * Set the time unit of the results.
         *
         * @param timeUnit the time unit of the results.
         * @return {@code this}
         * @since 3.0.0
         */
        @Nonnull
        public Builder precision(@Nonnull final TimeUnit timeUnit) {

            Objects.requireNonNull(timeUnit, "TimeUnit is required");

            this.precision = timeUnit;
            return this;
        }

        /**
         * Build an instance of QueryOptions.
         *
         * @return {@code QueryOptions}
         */
        @Nonnull
        public QueryOptions build() {

            return new QueryOptions(this);
        }
    }
}
