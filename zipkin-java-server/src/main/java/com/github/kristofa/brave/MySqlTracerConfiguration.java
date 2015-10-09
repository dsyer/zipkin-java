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

package com.github.kristofa.brave;

import java.security.SecureRandom;
import java.util.Collections;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import com.github.kristofa.brave.mysql.MySQLStatementInterceptor;
import com.mysql.jdbc.Driver;

/**
 * @author Dave Syer
 *
 */
@ConditionalOnClass({ Driver.class, MySQLStatementInterceptor.class })
@Configuration
public class MySqlTracerConfiguration {

  @Autowired
  ServerAndClientSpanState state;
  @Autowired
  SpanCollector spanCollector;

  @PostConstruct
  public void init() {
    MySQLStatementInterceptor.setClientTracer(tracer());
  }

  private ClientTracer tracer() {
    return ClientTracer.builder().traceFilters(Collections.emptyList())
        .randomGenerator(new SecureRandom()).state(this.state)
        .spanCollector(this.spanCollector).build();
  }

}
