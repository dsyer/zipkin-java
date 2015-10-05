/**
 * Copyright 2015 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.zipkin;

import com.facebook.swift.codec.ThriftCodecManager;
import com.facebook.swift.service.ThriftServer;
import com.facebook.swift.service.ThriftServerConfig;
import com.facebook.swift.service.ThriftServiceProcessor;
import com.zaxxer.hikari.HikariDataSource;
import io.zipkin.jdbc.JDBCSpanStore;
import io.zipkin.scribe.Scribe;
import io.zipkin.scribe.ScribeSpanConsumer;
import java.io.IOException;
import org.jooq.conf.Settings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import static io.zipkin.internal.Util.envOr;
import static java.util.Collections.emptyList;

@SpringBootApplication
public class ZipkinServer {

  public static void main(String[] args) throws IOException, InterruptedException {
    // Silence Invalid method name: '__can__finagle__trace__v3__'
    System.setProperty("logging.level.com.facebook.swift.service.ThriftServiceProcessor", "OFF");

    System.setProperty("server.port", envOr("QUERY_PORT", "9411"));
    SpringApplication.run(ZipkinServer.class, args)
        .getBean(ThriftServer.class).start();
  }

  @Bean
  SpanStore provideSpanStore() {
    if (System.getenv("MYSQL_HOST") != null) {
      String mysqlHost = System.getenv("MYSQL_HOST");
      int mysqlPort = envOr("MYSQL_TCP_PORT", 3306);
      String mysqlUser = envOr("MYSQL_USER", "");
      String mysqlPass = envOr("MYSQL_PASS", "");

      String url = String.format("jdbc:mysql://%s:%s/zipkin?user=%s&password=%s&autoReconnect=true",
          mysqlHost, mysqlPort, mysqlUser, mysqlPass);

      HikariDataSource datasource = new HikariDataSource();
      datasource.setDriverClassName("com.mysql.jdbc.Driver");
      datasource.setJdbcUrl(url);
      datasource.setMaximumPoolSize(10);
      datasource.setConnectionTestQuery("SELECT '1'");
      return new JDBCSpanStore(datasource, new Settings());
    } else {
      return new InMemorySpanStore();
    }
  }

  @Bean
  Scribe scribeConsumer(SpanStore spanStore) {
    return new ScribeSpanConsumer(spanStore::accept);
  }

  @Bean
  ThriftServer scribeServer(Scribe scribe) {
    int scribePort = envOr("COLLECTOR_PORT", 9410);
    ThriftServiceProcessor processor = new ThriftServiceProcessor(new ThriftCodecManager(), emptyList(), scribe);
    return new ThriftServer(processor, new ThriftServerConfig()
        .setBindAddress("localhost")
        .setPort(scribePort));
  }
}
