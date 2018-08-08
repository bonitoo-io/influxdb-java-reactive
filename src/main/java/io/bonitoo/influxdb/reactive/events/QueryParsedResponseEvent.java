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

import okio.BufferedSource;
import org.influxdb.dto.QueryResult;

/**
 * The event is published when is parsed streamed response to query result.
 *
 * @author Jakub Bednar (bednar@github) (14/06/2018 11:46)
 * @since 1.0.0
 */
public class QueryParsedResponseEvent extends AbstractInfluxEvent {

    private static final Logger LOG = Logger.getLogger(QueryParsedResponseEvent.class.getName());

    private final BufferedSource bufferedSource;
    private final QueryResult queryResult;

    public QueryParsedResponseEvent(@Nonnull final BufferedSource bufferedSource,
                                    @Nonnull final QueryResult queryResult) {

        Objects.requireNonNull(bufferedSource, "BufferedSource is not null");
        Objects.requireNonNull(queryResult, "QueryResult is not null");

        this.bufferedSource = bufferedSource;
        this.queryResult = queryResult;
    }

    /**
     * @return chunked response
     */
    @Nonnull
    public BufferedSource getBufferedSource() {
        return bufferedSource;
    }

    /**
     * @return parsed response
     */
    @Nonnull
    public QueryResult getQueryResult() {
        return queryResult;
    }

    @Override
    public void logEvent() {

        LOG.log(Level.FINEST, "Chunk response parsed to {0}", queryResult);
    }
}
