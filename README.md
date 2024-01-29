# CDC Demo

## Setup the MS SQL Server DB

```
oc new-project earth

#This command must be run as cluster-admin
oc adm policy add-scc-to-user anyuid -z default -n earth

oc apply -f mssql-sql.yml -n earth
oc apply -f mssql-data.yml -n earth
oc apply -f mssql-deployment.yml -n earth
oc apply -f mssql-service.yml -n earth

oc exec $(oc get pods -n earth -l 'deployment=server' -o jsonpath='{.items[0].metadata.name}') -n earth -- /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P 'Abcd1234' -i /opt/workshop/mssql-sql.sql
```

## Setup the PostgreSQL DB

```
oc new-project moon

oc apply -f postgresql-sql.yml -n moon
oc apply -f postgresql-data.yml -n moon
oc apply -f postgresql-deployment.yml -n moon
oc apply -f postgresql-service.yml -n moon

oc exec $(oc get pods -n moon -l 'deployment=postgresql' -o jsonpath='{.items[0].metadata.name}') -n moon -- /usr/bin/env PGPASSWORD='Abcd1234!' psql -dsampledb -hpostgresql.moon.svc -Uuser1 -f/opt/workshop/postgresql-sql.sql
```

## Setup the Kafka cluster

```
oc new-project streams

# Make sure to install the AMQ Streams Operator.

oc apply -f kafka-cluster.yml -n streams

oc apply -f kafka-connect.yml -n streams
oc apply -f kafka-connector-sqlserver-debezium.yml -n streams
```

## Build/deploy the Camel CDC Processor App

```
pushd postgresql-cdc-processor
oc new-project camel
mvn -P openshift clean package oc:deploy
popd
```

## Monitor the CDC topic (optional)

```
oc run -n streams kafka-consumer -ti --image=registry.redhat.io/amq-streams/kafka-36-rhel8:2.6.0-4 --rm=true --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic server.earth.InternationalDB.dbo.Orders --from-beginning
```

## Make some changes to the MS SQL Server DB

```
oc run -n earth server-client -ti --image=mcr.microsoft.com/mssql/server:2022-latest --rm=true --restart=Never -- /opt/mssql-tools/bin/sqlcmd -S server.earth.svc -U sa -P 'Abcd1234'
```

```
use InternationalDB
go

insert into Orders values ('I', 'Cogs', 10, '$1', '1111 Some Street, Anytown, US', '11111-1111', 'avandelay')
go

update Orders set OrderItemName = 'Sprockets' where OrderId = 1
go

delete from Orders where OrderId = 1
go
```

## Check the PostgreSQL DB

```
oc run -n moon postgresql-client -ti --image=postgres:15 --rm=true --restart=Never -- /usr/bin/env PGPASSWORD='Abcd1234!' psql -dsampledb -hpostgresql.moon.svc -Uuser1
```

```
select * from Orders;
```

## Camel K (optional)

If you want to run a Camel K app which syncs an AWS DynamoDB table, `cd` into the 'dynamodb-cdc-processor' directory and folow [these instructions](./dynamodb-cdc-processor/README.md).
