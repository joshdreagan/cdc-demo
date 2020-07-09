# Camel K

## Prerequisites

Create an AWS DynamoDB table with a key named "OrderId" of type "Number".

Install the Camel K operator and command line client.

## DynamoDB CDC Processor

Edit the [dynamodb-cdc-processor-configmap.properties](./dynamodb-cdc-processor-configmap.properties) and [dynamodb-cdc-processor-secret.properties](./dynamodb-cdc-processor-secret.properties) files with your settings and credentials. Then create the ConfigMap & Secret.

```
oc create configmap dynamodb-cdc-processor-configmap --from-file=application.properties=./dynamodb-cdc-processor-configmap.properties -n camel-k
oc create secret generic dynamodb-cdc-processor-secret --from-file=application.properties=./dynamodb-cdc-processor-secret.properties -n camel-k
```

Run the Camel K application. _Note: Append `--dev` to the end of the command if you want to run in dev mode._

```
kamel run \
--namespace camel-k \
--configmap dynamodb-cdc-processor-configmap \
--secret dynamodb-cdc-processor-secret \
./DynamoDbCdcProcessor.java
```
