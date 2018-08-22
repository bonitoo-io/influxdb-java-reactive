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

import io.bonitoo.influxdb.reactive.InfluxDBReactive;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.impl.Preconditions;


/**
 * Various settings to control the behavior of a {@link io.bonitoo.influxdb.reactive.InfluxDBReactiveFactory}.
 *
 * @author Jakub Bednar (bednar@github) (01/06/2018 07:53)
 * @see InfluxDB
 * @see InfluxDBFactory
 * @since 1.0.0
 */
public final class InfluxDBOptions {

    //TODO "DONE" for message pack

    private String url;

    private String username;
    private String password;

    private String database;
    private String retentionPolicy;
    private InfluxDB.ConsistencyLevel consistencyLevel;

    private TimeUnit precision;

    private InfluxDB.ResponseFormat responseFormat;
    private MediaType mediaType;

    private OkHttpClient.Builder okHttpClient;
    //TODO listeners
//    private List<InfluxDBEventListener> listeners;

    private InfluxDBOptions(@Nonnull final Builder builder) {

        Objects.requireNonNull(builder, "InfluxDBOptions.Builder is required");

        url = builder.url;

        username = builder.username;
        password = builder.password;


        database = builder.database;
        retentionPolicy = builder.retentionPolicy;
        consistencyLevel = builder.consistencyLevel;

        precision = builder.precision;

        responseFormat = builder.responseFormat;
        mediaType = builder.mediaType;

        okHttpClient = builder.okHttpClient;
//        listeners =  Collections.unmodifiableList(builder.listeners);
    }

    /**
     * The url to connect to InfluxDB.
     *
     * @return url
     * @since 1.0.0
     */
    @Nonnull
    public String getUrl() {
        return url;
    }

    /**
     * The username which is used to authorize against the InfluxDB instance.
     *
     * @return username
     * @since 1.0.0
     */
    @Nullable
    public String getUsername() {
        return username;
    }

    /**
     * The password for the username which is used to authorize against the InfluxDB instance.
     *
     * @return password
     * @since 1.0.0
     */
    @Nullable
    public String getPassword() {
        return password;
    }

    /**
     * The database which is used for writing points.
     *
     * @return database
     * @since 1.0.0
     */
    @Nullable
    public String getDatabase() {
        return database;
    }

    /**
     * The retention policy which is used for writing points.
     *
     * @return retention policy
     * @since 1.0.0
     */
    @Nonnull
    public String getRetentionPolicy() {
        return retentionPolicy;
    }

    /**
     * The consistency level which is used for writing points.
     *
     * @return retention policy
     * @since 1.0.0
     */
    @Nonnull
    public InfluxDB.ConsistencyLevel getConsistencyLevel() {
        return consistencyLevel;
    }

    /**
     * The default TimeUnit of the interval.
     *
     * @return time unit
     * @since 1.0.0
     */
    @Nonnull
    public TimeUnit getPrecision() {
        return precision;
    }

    /**
     * The format of HTTP Response body from InfluxDB server.
     *
     * @return response format
     * @since 1.0.0
     */
    @Nonnull
    public InfluxDB.ResponseFormat getResponseFormat() {
        return responseFormat;
    }

    /**
     * The encoding of the point's data.
     *
     * @return media type
     * @since 1.0.0
     */
    @Nonnull
    public MediaType getMediaType() {
        return mediaType;
    }

    /**
     * The HTTP client to use for communication to InfluxDB.
     *
     * @return okHttpClient
     * @since 1.0.0
     */
    @Nonnull
    public OkHttpClient.Builder getOkHttpClient() {
        return okHttpClient;
    }

//    /**
//     * Returns list of listeners registered by this client.
//     * @since 1.0.0
//     * @return unmodifiable list of listeners
//     */
//    @Nonnull
//    public List<InfluxDBEventListener> getListeners() {
//        return listeners;
//    }

    /**
     * Creates a builder instance.
     *
     * @return a builder
     * @since 1.0.0
     */
    @Nonnull
    public static Builder builder() {
        return new Builder();
    }
    /**
     * A builder for {@code InfluxDBOptions}.
     *
     * @since 1.0.0
     */
    @NotThreadSafe
    public static class Builder {

        private String url;

        private String username;
        private String password;

        private String database;

        private String retentionPolicy = InfluxDBReactive.DEFAULT_RETENTION_POLICY;
        private InfluxDB.ConsistencyLevel consistencyLevel = InfluxDBReactive.DEFAULT_CONSISTENCY_LEVEL;
        private TimeUnit precision = InfluxDBReactive.DEFAULT_PRECISION;

        private InfluxDB.ResponseFormat responseFormat = InfluxDB.ResponseFormat.JSON;
        private MediaType mediaType = MediaType.parse("text/plain; charset=utf-8");

