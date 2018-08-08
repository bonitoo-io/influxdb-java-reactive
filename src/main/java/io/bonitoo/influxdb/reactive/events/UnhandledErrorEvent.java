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

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/**
 * The event is published when occurs a unhandled exception.
 *
 * @author Jakub Bednar (bednar@github) (14/06/2018 10:19)
 * @since 1.0.0
 */
public class UnhandledErrorEvent extends AbstractInfluxEvent {

    private static final Logger LOG = Logger.getLogger(UnhandledErrorEvent.class.getName());

    private final Throwable throwable;

    public UnhandledErrorEvent(@Nonnull final Throwable throwable) {

        Objects.requireNonNull(throwable, "Throwable is required");

        this.throwable = throwable;
    }

    /**
     * @return the exception that was throw
     */
    @Nonnull
    public Throwable getThrowable() {
        return throwable;
    }

    @Override
    public void logEvent() {
        LOG.log(Level.SEVERE, "Unexpected error", throwable);
    }
}
