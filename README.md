# influxdb-java-reactive

[![Build Status](https://img.shields.io/circleci/project/github/bonitoo-io/influxdb-java-reactive/master.svg)](https://circleci.com/gh/bonitoo-io/workflows/influxdb-java-reactive/tree/master)
[![codecov](https://codecov.io/gh/bonitoo-io/influxdb-java-reactive/branch/master/graph/badge.svg)](https://codecov.io/gh/bonitoo-io/influxdb-java-reactive)
[![License](https://img.shields.io/github/license/bonitoo-io/influxdb-java-reactive.svg)](https://github.com/bonitoo-io/influxdb-java-reactive/blob/master/LICENSE)
[![Snapshot Version](https://img.shields.io/nexus/s/https/apitea.com/nexus/io.bonitoo.influxdb/influxdb-java-reactive.svg)](https://apitea.com/nexus/content/repositories/bonitoo-snapshot/)
[![GitHub issues](https://img.shields.io/github/issues-raw/bonitoo-io/influxdb-java-reactive.svg)](https://github.com/bonitoo-io/influxdb-java-reactive/issues)
[![GitHub pull requests](https://img.shields.io/github/issues-pr-raw/bonitoo-io/influxdb-java-reactive.svg)](https://github.com/bonitoo-io/influxdb-java-reactive/pulls)

This is the Java Reactive Client library for the InfluxDB.

The reactive client library is based on the RxJava. It's support all features from the [influxdb-java - core library](https://github.com/influxdata/influxdb-java/) in the reactive way.

## Usage

### Factory

The `InfluxDBReactiveFactory` creates the reactive instance of a InfluxDB client. 
The `InfluxDBReactive` client can be configured by two parameters:
- `InfluxDBOptions` -  the configuration of connection to the InfluxDB
- `BatchOptionsReactive` - the configuration of batching

The `InfluxDBReactive` client can be also created with default batching configuration by:
```java
// Connection configuration
InfluxDBOptions options = InfluxDBOptions.builder()
    .url("http://172.17.0.2:8086")
    .username("root")
    .password("root")
    .database("reactive_measurements")
    .build();

// Reactive client
InfluxDBReactive influxDBReactive = InfluxDBReactiveFactory.connect(options);

...

influxDBReactive.close();
```
### Events
The `InfluxDBReactive` produces events that allow user to be notified and react to this events:

- `WriteSuccessEvent` - published when arrived the success response from InfluxDB server
- `WriteErrorEvent` - published when arrived the error response from InfluxDB server
- `WritePartialEvent` - published when arrived the partial error response from InfluxDB server
- `WriteUDPEvent` - published when the data was written through UDP to InfluxDB server
- `QueryParsedResponseEvent` -  published when is parsed streamed response to query result
- `BackpressureEvent` -  published when is backpressure applied
- `UnhandledErrorEvent` -  published when occurs a unhandled exception

#### Examples

##### Handle the Success write
```java
InfluxDBReactive influxDBReactive = InfluxDBReactiveFactory.connect(options);

influxDBReactive.listenEvents(WriteSuccessEvent.class).subscribe(event -> {

    List<Point> points = event.getPoints();

    // handle success
    ...
});
```
##### Handle the Error Write
```java
InfluxDBReactive influxDBReactive = InfluxDBReactiveFactory.connect(options);

influxDBReactive.listenEvents(WriteErrorEvent.class).subscribe(event -> {
            
    InfluxDBException exception = event.getException();
    List<Point> points = event.getPoints();

    // handle error
    ...
});
```
    
### Writes

The writes can be configured by `WriteOptions` and are processed in batches which are configurable by `BatchOptionsReactive`.
It's use the same **Retry on error** strategy as non reactive client. 

The `InfluxDBReactive` supports write data points to InfluxDB as POJO, `org.influxdb.dto.Point` or directly in [InfluxDB Line Protocol](https://docs.influxdata.com/influxdb/latest/write_protocols/line_protocol_tutorial/).

#### Write configuration
- `database` - the name of the database to write
- `retentionPolicy` - the Retention Policy to use
- `consistencyLevel` - the ConsistencyLevel to use
- `precision` - the time precision to use
- `udp` 
    - `enable` - enable write data through [UDP](https://docs.influxdata.com/influxdb/latest/supported_protocols/udp/)
    - `port` - the UDP Port where InfluxDB is listening

```java
WriteOptions writeOptions = WriteOptions.builder()
    .database("reactive_measurements")
    .retentionPolicy("my_policy")
    .consistencyLevel(InfluxDB.ConsistencyLevel.QUORUM)
    .precision(TimeUnit.MINUTES)
    .build();

influxDBReactive.writeMeasurements(measurements, writeOptions);
```
The writes can be also used with default configuration by:
```java
influxDBReactive.writeMeasurements(measurements);
```

#### Batching configuration
- `batchSize` - the number of data point to collect in batch
- `flushInterval` - the number of milliseconds before the batch is written 
- `jitterInterval` - the number of milliseconds to increase the batch flush interval by a random amount (see documentation above)
- `retryInterval` - the number of milliseconds to retry unsuccessful write
- `bufferLimit` - the maximum number of unwritten stored points
- `writeScheduler` - the scheduler which is used for write data points (by overriding default settings can be disabled batching)
- `backpressureStrategy` - the strategy to deal with buffer overflow

```java
BatchOptionsReactive batchOptions = BatchOptionsReactive.builder()
    .batchSize(5_000)
    .flushInterval(10_000)
    .jitterInterval(5_000)
    .retryInterval(5_000)
    .bufferLimit(100_000)
    .backpressureStrategy(BackpressureOverflowStrategy.ERROR)
    .build();

// Reactive client
InfluxDBReactive influxDBReactive = InfluxDBReactiveFactory.connect(options, batchOptions);

...

influxDBReactive.close();
```
The BatchOptionsReactive can be also created with default configuration by:
```java
// batchSize = 1_000
// flushInterval = 1_000
// jitterInterval = 0
// retryInterval = 1_000
// bufferLimit = 10_000
// writeScheduler = Schedulers.trampoline()
// backpressureStrategy = DROP_OLDEST
BatchOptions options = BatchOptions.DEFAULTS;
```
There is also configuration for disable batching (data points are written asynchronously one-by-one):
```java
BatchOptionsReactive disabledBatching = BatchOptionsReactive.DISABLED;

// Reactive client
InfluxDBReactive influxDBReactive = InfluxDBReactiveFactory.connect(options, disabledBatching);

...

influxDBReactive.close();
```
#### Backpressure
The backpressure presents the problem of what to do with a growing backlog of unconsumed data points. 
The key feature of backpressure is provides capability to avoid consuming the unexpected amount of system resources.  
This situation is not common and can be caused by several problems: generating too much measurements in short interval,
long term unavailability of the InfluxDB server, network issues. 

The size of backlog is configured by 
`BatchOptionsReactive.bufferLimit` and backpressure strategy by `BatchOptionsReactive.backpressureStrategy`.

##### Strategy how react to backlog overflows
- `DROP_OLDEST` - Drop the oldest data points from the backlog 
- `DROP_LATEST` - Drop the latest data points from the backlog  
- `ERROR` - Signal a exception
- `BLOCK` - (not implemented yet) Wait specified time for space in buffer to become available
  - `timeout` - how long to wait before giving up
  - `unit` - TimeUnit of the timeout

If is used the strategy `DROP_OLDEST` or `DROP_LATEST` there is a possibility to react on backpressure event and slowdown the producing new measurements:
```java
InfluxDBReactive influxDBReactive = InfluxDBReactiveFactory.connect(options, batchOptions);
influxDBReactive.listenEvents(BackpressureEvent.class).subscribe(event -> {
    
    // slowdown producers
    ...
});
```

#### Examples

##### Write POJO
```java
CpuLoad cpuLoad = new CpuLoad();
cpuLoad.host = "server02";
cpuLoad.value = 0.67D;

influxDBReactive.writeMeasurement(cpuLoad);
```

##### Write Point
```java
Point point = Point.measurement("h2o_feet")
    .tag("location", "coyote_creek")
    .addField("water_level", 2.927)
    .addField("level description", "below 3 feet")
    .time(1440046800, TimeUnit.NANOSECONDS)
    .build();

influxDBReactive.writePoint(point);
```

##### Write InfluxDB Line Protocol
```java
String record = "h2o_feet,location=coyote_creek water_level=2.927,level\\ description=\"below 3 feet\"";

influxDBReactive.writeRecord(record);
```

##### Write measurements every 10 seconds
```java
Flowable<H2OFeetMeasurement> measurements = Flowable.interval(10, TimeUnit.SECONDS, Schedulers.trampoline())
    .map(time -> {

        double h2oLevel = getLevel();
        String location = getLocation();
        String description = getLocationDescription();
                    
        return new H2OFeetMeasurement(location, h2oLevel, description, Instant.now());
    });
        
influxDBReactive.writeMeasurements(measurements);
```

##### Write through UDP
```java
WriteOptions udpOptions = WriteOptions.builder()
    .udp(true, 8089)
    .build();

CpuLoad cpuLoad = new CpuLoad();
cpuLoad.host = "server02";
cpuLoad.value = 0.67D;

influxDBReactive.writeMeasurement(cpuLoad, udpOptions);
```

### Queries
The queries uses the [InfluxDB chunking](https://docs.influxdata.com/influxdb/latest/guides/querying_data/#chunking) 
for streaming response to the consumer. The default `chunk_size` is preconfigured to 10,000 points 
(or series) and can be configured for every query by `QueryOptions`.

#### Query configuration
- `chunkSize` - the number of QueryResults to process in one chunk
- `precision` - the time unit of the results 

```java
QueryOptions options = QueryOptions.builder()
    .chunkSize(20_000)
    .precision(TimeUnit.SECONDS)
    .build();

Query query = new Query("select * from cpu", "telegraf");
Flowable<CpuMeasurement> measurements = influxDBReactive.query(query, Cpu.class, options);
...
```
#### Examples
##### The CPU usage in last 72 hours
```java
Instant last72hours = Instant.now().minus(72, ChronoUnit.HOURS);

Query query = new Query("select * from cpu", "telegraf");

Single<Double> sum = influxDBReactive.query(query, Cpu.class)
    .filter(cpu -> cpu.time.isAfter(last72hours))
    .map(cpu -> cpu.usageUser)
    .reduce(0D, (usage1, usage2) -> usage1 + usage2);

System.out.println("The CPU usage in last 72 hours: " + sum.blockingGet());
```
##### The maximum disks usages
```java
Query query = new Query("select * from disk", "telegraf");

Flowable<Disk> maximumDisksUsages = influxDBReactive.query(query, Disk.class)
    .groupBy(disk -> disk.device)
    .flatMap(group -> group
        .reduce((disk1, disk2) -> disk1.usedPercent.compareTo(disk2.usedPercent) > 0 ? disk1 : disk2)
        .toFlowable());

maximumDisksUsages.subscribe(disk -> System.out.println("Device: " + disk.device + " percent usage: " + disk.usedPercent));
```
##### Group measurements by host
```java
Flowable<Cpu> cpu = influxDBReactive.query(new Query("select * from cpu", "telegraf"), Cpu.class);
Flowable<Mem> mem = influxDBReactive.query(new Query("select * from mem", "telegraf"), Mem.class);

Flowable.merge(cpu, mem)
    .groupBy(it -> it instanceof Cpu ? ((Cpu) it).host : ((Mem) it).host)
    .flatMap(group -> {
                    
        // Operate with grouped measurements by their tag host
        
    });
```
### Advanced Usage

#### Gzip's support 
Same as the non reactive client. For detail information see [documentation](#user-content-gzips-support-version-25-required).

#### Check the status and version of InfluxDB instance
The InfluxDB HTTP API [ping](https://docs.influxdata.com/influxdb/latest/tools/api/#ping) endpoint provides ability 
to check the status of your InfluxDB instance and your version of InfluxDB:

```java
// check response time and version
influxDBReactive
    .ping()
    .subscribe(pong -> {
        
        long responseTime = pong.getResponseTime();
        String version = pong.getVersion();
        
        System.out.println("InfluxDB response time: " + responseTime + " version: " + version);
    });

// check only the version
influxDBReactive
    .version()
    .subscribe(version -> System.out.println("InfluxDB version: " + version));
```

## FAQ

###  How to tell the system to stop sending more chunks once I've found what I'm looking for?

This is done automatically by disposing the downstream sequence. 

The query `select * from disk` return 1 000 000 rows, chunking is set to 1000 and 
we want only the first 500 results. The result is that the stream is closed after first chunk.

```java
QueryOptions options = QueryOptions.builder()
    .chunkSize(1_000)
    .build();
        
Flowable<QueryResult> results = client
    // 1 000 000 rows
    .query(new Query("select * from disk", database), options)
    // We want only the first 500
    .take(500);
```

### Is there a way to tell all chunks have arrived? 

Yes, by `onComplete` action.

```java
Flowable<H2OFeetMeasurement> measurements = influxDBReactive
    .query(query, H2OFeetMeasurement.class, options)
    .doOnComplete(() -> System.out.println("All chunks have arrived."));
```

## Version

The latest version for Maven dependency:
```xml
<dependency>
  <groupId>io.bonitoo.influxdb</groupId>
  <artifactId>influxdb-java-reactive</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```
  
Or when using with Gradle:
```groovy
dependencies {
    compile "io.bonitoo.influxdb:influxdb-java-reactive:1.0.0-SNAPSHOT"
}
```

### Snapshot repository
The snapshot repository is temporally located [here](https://apitea.com/nexus/content/repositories/bonitoo-snapshot/).

#### Maven
```xml
<repository>
    <id>bonitoo-snapshot</id>
    <name>Bonitoo.io snapshot repository</name>
    <url>https://apitea.com/nexus/content/repositories/bonitoo-snapshot/</url>
    <releases>
        <enabled>false</enabled>
    </releases>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
```
#### Gradle
```
repositories {

    maven { url "https://apitea.com/nexus/content/repositories/bonitoo-snapshot" }
}
```

### Build Requirements

* Java 1.8+ (tested with jdk8)
* Maven 3.0+ (tested with maven 3.5.0)
* Docker daemon running

Then you can build influxdb-java-reactive with all tests with:

```bash
$ mvn clean install
```

If you don't have Docker running locally, you can skip tests with -DskipTests flag set to true:

```bash
$ mvn clean install -DskipTests=true
```

If you have Docker running, but it is not at localhost (e.g. you are on a Mac and using `docker-machine`) you can set an optional environments to point to the correct IP addresses and ports:

- `INFLUXDB_IP`
- `INFLUXDB_PORT_API`

```bash
$ export INFLUXDB_IP=192.168.99.100
$ mvn test
```

For convenience we provide a small shell script which starts a InfluxDB inside Docker containers and executes `mvn clean install` with all tests locally.

```bash
$ ./compile-and-test.sh
```

## Developer

Add licence to files: `mvn license:format`.
