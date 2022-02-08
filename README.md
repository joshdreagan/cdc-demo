# CDC Demo

## Setup the MS SQL Server DB

```
oc new-project earth

oc adm policy add-scc-to-user anyuid -z default -n earth #This command must be run as cluster-admin

oc apply -f earth-data-sql.yml -n earth
oc new-app mcr.microsoft.com/mssql/server:2017-CU9-ubuntu -e 'ACCEPT_EULA=Y' -e 'SA_PASSWORD=Password!' -e 'MSSQL_PID=Standard' -e 'MSSQL_AGENT_ENABLED=true' -n earth
oc set volume deployment/server --add --name=earth-data --claim-size 10G --mount-path=/var/opt/mssql -n earth
oc set volume deployment/server --add --name=earth-data-sql --type=configmap --mount-path=/opt/workshop --configmap-name=earth-data-sql -n earth

oc exec $(oc get pods -n earth -l 'deployment=server' -o jsonpath='{.items[0].metadata.name}') -n earth -- /opt/mssql-tools/bin/sqlcmd -S server.earth.svc -U sa -P 'Password!' -i /opt/workshop/earth-data.sql
```

## Setup the PostgreSQL DB

```
oc new-project moon

oc apply -f moon-data-sql.yml -n moon
oc new-app postgresql:10 -e 'POSTGRESQL_USER=user1' -e 'POSTGRESQL_PASSWORD=Abcd1234!' -e 'POSTGRESQL_ADMIN_PASSWORD=Abcd1234!' -e 'POSTGRESQL_DATABASE=sampledb' -n moon
oc set volume deployment/postgresql --add --name=moon-data --claim-size 10G --mount-path=/var/lib/pgsql/data -n moon
oc set volume deployment/postgresql --add --name=moon-data-sql --type=configmap --mount-path=/opt/workshop --configmap-name=moon-data-sql -n moon

oc exec $(oc get pods -n moon -l 'deployment=postgresql' -o jsonpath='{.items[0].metadata.name}') -n moon -- /usr/bin/env PGPASSWORD='Abcd1234!' psql -dsampledb -hpostgresql.moon.svc -Uuser1 -f/opt/workshop/moon-data.sql
```

## Setup the Kafka cluster

```
oc new-project streams

# Make sure to install the AMQ Streams Operator.

oc apply -f kafka-cluster.yml -n streams

oc apply -f kafka-connect.yml -n streams
oc apply -f kafka-connector-sqlserver-debezium.yml -n streams
```

## Build/deploy the Fuse CDC Processor App

```
pushd postgresql-cdc-processor
oc new-project camel
mvn -P openshift clean package oc:deploy
popd
```

## Monitor the CDC topic (optional)

```
oc run -n streams kafka-consumer -ti --image=registry.redhat.io/amq7/amq-streams-kafka-30-rhel8:2.0.0 --rm=true --restart=Never -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic mssql-server-linux.dbo.Orders --from-beginning
```

## Make some changes to the MS SQL Server DB

```
oc run -n earth server-client -ti --image=mcr.microsoft.com/mssql/server:2017-CU9-ubuntu --rm=true --restart=Never -- /opt/mssql-tools/bin/sqlcmd -S server.earth.svc -U sa -P 'Password!'
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
oc run -n moon postgresql-client -ti --image=registry.redhat.io/rhel8/postgresql-10 --rm=true --restart=Never -- /usr/bin/env PGPASSWORD='Abcd1234!' psql -dsampledb -hpostgresql.moon.svc -Uuser1
```

```
select * from Orders;
```

## Camel K (optional)

If you want to run a Camel K app which syncs an AWS DynamoDB table, `cd` into the 'dynamodb-cdc-processor' directory and folow [these instructions](./dynamodb-cdc-processor/README.md).
