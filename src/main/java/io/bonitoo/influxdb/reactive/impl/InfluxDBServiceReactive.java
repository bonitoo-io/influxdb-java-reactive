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

import javax.annotation.Nonnull;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.influxdb.impl.InfluxDBService;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

/**
 * @author Jakub Bednar (bednar@github) (01/06/2018 11:56)
 * @since 1.0.0
 */
public interface InfluxDBServiceReactive {

    @POST("/write")
    @Nonnull
    Completable writePoints(@Query(InfluxDBService.U) String username,
                            @Query(InfluxDBService.P) String password,
                            @Query(InfluxDBService.DB) String database,
                            @Query(InfluxDBService.RP) String retentionPolicy,
                            @Query(InfluxDBService.PRECISION) String precision,
                            @Query(InfluxDBService.CONSISTENCY) String consistency,
                            @Body RequestBody points);

    @Streaming
    @GET("/query?chunked=true")
    @Nonnull
    Observable<ResponseBody> query(@Query(InfluxDBService.U) String username,
                                   @Query(InfluxDBService.P) String password,
                                   @Query(InfluxDBService.DB) String db,
                                   @Query(InfluxDBService.EPOCH) String epoch,
                                   @Query(InfluxDBService.CHUNK_SIZE) int chunkSize,
                                   @Query(value = InfluxDBService.Q, encoded = true) String query,
                                   @Query(value = InfluxDBService.PARAMS, encoded = true) String params);

    @GET("/ping")
    Maybe<Response<ResponseBody>> ping();
}
