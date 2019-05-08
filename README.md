# spring-rabbit-filter
A sample application to validate the use of Headers Exchange and Alternate Exchange to filter messages 

## Running the app

### RabbitMQ
The only external dependency is the RabbitMQ server, which is used as a docker container. To start up the server:

```bash
$ docker-compose up
```

Something like this would be printed at the console:

```
rabbitmq    | 2019-05-07 20:10:58.387 [info] <0.8.0> Server startup complete; 3 plugins started.
rabbitmq    |  * rabbitmq_management
rabbitmq    |  * rabbitmq_web_dispatch
rabbitmq    |  * rabbitmq_management_agent
```

### Spring-Boot App
It's a maven, java 8, Spring Boot application.

```bash
$ mvn clean package
$ java -jar ./target/rabbit-filter-0.1.jar
```

After start, some messagens are published and consumed by the listeners. At the console you will see:

```
... INFO 47184 --- [main] wirecard.filtering.Application: Started Application in 2.912 seconds (JVM running for 3.772)
 [x] Sent message: 'Filtered Message'
 [x] Sent message: 'NOT Filtered Message'
FilterListener - [message] Filtered Message [header] MPA-001
NoFilterListener - [message] NOT Filtered Message [header] null
```

## Code Explanation

### Main Idea
The purpose is to segregate messages consume based on some message header information.

