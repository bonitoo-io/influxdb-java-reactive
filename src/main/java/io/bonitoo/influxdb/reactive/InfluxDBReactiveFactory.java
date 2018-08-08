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

import java.util.Objects;
import javax.annotation.Nonnull;

import io.bonitoo.influxdb.reactive.impl.InfluxDBReactiveImpl;
import io.bonitoo.influxdb.reactive.options.BatchOptionsReactive;
import io.bonitoo.influxdb.reactive.options.InfluxDBOptions;

/**
 * The Factory that create a reactive instance of a InfluxDB client.
 *
 * @author Jakub Bednar (bednar@github) (12/06/2018 10:32)
 * @since 1.0.0
 */
public final class InfluxDBReactiveFactory {

    private InfluxDBReactiveFactory() {
    }

    /**
     * Create a instance of the InfluxDB reactive client.
     *
     * @param options the connection configuration
     * @return 3.0.0
     */
    @Nonnull
    public static InfluxDBReactive connect(@Nonnull final InfluxDBOptions options) {

        Objects.requireNonNull(options, "InfluxDBOptions is required");

        return connect(options, BatchOptionsReactive.DEFAULTS);
    }

    /**
     * Create a instance of the InfluxDB reactive client.
     *
     * @param options      the connection configuration
     * @param batchOptions the batch configuration
     * @return 3.0.0
     */
    @Nonnull
    public static InfluxDBReactive connect(@Nonnull final InfluxDBOptions options,
                                           @Nonnull final BatchOptionsReactive batchOptions) {

        Objects.requireNonNull(options, "InfluxDBOptions is required");
        Objects.requireNonNull(batchOptions, "BatchOptionsReactive is required");

        return new InfluxDBReactiveImpl(options, batchOptions);
    }
}