        private OkHttpClient.Builder okHttpClient = new OkHttpClient.Builder();
//        private List<InfluxDBEventListener> listeners = new ArrayList<>();

        /**
         * Set the url to connect to InfluxDB.
         *
         * @param url the url to connect to InfluxDB. It must be defined.
         * @return {@code this}
         * @since 1.0.0
         */
        @Nonnull
        public Builder url(@Nonnull final String url) {
            Preconditions.checkNonEmptyString(url, "url");
            this.url = url;
            return this;
        }

        /**
         * Set the username which is used to authorize against the InfluxDB instance.
         *
         * @param username the username which is used to authorize against the InfluxDB instance. It may be null.
         * @return {@code this}
         * @since 1.0.0
         */
        @Nonnull
        public Builder username(@Nullable final String username) {
            this.username = username;
            return this;
        }

        /**
         * Set the password for the username which is used to authorize against the InfluxDB instance.
         *
         * @param password the password for the username which is used to authorize against the InfluxDB
         *                 instance. It may be null.
         * @return {@code this}
         * @since 1.0.0
         */
        @Nonnull
        public Builder password(@Nullable final String password) {
            this.password = password;
            return this;
        }

        /**
         * Set the database which is used for writing points.
         *
         * @param database the database to set.
         * @return {@code this}
         * @since 1.0.0
         */
        @Nonnull
        public Builder database(@Nullable final String database) {
            this.database = database;
            return this;
        }

        /**
         * Set the retention policy which is used for writing points.
         *
         * @param retentionPolicy the retention policy to set. It may be null.
         *                        If null than use default policy "autogen".
         * @return {@code this}
         * @since 1.0.0
         */
        @Nonnull
        public Builder retentionPolicy(@Nullable final String retentionPolicy) {

            if (retentionPolicy != null) {
                this.retentionPolicy = retentionPolicy;
            }
            return this;
        }

        /**
         * Set the consistency level which is used for writing points.
         *
         * @param consistencyLevel the consistency level to set. It may be null.
         *                         If null than use default level {@link InfluxDB.ConsistencyLevel#ONE}.
         * @return {@code this}
         * @since 1.0.0
         */
        @Nonnull
        public Builder consistencyLevel(@Nullable final InfluxDB.ConsistencyLevel consistencyLevel) {

            if (consistencyLevel != null) {
                this.consistencyLevel = consistencyLevel;
            }
            return this;
        }

        /**
         * Set the default TimeUnit of the interval.
         *
         * @param precision the default TimeUnit of the interval. It may be null.
         *                  If null than use default level {@link TimeUnit#NANOSECONDS}.
         * @return {@code this}
         * @since 1.0.0
         */
        @Nonnull
        public Builder precision(@Nullable final TimeUnit precision) {

            if (precision != null) {
                this.precision = precision;
            }
            return this;
        }

        /**
         * Set the Format of HTTP Response body from InfluxDB server.
         *
         * @param responseFormat Format of HTTP Response body from InfluxDB server
         * @return {@code this}
         * @since 1.0.0
         */
        @Nonnull
        public Builder responseFormat(@Nullable final InfluxDB.ResponseFormat responseFormat) {
            if (responseFormat != null) {
                this.responseFormat = responseFormat;
            }

            return this;
        }

        /**
         * Set the content type of HTTP request/response.
         *
         * @param mediaType the content type of HTTP request/response. It may be null.
         *                 If null than use default encoding {@code text/plain; charset=utf-8}.
         * @return {@code this}
         * @since 1.0.0
         */
        @Nonnull
        public Builder mediaType(@Nullable final MediaType mediaType) {
            if (mediaType != null) {
                this.mediaType = mediaType;
            }
            return this;
        }

        /**
         * Set the HTTP client to use for communication to InfluxDB.
         *
         * @param okHttpClient the HTTP client to use.
         * @return {@code this}
         * @since 1.0.0
         */
        @Nonnull
        public Builder okHttpClient(@Nonnull final OkHttpClient.Builder okHttpClient) {
            Objects.requireNonNull(okHttpClient, "OkHttpClient.Builder is required");
            this.okHttpClient = okHttpClient;
            return this;
        }

//        /**
//         * Adds custom listener to listen events from InfluxDB client.
//         */
//        public Builder addListener(@Nonnull final  InfluxDBEventListener eventListener) {
//            this.listeners.add(eventListener);
//            return this;
//        }

        /**
         * Build an instance of InfluxDBOptions.
         *
         * @return {@code InfluxDBOptions}
         */
        @Nonnull
        public InfluxDBOptions build() {

            if (url == null) {
                throw new IllegalStateException("The url to connect to InfluxDB has to be defined.");
            }

            return new InfluxDBOptions(this);
        }
    }
}
