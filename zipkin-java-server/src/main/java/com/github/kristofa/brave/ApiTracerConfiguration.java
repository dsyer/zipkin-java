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
// TODO: switch package back after https://github.com/openzipkin/brave/pull/99
package com.github.kristofa.brave;

import com.github.kristofa.brave.http.DefaultSpanNameProvider;
import com.github.kristofa.brave.spring.ServletHandlerInterceptor;
import com.github.kristofa.brave.zipkin.ZipkinSpanCollector;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.SecureRandom;
import java.util.Collections;
import javax.inject.Singleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class ApiTracerConfiguration extends WebMvcConfigurerAdapter {

  /** This attempts to get the IP associated with the zipkin-query server's endpoint. */
  // http://stackoverflow.com/questions/8765578/get-local-ip-address-without-connecting-to-the-internet
  @Bean
  @Singleton
  static InetAddress localAddress() throws SocketException {
    return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
        .flatMap(i -> Collections.list(i.getInetAddresses()).stream())
        .filter(ip -> ip instanceof Inet4Address && ip.isSiteLocalAddress())
        .findAny().orElseThrow(SocketException::new);
  }

  @Bean
  @Singleton
  static ServerAndClientSpanState state(InetAddress localAddress, @Value("${server.port}") int port) {
    return new ThreadLocalServerAndClientSpanState(localAddress, port, "zipkin-query");
  }

  @Bean
  @Singleton
  static ServerTracer tracer(ServerAndClientSpanState state, @Value("${zipkin.collector.port}") int scribePort) {
    return ServerTracer.builder()
        .state(state)
        .randomGenerator(new SecureRandom())
        .traceFilters(Collections.emptyList())
        .spanCollector(new ZipkinSpanCollector("127.0.0.1", scribePort))
        .build();
  }

  @Autowired
  ServerAndClientSpanState state;
  @Autowired
  ServerTracer tracer;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new ServletHandlerInterceptor(
        new ServerRequestInterceptor(this.tracer),
        new ServerResponseInterceptor(this.tracer),
        new DefaultSpanNameProvider(),
        new ServerSpanThreadBinder(this.state)
    ));
  }
}
