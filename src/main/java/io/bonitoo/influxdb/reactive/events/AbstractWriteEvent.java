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
package io.bonitoo.influxdb.reactive.events;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;

import io.bonitoo.influxdb.reactive.options.WriteOptions;

/**
 * @author Jakub Bednar (bednar@github) (18/06/2018 13:38)
 * @since 3.0.0
 */
public abstract class AbstractWriteEvent extends AbstractInfluxEvent {

    private final List<?> points;
    private final WriteOptions writeOptions;

    AbstractWriteEvent(@Nonnull final List<?> points,
                       @Nonnull final WriteOptions writeOptions) {

        Objects.requireNonNull(points, "Points are required");
        Objects.requireNonNull(writeOptions, "WriteOptions are required");

        this.points = points;
        this.writeOptions = writeOptions;
    }

    /**
     * @param <D> type of data points: Measurements, Line Protocols, {@link org.influxdb.dto.Point}s
     * @return the points that was sent to InfluxDB
     */
    @Nonnull
    public <D> List<D> getDataPoints() {

        //noinspection unchecked
        return List.class.cast(points);
    }

    /**
     * @return {@code writeOptions} that was used in write
     */
    @Nonnull
    public WriteOptions getWriteOptions() {
        return writeOptions;
    }
}
