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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

import io.bonitoo.influxdb.reactive.options.WriteOptions;

/**
 * The event is published when the data was written through UDP to InfluxDB server.
 *
 * @author Jakub Bednar (bednar@github) (20/06/2018 07:56)
 * @since 1.0.0
 */
public class WriteUDPEvent extends AbstractWriteEvent {

    private static final Logger LOG = Logger.getLogger(WriteUDPEvent.class.getName());

    public WriteUDPEvent(@Nonnull final List<?> points,
                         @Nonnull final WriteOptions writeOptions) {

        super(points, writeOptions);
    }

    @Override
    public void logEvent() {
        LOG.log(Level.FINEST, "The data was written through UDP to InfluxDB.");
    }
}
