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

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

import io.bonitoo.influxdb.reactive.options.WriteOptions;
import org.influxdb.InfluxDBMapperException;
import org.influxdb.annotation.Column;
import org.influxdb.dto.Point;
import org.influxdb.impl.InfluxDBResultMapper;

/**
 * @author Jakub Bednar (bednar@github) (18/06/2018 14:57)
 */
final class MeasurementData<M> extends AbstractData<M> {

    private static final Logger LOG = Logger.getLogger(MeasurementData.class.getName());

    private M measurement;

    MeasurementData(@Nonnull final M measurement, @Nonnull final WriteOptions writeOptions) {
        super(writeOptions);
        this.measurement = measurement;
    }

    @Nonnull
    @Override
    M getData() {
        return measurement;
    }

    @Nonnull
    @Override
    String lineProtocol() {

        String lineProtocol = new InfluxDBPointMapper()
                .toPoint(measurement, writeOptions.getPrecision())
                .lineProtocol(writeOptions.getPrecision());

        Object[] params = {measurement, lineProtocol};
        LOG.log(Level.FINEST, "Map measurement: {0} to InfluxDB Line Protocol: {1}", params);

        return lineProtocol;
    }

    private static class InfluxDBPointMapper extends InfluxDBResultMapper {

        private static final Logger LOG = Logger.getLogger(InfluxDBPointMapper.class.getName());

        /**
         * Map the {@code measurement} to {@link Point}.
         *
         * @param measurement for mapping to {@link Point}
         * @param precision   the precision to use for store {@code time} of {@link Point}
         * @param <M>         type of measurement
         * @return a {@link Point} created from {@code measurement}
         * @throws InfluxDBMapperException if the {@code measurement} can't be mapped to {@link Point}
         */
        @Nonnull
        public <M> Point toPoint(@Nonnull final M measurement, @Nonnull final TimeUnit precision)
                throws InfluxDBMapperException {

            Objects.requireNonNull(measurement, "Measurement is required");
            Objects.requireNonNull(precision, "TimeUnit precision is required");

            Class<?> measurementType = measurement.getClass();
            cacheMeasurementClass(measurementType);

            Point.Builder builder = Point.measurement(getMeasurementName(measurementType));

            getColNameAndFieldMap(measurementType).forEach((name, field) -> {

                Column column = field.getAnnotation(Column.class);

                Object value;
                try {
                    field.setAccessible(true);
                    value = field.get(measurement);
                } catch (IllegalAccessException e) {

                    String msg = String.format("Field '%s' of '%s' is not accessible", field.getName(), measurement);

                    throw new InfluxDBMapperException(msg, e);
                }

                if (value == null) {
                    Object[] params = {field.getName(), measurement};
                    LOG.log(Level.FINEST, "Field {0} of {1} has null value", params);
                    return;
                }

                Class<?> fieldType = field.getType();
                if (column.tag()) {
                    builder.tag(column.name(), value.toString());
                } else if (isNumber(fieldType)) {
                    builder.addField(column.name(), (Number) value);
                } else if (Boolean.class.isAssignableFrom(fieldType) || boolean.class.isAssignableFrom(fieldType)) {
                    builder.addField(column.name(), (Boolean) value);
                } else if (String.class.isAssignableFrom(fieldType)) {
                    builder.addField(column.name(), (String) value);
                } else if (Instant.class.isAssignableFrom(fieldType)) {
                    Instant instant = (Instant) value;
                    long timeToSet = precision.convert(instant.toEpochMilli(), TimeUnit.MILLISECONDS);
                    builder.time(timeToSet, precision);
                }
            });

            Point point = builder.build();
            LOG.log(Level.FINEST, "Mapped measurement: {0} to Point: {1}", new Object[]{measurement, point});

            return point;
        }

        private boolean isNumber(@Nonnull final Class<?> fieldType) {
            return Number.class.isAssignableFrom(fieldType)
                    || double.class.isAssignableFrom(fieldType)
                    || long.class.isAssignableFrom(fieldType)
                    || int.class.isAssignableFrom(fieldType);
        }
    }
}
