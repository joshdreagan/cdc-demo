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
        "import com.amazonaws.services.dynamodbv2.model.AttributeValue;\n" +
        "var map = [:];\n" +
        "map['OrderId'] = new AttributeValue().withN(request.body?.OrderId as String);\n" +
        "map['OrderType'] = new AttributeValue().withS(request.body?.OrderType);\n" +
        "map['OrderItemName'] = new AttributeValue().withS(request.body?.OrderItemName);\n" +
        "map['Quantity'] = new AttributeValue().withN(request.body?.Quantity as String);\n" +
        "map['Price'] = new AttributeValue().withS(request.body?.Price);\n" +
        "map['ShipmentAddress'] = new AttributeValue().withS(request.body?.ShipmentAddress);\n" +
        "map['ZipCode'] = new AttributeValue(request.body?.ZipCode);\n" +
        "map['OrderUser'] = new AttributeValue(request.body?.OrderUser);\n" +
        "return map;"
      )
      .to("aws-ddb:Orders?operation=PutItem")
    ;
    
    from("direct:u")
      .transform().groovy("request.body.payload?.after")
      .setHeader("CamelAwsDdbKey").groovy(
        "import com.amazonaws.services.dynamodbv2.model.AttributeValue;\n" +
        "var map = [:];\n" +
        "map['OrderId'] = new AttributeValue().withN(request.body?.OrderId as String);\n" +
        "return map;"
      )
      .setHeader("CamelAwsDdbUpdateValues").groovy(
        "import com.amazonaws.services.dynamodbv2.model.AttributeAction;\n" +
        "import com.amazonaws.services.dynamodbv2.model.AttributeValue;\n" +
        "import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;\n" +
        "var map = [:];\n" +
        "map['OrderType'] = new AttributeValueUpdate(new AttributeValue().withS(request.body?.OrderType), AttributeAction.PUT);\n" +
        "map['OrderItemName'] = new AttributeValueUpdate(new AttributeValue().withS(request.body?.OrderItemName), AttributeAction.PUT);\n" +
        "map['Quantity'] = new AttributeValueUpdate(new AttributeValue().withN(request.body?.Quantity as String), AttributeAction.PUT);\n" +
        "map['Price'] = new AttributeValueUpdate(new AttributeValue().withS(request.body?.Price), AttributeAction.PUT);\n" +
        "map['ShipmentAddress'] = new AttributeValueUpdate(new AttributeValue().withS(request.body?.ShipmentAddress), AttributeAction.PUT);\n" +
        "map['ZipCode'] = new AttributeValueUpdate(new AttributeValue(request.body?.ZipCode), AttributeAction.PUT);\n" +
        "map['OrderUser'] = new AttributeValueUpdate(new AttributeValue(request.body?.OrderUser), AttributeAction.PUT);\n" +
        "return map;"
      )
      .to("aws-ddb:Orders?operation=UpdateItem")
    ;
    
    from("direct:d")
      .transform().groovy("request.body.payload?.before")
      .setHeader("CamelAwsDdbKey").groovy(
        "import com.amazonaws.services.dynamodbv2.model.AttributeValue;\n" +
        "var map = [:];\n" +
        "map['OrderId'] = new AttributeValue().withN(request.body?.OrderId as String);\n" +
        "return map;"
      )
      .to("aws-ddb:{{dynamodb.tableName}}?operation=DeleteItem")
    ;
  }
}
