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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The event is published when is backpressure applied.
 *
 * @author Jakub Bednar (bednar@github) (14/06/2018 12:12)
 * @since 3.0.0
 * @see io.reactivex.Flowable#onBackpressureBuffer(int, boolean, boolean, io.reactivex.functions.Action)
 */
public class BackpressureEvent extends AbstractInfluxEvent {

    private static final Logger LOG = Logger.getLogger(BackpressureEvent.class.getName());

    @Override
    public void logEvent() {
        LOG.log(Level.WARNING, "Backpressure applied, try increase BatchOptionsReactive.bufferLimit");
    }
}
