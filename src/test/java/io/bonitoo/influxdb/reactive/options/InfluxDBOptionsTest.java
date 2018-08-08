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

import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import org.assertj.core.api.Assertions;
import org.influxdb.InfluxDB;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * @author Jakub Bednar (bednar@github) (01/06/2018 08:18)
 */
@RunWith(JUnitPlatform.class)
class InfluxDBOptionsTest {

    @Test
    void optionsDefault()
    {
        InfluxDBOptions options = InfluxDBOptions.builder().url("http://influxdb:8086").build();

        Assertions.assertThat(options.getUrl()).isEqualTo("http://influxdb:8086");
        Assertions.assertThat(options.getPassword()).isNull();
        Assertions.assertThat(options.getUsername()).isNull();
        Assertions.assertThat(options.getDatabase()).isNull();
        Assertions.assertThat(options.getRetentionPolicy()).isEqualTo("autogen");
        Assertions.assertThat(options.getConsistencyLevel()).isEqualTo(InfluxDB.ConsistencyLevel.ONE);
        Assertions.assertThat(options.getPrecision()).isEqualTo(TimeUnit.NANOSECONDS);
        Assertions.assertThat(options.getResponseFormat()).isEqualTo(InfluxDB.ResponseFormat.JSON);
        Assertions.assertThat(options.getMediaType()).isEqualTo(MediaType.parse("text/plain; charset=utf-8"));
        Assertions.assertThat(options.getOkHttpClient()).isNotNull();
    }

    @Test
    void optionsFull() {

        OkHttpClient.Builder okBuilder = new OkHttpClient.Builder();

        InfluxDBOptions options = InfluxDBOptions.builder()
                .url("http://influxdb:8086")
                .username("admin")
                .password("password")
                .database("weather")
                .retentionPolicy("short-policy")
                .consistencyLevel(InfluxDB.ConsistencyLevel.ALL)
                .precision(TimeUnit.SECONDS)
                .responseFormat(InfluxDB.ResponseFormat.MSGPACK)
                .mediaType(MediaType.parse("text/plain; charset=US-ASCII"))
                .okHttpClient(okBuilder)
                .build();

        Assertions.assertThat(options.getUrl()).isEqualTo("http://influxdb:8086");
        Assertions.assertThat(options.getUsername()).isEqualTo("admin");
        Assertions.assertThat(options.getPassword()).isEqualTo("password");
        Assertions.assertThat(options.getDatabase()).isEqualTo("weather");
        Assertions.assertThat(options.getRetentionPolicy()).isEqualTo("short-policy");
        Assertions.assertThat(options.getConsistencyLevel()).isEqualTo(InfluxDB.ConsistencyLevel.ALL);
        Assertions.assertThat(options.getPrecision()).isEqualTo(TimeUnit.SECONDS);
        Assertions.assertThat(options.getMediaType()).isEqualTo(MediaType.parse("text/plain; charset=US-ASCII"));
        Assertions.assertThat(options.getOkHttpClient()).isEqualTo(okBuilder);
    }

    @Test
    void urlIsNotEmptyString() {

        InfluxDBOptions.Builder builder = InfluxDBOptions.builder();

        Assertions.assertThatThrownBy(() -> builder.url(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void urlIsRequired() {

        InfluxDBOptions.Builder builder = InfluxDBOptions.builder();

        Assertions.assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void retentionPolicyNull(){

        InfluxDBOptions options = InfluxDBOptions.builder()
                .url("http://influxdb:8086")
                .retentionPolicy(null)
                .build();

        Assertions.assertThat(options.getRetentionPolicy()).isEqualTo("autogen");
    }

    @Test
    void consistencyLevelNull(){

        InfluxDBOptions options = InfluxDBOptions.builder()
                .url("http://influxdb:8086")
                .consistencyLevel(null)
                .build();

        Assertions.assertThat(options.getConsistencyLevel())
                .isEqualTo(InfluxDB.ConsistencyLevel.ONE);
    }

    @Test
    void precisionNull() {

        InfluxDBOptions options = InfluxDBOptions.builder()
                .url("http://influxdb:8086")
                .precision(null)
                .build();

        Assertions.assertThat(options.getPrecision()).isEqualTo(TimeUnit.NANOSECONDS);
    }

    @Test
    void responseFormatNull() {

        InfluxDBOptions options = InfluxDBOptions.builder()
                .url("http://influxdb:8086")
                .responseFormat(null)
                .build();

        Assertions.assertThat(options.getResponseFormat()).isEqualTo(InfluxDB.ResponseFormat.JSON);
    }

    @Test
    void encodingNull() {

        InfluxDBOptions options = InfluxDBOptions.builder()
                .url("http://influxdb:8086")
                .mediaType(null)
                .build();

        Assertions.assertThat(options.getMediaType())
                .isEqualTo(MediaType.parse("text/plain; charset=utf-8"));
    }

    @Test
    void okHttpClientIsRequired() {

        InfluxDBOptions.Builder builder = InfluxDBOptions.builder().url("http://influxdb:8086");

        //noinspection ConstantConditions
        Assertions.assertThatThrownBy(() -> builder.okHttpClient(null))
                .isInstanceOf(NullPointerException.class);
    }
}