# zipkin-server
The  receives spans via HTTP POST and respond to queries from zipkin-web.

Note that the server requires minimum JRE 8.

## Running locally

To run the server from the currently checked out source, enter the following.
```bash
$ ./mvnw -pl zipkin-server spring-boot:run
```

## Environment Variables
zipkin-server is a drop-in replacement for the [scala query service](https://github.com/openzipkin/zipkin/tree/master/zipkin-query-service).

The following environment variables from zipkin-scala are honored.

    * `QUERY_PORT`: Listen lookback for the query http api; Defaults to 9411
    * `QUERY_LOG_LEVEL`: Log level written to the console; Defaults to INFO
    * `QUERY_LOOKBACK`: How many milliseconds queries look back from endTs; Defaults to 7 days
    * `STORAGE_TYPE`: SpanStore implementation: one of `mem` or `mysql`
    * `COLLECTOR_SAMPLE_RATE`: Percentage of traces to retain, defaults to always sample (1.0).

This "minimal" server runs with an in-memory database. It's main function is as an integration test that Zipkin works without its optional dependencies, but it can still be used as a lightweight server for demo purposes.