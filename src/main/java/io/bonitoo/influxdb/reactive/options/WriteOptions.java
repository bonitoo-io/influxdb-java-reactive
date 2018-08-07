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
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

import org.influxdb.InfluxDB;
import org.influxdb.impl.Preconditions;

import static org.influxdb.InfluxDB.DEFAULT_CONSISTENCY_LEVEL;
import static org.influxdb.InfluxDB.DEFAULT_PRECISION;
import static org.influxdb.InfluxDB.DEFAULT_RETENTION_POLICY;

/**
 * WriteOptions are used to configure writes to the InfluxDB.
 *
 * @author Jakub Bednar (bednar@github) (14/06/2018 15:44)
 * @since 3.0.0
 */
@ThreadSafe
public final class WriteOptions {

    private final String database;
    private final String retentionPolicy;
    private final InfluxDB.ConsistencyLevel consistencyLevel;
    private final TimeUnit precision;
    private final boolean udpEnable;
    private final int udpPort;

    private WriteOptions(@Nonnull final Builder builder) {

        Objects.requireNonNull(builder, "WriteOptions.Builder is required");

        database = builder.database;
        retentionPolicy = builder.retentionPolicy;
        consistencyLevel = builder.consistencyLevel;
        precision = builder.precision;

        udpEnable = builder.udpEnable;
        udpPort = builder.udpPort;
    }

    /**
     * @return the name of the database to write. It can be nullable for UDP writes.
     */
    @Nullable
    public String getDatabase() {
        return database;
    }

    /**
     * @return the retentionPolicy to use
     */
    @Nonnull
    public String getRetentionPolicy() {
        return retentionPolicy;
    }

    /**
     * @return the ConsistencyLevel to use
     */
    @Nonnull
    public InfluxDB.ConsistencyLevel getConsistencyLevel() {
        return consistencyLevel;
    }

    /**
     * @return the time precision to use
     */
    @Nonnull
    public TimeUnit getPrecision() {
        return precision;
    }


    /**
     * @return if {@link Boolean#TRUE} than enable write data points through UDP
     * @see WriteOptions.Builder#udp(boolean, int)
     * @since 3.0.0
     */
    public boolean isUdpEnable() {
        return udpEnable;
    }


    /**
     * @return the UDP Port where InfluxDB is listening
     * @see WriteOptions.Builder#udp(boolean, int)
     * @since 3.0.0
     */
    public int getUdpPort() {
        return udpPort;
    }

    /**
     * Creates a builder instance.
     *
     * @return a builder
     * @since 3.0.0
     */
    @Nonnull
    public static WriteOptions.Builder builder() {
        return new WriteOptions.Builder();
    }

    /**
     * A builder for {@code WriteOptions}.
     *
     * @since 3.0.0
     */
    @NotThreadSafe
    public static class Builder {

        private String database;
        private String retentionPolicy = DEFAULT_RETENTION_POLICY;
        private InfluxDB.ConsistencyLevel consistencyLevel = DEFAULT_CONSISTENCY_LEVEL;
        private TimeUnit precision = DEFAULT_PRECISION;
        private boolean udpEnable = false;
        private int udpPort = -1;

        /**
         * Set the name of the database to write.
         *
         * @param database the name of the database to write
         * @return {@code this}
         * @since 3.0.0
         */
        @Nonnull
        public Builder database(@Nonnull final String database) {

            Preconditions.checkNonEmptyString(database, "database");

            this.database = database;
            return this;
        }

        /**
         * Set the retentionPolicy to use.
         *
         * @param retentionPolicy the retentionPolicy to use
         * @return {@code this}
         * @since 3.0.0
         */
        @Nonnull
        public Builder retentionPolicy(@Nonnull final String retentionPolicy) {

            Preconditions.checkNonEmptyString(retentionPolicy, "retentionPolicy");

            this.retentionPolicy = retentionPolicy;
            return this;
        }

        /**
         * Set the ConsistencyLevel to use.
         *
         * @param consistencyLevel the ConsistencyLevel to use
         * @return {@code this}
         * @since 3.0.0
         */
        @Nonnull
        public Builder consistencyLevel(@Nonnull final InfluxDB.ConsistencyLevel consistencyLevel) {

            Objects.requireNonNull(consistencyLevel, "InfluxDB.ConsistencyLevel is required");

            this.consistencyLevel = consistencyLevel;
            return this;
        }

        /**
         * Set the time precision to use.
         *
         * @param precision the time precision to use
         * @return {@code this}
         * @since 3.0.0
         */
        @Nonnull
        public Builder precision(@Nonnull final TimeUnit precision) {

            Objects.requireNonNull(precision, "TimeUnit precision is required");

            this.precision = precision;
            return this;
        }

        /**
         * Enable write data through
         * <a href="https://docs.influxdata.com/influxdb/latest/supported_protocols/udp/">UDP</a>.
         * If is enabled UDP than are ignored {@link WriteOptions#getDatabase()},
         * {@link WriteOptions#getConsistencyLevel()}, {@link WriteOptions#getPrecision()}
         * and {@link WriteOptions#getRetentionPolicy()}. Those settings depends on the InfluxDB UDP endpoint.
         *
         * @param enable if {@link Boolean#TRUE} than enable write data points through UDP
         * @param port   the UDP Port where InfluxDB is listening
         * @return {@code this}
         * @since 3.0.0
         */
        @Nonnull
        public Builder udp(final boolean enable, final int port) {
            this.udpEnable = enable;
            this.udpPort = port;
            return this;
        }

        /**
         * Build an instance of WriteOptions.
         *
         * @return {@code WriteOptions}
         */
        @Nonnull
        public WriteOptions build() {

            if (!udpEnable) {
                Preconditions.checkNonEmptyString(database, "database");
            }

            return new WriteOptions(this);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WriteOptions)) {
            return false;
        }
        WriteOptions that = (WriteOptions) o;
        return udpEnable == that.udpEnable
                && udpPort == that.udpPort
                && Objects.equals(database, that.database)
                && Objects.equals(retentionPolicy, that.retentionPolicy)
                && consistencyLevel == that.consistencyLevel
                && precision == that.precision;
    }

    @Override
    public int hashCode() {
        return Objects.hash(database, retentionPolicy, consistencyLevel, precision, udpEnable, udpPort);
    }

    @Override
    public String toString() {
        return "org.influxdb.reactive.options.WriteOptions{"
                + "database='" + database + '\''
                + ", retentionPolicy='" + retentionPolicy + '\''
                + ", consistencyLevel=" + consistencyLevel
                + ", precision=" + precision
                + '}';
    }
}

