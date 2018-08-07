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

import io.bonitoo.influxdb.reactive.options.BatchOptionsReactive;
import io.bonitoo.influxdb.reactive.options.InfluxDBOptions;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * @author Jakub Bednar (bednar@github) (12/06/2018 12:54)
 * @since 3.0.0
 */
@RunWith(JUnitPlatform.class)
class InfluxDBReactiveFactoryTest {

    @Test
    void optionsRequired() {

        Assertions.assertThatThrownBy(() -> InfluxDBReactiveFactory.connect(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("InfluxDBOptions is required");
    }

    @Test
    void batchOptionsRequired() {

        InfluxDBOptions options = InfluxDBOptions.builder()
                .url("http://172.17.0.2:8086")
                .username("root")
                .password("root")
                .database("reactive_measurements")
                .build();

        Assertions.assertThatThrownBy(() -> InfluxDBReactiveFactory.connect(options, (BatchOptionsReactive) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("BatchOptionsReactive is required");
    }

    @Test
    void success() {

        InfluxDBOptions options = InfluxDBOptions.builder()
                .url("http://172.17.0.2:8086")
                .username("root")
                .password("root")
                .database("reactive_measurements")
                .build();

        InfluxDBReactive influxDBReactive = InfluxDBReactiveFactory.connect(options);
        Assertions.assertThat(influxDBReactive).isNotNull();
    }
}