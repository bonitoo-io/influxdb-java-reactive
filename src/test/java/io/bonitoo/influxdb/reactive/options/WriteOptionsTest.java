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

import org.assertj.core.api.Assertions;
import org.influxdb.InfluxDB;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * @author Jakub Bednar (bednar@github) (14/06/2018 15:56)
 */
@RunWith(JUnitPlatform.class)
class WriteOptionsTest {

    @Test
    void defaults() {

        WriteOptions writeOptions = WriteOptions.builder().database("my_db").build();

        Assertions.assertThat(writeOptions.getDatabase()).isEqualTo("my_db");
        Assertions.assertThat(writeOptions.getRetentionPolicy()).isEqualTo("autogen");
        Assertions.assertThat(writeOptions.getConsistencyLevel()).isEqualTo(InfluxDB.ConsistencyLevel.ONE);
        Assertions.assertThat(writeOptions.getPrecision()).isEqualTo(TimeUnit.NANOSECONDS);
        Assertions.assertThat(writeOptions.isUdpEnable()).isFalse();
        Assertions.assertThat(writeOptions.getUdpPort()).isEqualTo(-1);
    }

    @Test
    void databaseRequired() {

        WriteOptions.Builder builder = WriteOptions.builder();

        Assertions.assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expecting a non-empty string for database");
    }

    @Test
    void retentionPolicy() {

        WriteOptions writeOptions = WriteOptions.builder().database("my_db").retentionPolicy("my_policy").build();

        Assertions.assertThat(writeOptions.getRetentionPolicy()).isEqualTo("my_policy");
    }

    @Test
    void retentionPolicyRequired() {

        WriteOptions.Builder builder = WriteOptions.builder().database("my_db");

        Assertions.assertThatThrownBy(() -> builder.retentionPolicy(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expecting a non-empty string for retentionPolicy");
    }

    @Test
    void consistencyLevel() {

        WriteOptions writeOptions = WriteOptions.builder().database("my_db")
                .consistencyLevel(InfluxDB.ConsistencyLevel.QUORUM).build();

        Assertions.assertThat(writeOptions.getConsistencyLevel()).isEqualTo(InfluxDB.ConsistencyLevel.QUORUM);
    }

    @Test
    void consistencyLevelRequired() {

        WriteOptions.Builder builder = WriteOptions.builder().database("my_db");

        Assertions.assertThatThrownBy(() -> builder.consistencyLevel(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("InfluxDB.ConsistencyLevel is required");
    }

    @Test
    void precision() {

        WriteOptions writeOptions = WriteOptions.builder().database("my_db")
                .precision(TimeUnit.HOURS).build();

        Assertions.assertThat(writeOptions.getPrecision()).isEqualTo(TimeUnit.HOURS);
    }

    @Test
    void precisionRequired() {

        WriteOptions.Builder builder = WriteOptions.builder().database("my_db");

        Assertions.assertThatThrownBy(() -> builder.precision(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("TimeUnit precision is required");
    }

    @Test
    void equals() {
        WriteOptions writeOptions1 = WriteOptions.builder().database("my_db").build();
        WriteOptions writeOptions2 = WriteOptions.builder().database("my_db").build();

        Assertions.assertThat(writeOptions1).isEqualTo(writeOptions2);
    }

    @Test
    void udpEnable() {

        WriteOptions writeOptions = WriteOptions.builder()
                .database("my_db")
                .udp(true, 12_345)
                .build();

        Assertions.assertThat(writeOptions.isUdpEnable()).isTrue();
        Assertions.assertThat(writeOptions.getUdpPort()).isEqualTo(12_345);
    }

    @Test
    void udpEnableWithoutDatabase() {

        WriteOptions.builder()
                .udp(true, 12_345)
                .build();
    }

    @Test
    void equalsUdpPort() {
        WriteOptions writeOptions1 = WriteOptions.builder()
                .database("my_db")
                .udp(true, 1)
                .build();

        WriteOptions writeOptions2 = WriteOptions.builder()
                .database("my_db")
                .udp(true, 2)
                .build();

        Assertions.assertThat(writeOptions1).isNotEqualTo(writeOptions2);
    }
}