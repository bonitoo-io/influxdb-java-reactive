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

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

/**
 * @author Jakub Bednar (bednar@github) (04/06/2018 11:48)
 */
@Measurement(name = "h2o_feet", timeUnit = TimeUnit.NANOSECONDS)
public class H2OFeetMeasurement {

    @Column(name = "location", tag = true)
    private String location;

    @Column(name = "water_level")
    private Double level;

    @Column(name = "level description")
    private String description;

    @Column(name = "time")
    private Instant time;

    public H2OFeetMeasurement() {
    }

    public H2OFeetMeasurement(String location, Double level, String description, @Nullable final Long millis) {
        this.location = location;
        this.level = level;
        this.description = description;
        this.time = millis != null ? Instant.ofEpochMilli(millis) : null;
    }

    public String getLocation() {
        return location;
    }

    public Double getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    public Instant getTime() {
        return time;
    }

    @Nonnull
    public static H2OFeetMeasurement createMeasurement(@Nonnull final Integer index)
    {
        Objects.requireNonNull(index, "Measurement index is required");

        double level = index.doubleValue();
        long time = 1440046800L + index;

        return new H2OFeetMeasurement("coyote_creek", level, "feet " + index, time);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof H2OFeetMeasurement)) return false;
        H2OFeetMeasurement h20Feet = (H2OFeetMeasurement) o;
        return Objects.equals(location, h20Feet.location) &&
                Objects.equals(level, h20Feet.level) &&
                Objects.equals(description, h20Feet.description) &&
                Objects.equals(time, h20Feet.time);
    }

    @Override
    public int hashCode() {

        return Objects.hash(location, level, description, time);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("org.influxdb.reactive.H2OFeetMeasurement{");
        sb.append("location='").append(location).append('\'');
        sb.append(", level=").append(level);
        sb.append(", description='").append(description).append('\'');
        sb.append(", time=").append(time);
        sb.append('}');
        return sb.toString();
    }
}