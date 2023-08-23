/*
 * Copyright 2016 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */
package com.redhat.examples;

import java.util.Map;
import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CamelConfiguration extends RouteBuilder {

  private static final Logger log = LoggerFactory.getLogger(CamelConfiguration.class);

  @Override
  public void configure() throws Exception {

    from("kafka:server.earth.InternationalDB.dbo.Orders")
      .log(LoggingLevel.INFO, log, "Picked up message: [${body}]")
      .filter(body().isNull())
        .stop()
      .end()
      .unmarshal().json(JsonLibrary.Jackson, Map.class)
      .setHeader("DebeziumOperation").groovy("request.body.payload?.op")
      .routingSlip().simple("direct:${header.DebeziumOperation}")
    ;

    from("direct:c")
      .transform().groovy("request.body.payload?.after")
      .to(ExchangePattern.InOnly, "sql:insert into Orders values (:#${body['OrderId']}, :#${body['OrderType']}, :#${body['OrderItemName']}, :#${body['Quantity']}, :#${body['Price']}, :#${body['ShipmentAddress']}, :#${body['ZipCode']}, :#${body['OrderUser']})?dataSource=#dataSource")
    ;

    from("direct:u")
      .transform().groovy("request.body.payload?.after")
      .to(ExchangePattern.InOnly, "sql:update Orders set OrderType = :#${body['OrderType']}, OrderItemName = :#${body['OrderItemName']}, Quantity = :#${body['Quantity']}, Price = :#${body['Price']}, ShipmentAddress = :#${body['ShipmentAddress']}, ZipCode = :#${body['ZipCode']}, OrderUser = :#${body['OrderUser']} where OrderId = :#${body['OrderId']}?dataSource=#dataSource")
    ;

    from("direct:d")
      .transform().groovy("request.body.payload?.before")
      .to(ExchangePattern.InOnly, "sql:delete from Orders where OrderId = :#${body['OrderId']}?dataSource=#dataSource")
    ;
  }
}
