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
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * @author Jakub Bednar (bednar@github) (11/06/2018 14:13)
 */
@RunWith(JUnitPlatform.class)
class QueryOptionsTest {

    @Test
    void defaults() {
        QueryOptions queryOptions = QueryOptions.builder().build();

        Assertions.assertThat(queryOptions.getChunkSize()).isEqualTo(10_000);
        Assertions.assertThat(queryOptions.getPrecision()).isEqualTo(TimeUnit.NANOSECONDS);
    }

    @Test
    void chunkSizePositive() {

        QueryOptions.Builder queryOptions = QueryOptions.builder();

        Assertions.assertThatThrownBy(() -> queryOptions.chunkSize(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expecting a positive number for chunkSize");
    }
}
