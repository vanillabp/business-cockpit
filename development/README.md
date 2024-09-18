# Development





* The VanillaBP business cockpit is a Java Spring Boot application using React as a web framework.

For local development there are two preconditions:

1. [A MongoDB cluster](#mongodb)
1. [A local NPM registry](#local-npm-registry)

The MongoDB and the NPM registry can be started by a prepared configuration using docker-compose:

```sh
cd development
docker-compose up -d
```

Afterwards one can [build and run the business cockpit](#build-and-run-the-business-cockpit).

*Hint:* Building the business cockpit will also establish npm-links between the packages provided by this repository. This helps to ensure using the right version during the build and also supports local development.

### MongoDB

For production MongoDB one has to use replica-sets because the VanillaBP business cockpit
uses the MongoDB `changestream` feature, which not available otherwise
(see https://www.mongodb.com/docs/manual/changeStreams/).

The local MongoDB is accessible at mongodb://127.0.0.1:27017 with your favourite tool, 
without user and password (which gives you an admin role).

The development application user credentials are:

* *username:* business-cockpit
* *password:* business-cockpit

and the replica set is named `rs-business-cockpit`.

### Local NPM registry

As part of the build NPM packages are published which has to be used by BPMS software which wants to integrate to the VanillaBP business cockpit. Additionally, the business cockpit itself uses those packages as dependencies. To make this work for local development as well as for publishing builds one has to use a local NPM registry. For this the tool [Verdaccio](https://www.verdaccio.org/) is used which is also part of the provided `docker-compose.yaml`.

To use this registry one has to create a file `.npmrc` in your home folder:

```
@vanillabp:registry=http://localhost:4873
//localhost:4873/:_authToken="fake"
```

*Hint:* Verdaccio is preconfigured to accept unauthorized attempts of publishing NPM packages. Therefore, as you can see, the content of the authentication token `fake` is not taken into account - it just has to be filled.

To connect to the registry UI use these parameters:

* *URL:* [http://localhost:4873/](http://localhost:4873/)
* *username:* admin
* *password:* admin

*Hint:* If you do repeating builds for testing then you have to use the Maven profile `unpublish-npm` which removes previously published packages from the local registry.

## Build and Run the Business Cockpit

The business cockpit is developed by using Java 17 and Spring Boot 3 (reactive). To build the business cockpit Maven is used:

```sh
cd vanillabp-business-cockpit
mvn -Dnpm.registry=http://localhost:4873 package -P unpublish-npm
```

(`-P unpublish-npm` is a Maven profile which forces to unpublish NPM components previously published to Verdaccio. You might need to skip this profile for the very first build since there was no packages published before.)

After the build succeeded the service can be started:

```sh
cd business-cockpit
java -Dspring.profiles.active=local -jar target/business-cockpit-*-runnable.jar
```

To connect to the business cockpit UI use these parameters:

* *URL:* [http://localhost:8080/](http://localhost:8080/)
* *username:* test
* *password:* test

This should show up an empty business cockpit since no user tasks or workflows were reported yet. To test this before connecting your business service one can use the [simulation service](#simulation-service).

### Repeating builds

During developing the Business Cockpit itself one might change only backend code and whishes to test that changes without doing a full build.
This can be easily achieved by this Maven command:

```sh
mvn install -Plocal-install
```

If you do some UI development one can run

```sh
npm start
```

in every UI source directory to get a continuous build.

Since the first build links all npm packages, changes in one packages are active immediately using this technique.
There is one exception: The `webapp-angular`-UI in `simulator` uses the package `dev-shell-angular` and Angular does not support NPM linking. So extending `dev-shell-angular` implies the need to publish the package to the local registry after every change and update the `webapp-angular` afterward to use that change:

```sh
cd development/dev-shell-angular
mvn -Dnpm.registry=http://localhost:4873 package -P unpublish-npm
cd -
cd development/simulator/src/main/webapp
npm update --scope '@vanillabp/*'
```

## Simulation Service

This is a service which can be used for local development and testing. I mimics a business service which reports user tasks and workflows to be shown in the business cockpit UI.

To start it use these commands:

```sh
cd development/simulator
java --add-opens=java.base/java.lang=ALL-UNNAMED -jar target/simulator-*-runnable.jar
```

Now you can open the [test data generator form](http://localhost:8079/testdata/usertask/form) in your browser. It gives you verify parameters to generate user tasks. For now simply press the `Generate` button. 10 new user tasks should appear in the VanillaBP business cockpit immediately.

## Integrate your Business Services

The [simulation service](#simulation-service) also acts as template for your services. In `development/simulator/src/main/webapp` you will find samples of user task forms.

to be completed...

## Mongo

Use

```sh
docker-compose up -d
```

to start Mongo database. It is required to use MongoDB as a ReplicaSet (for change-streams), so one has to add this line to you `/etc/hosts` file:

```sh
127.0.0.1       business-cockpit-mongo
```

*Hint:* For Windows the file is `C:\Windows\System32\drivers\etc\hosts`.

Use these parameters to connect to Mongo database using a GUI database tool:

- *Hostname:* business-cockpit-mongo
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
