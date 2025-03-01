# Development

## Mongo

Use

```sh
docker-compose up -d`
```

to start Mongo database.

*Hint:* It is required to use MongoDB as a ReplicaSet (for change-streams).

Use these parameters to connect to Mongo database using a GUI database tool:

- *Hostname:* localhost
- *Port:* 27017
- *Replica set:* rs-business-cockpit
- *Username:* business-cockpit
- *Password:* business-cockpit
- *Authentication database:* business-cockpit
- *Database:* business-cockpit

## Simulator

The *simulator* is a standalone Spring Boot application which mimiks the behavior of bounded systems. It can be used to develop features of tasklist without the need of having external services available.

### Generate testdata

To develop tasklist features dummy tasks can be generated. At [this URL](http://localhost:8079/testdata/usertask/form) you will find a form used to trigger new tasks. The tasks generated are independent from any process instance data.
