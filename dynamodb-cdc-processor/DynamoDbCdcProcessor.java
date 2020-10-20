// camel-k: language=java dependency=camel-groovy
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Map;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

public class DynamoDbCdcProcessor extends RouteBuilder {

  @Override
  public void configure() throws Exception {

    from("kafka:{{kafka.topic}}")
      .log("Picked up message: [${body}]")
      .filter(body().isNull())
        .stop()
      .end()
      .unmarshal().json(JsonLibrary.Jackson, Map.class)
      .setHeader("DebeziumOperation").groovy("request.body.payload?.op")
      .routingSlip().simple("direct:${header.DebeziumOperation}")
    ;

    from("direct:c")
    .transform().groovy("request.body.payload?.after")
    .setHeader("CamelAwsDdbItem").groovy(
        "import software.amazon.awssdk.services.dynamodb.model.AttributeValue;\n" +
        "var map = [:];\n" +
        "map['OrderId'] = AttributeValue.builder().n(request.body?.OrderId as String).build();\n" +
        "map['OrderType'] = AttributeValue.builder().s(request.body?.OrderType).build();\n" +
        "map['OrderItemName'] = AttributeValue.builder().s(request.body?.OrderItemName).build();\n" +
        "map['Quantity'] = AttributeValue.builder().n(request.body?.Quantity as String).build();\n" +
        "map['Price'] = AttributeValue.builder().s(request.body?.Price).build();\n" +
        "map['ShipmentAddress'] = AttributeValue.builder().s(request.body?.ShipmentAddress).build();\n" +
        "map['ZipCode'] = AttributeValue.builder().s(request.body?.ZipCode).build();\n" +
        "map['OrderUser'] = AttributeValue.builder().s(request.body?.OrderUser).build();\n" +
        "return map;"
      )
      .to("aws2-ddb:Orders?operation=PutItem")
    ;

    from("direct:u")
      .transform().groovy("request.body.payload?.after")
      .setHeader("CamelAwsDdbKey").groovy(
        "import software.amazon.awssdk.services.dynamodb.model.AttributeValue;\n" +
        "var map = [:];\n" +
        "map['OrderId'] = AttributeValue.builder().n(request.body?.OrderId as String).build();\n" +
        "return map;"
      )
      .setHeader("CamelAwsDdbUpdateValues").groovy(
        "import software.amazon.awssdk.services.dynamodb.model.AttributeAction;\n" +
        "import software.amazon.awssdk.services.dynamodb.model.AttributeValue;\n" +
        "import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;\n" +
        "var map = [:];\n" +
        "map['OrderType'] = AttributeValueUpdate.builder().value(AttributeValue.builder().s(request.body?.OrderType).build()).action(AttributeAction.PUT).build();\n" +
        "map['OrderItemName'] = AttributeValueUpdate.builder().value(AttributeValue.builder().s(request.body?.OrderItemName).build()).action(AttributeAction.PUT).build();\n" +
        "map['Quantity'] = AttributeValueUpdate.builder().value(AttributeValue.builder().n(request.body?.Quantity as String).build()).action(AttributeAction.PUT).build();\n" +
        "map['Price'] = AttributeValueUpdate.builder().value(AttributeValue.builder().s(request.body?.Price).build()).action(AttributeAction.PUT).build();\n" +
        "map['ShipmentAddress'] = AttributeValueUpdate.builder().value(AttributeValue.builder().s(request.body?.ShipmentAddress).build()).action(AttributeAction.PUT).build();\n" +
        "map['ZipCode'] = AttributeValueUpdate.builder().value(AttributeValue.builder().s(request.body?.ZipCode).build()).action(AttributeAction.PUT).build();\n" +
        "map['OrderUser'] = AttributeValueUpdate.builder().value(AttributeValue.builder().s(request.body?.OrderUser).build()).action(AttributeAction.PUT).build();\n" +
        "return map;"
      )
      .to("aws2-ddb:Orders?operation=UpdateItem")
    ;

    from("direct:d")
      .transform().groovy("request.body.payload?.before")
      .setHeader("CamelAwsDdbKey").groovy(
        "import software.amazon.awssdk.services.dynamodb.model.AttributeValue;\n" +
        "var map = [:];\n" +
        "map['OrderId'] = AttributeValue.builder().n(request.body?.OrderId as String).build();\n" +
        "return map;"
      )
      .to("aws2-ddb:{{dynamodb.tableName}}?operation=DeleteItem")
    ;
  }
}
