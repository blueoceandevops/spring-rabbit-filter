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

![](https://bit.ly/2H8Frhu)

For the producer, there is no difference between one message and another. The logic behind why or how the messages are segregated is totally defined by the relation established by the consumers and the RabbitMQ server.

### How to Segregate

Certantly, there are different ways to enchive the segratation. I'll discribe what a found.

#### Header Exchange

First, is mandatory to create an _Header Exchange_. Most common RabbitMQ implementations use _Topic Exchange_ to route messages  through routing key. Since the goal is to route based on header data, Topic Exchange wouldn't help. 

```java
Exchange headersExchange = headersExchange("EXCHANGE_NAME").build();
amqpAdmin.declareExchange(headersExchange);
```

With the exchange created, a bind must be declared with two important information: _(1)_ the queue where the message must be routed and, the purpose of all of this, _(2)_ the key-value that every message must have to be routed to the queue.

```java
Queue queue = new Queue("filter.queue.mpa", true, false, false, args);
amqpAdmin.declareQueue(queue);
Map headers = new HashMap<>();
headers.put("Mpa", "MPA-001");
amqpAdmin.declareBinding(bind(queue).to(headersExchange).with("").and(headers));
```

Making things even clear: only the messages created with header containing the key `"Mpa"` with the value `"MPA-001"` will be routed to the queue `"filter.queue.mpa"`.

An obvious question: What about the others messages without the expected header information ?

### Another Exchange

Short answer to the previuos question: They go to LIMBO.

That's not what we wanted. To fix that, another Exchange must be defined to deal with the others messages and routing them to a different queue.

The idea is: If the _Header Exchange_ fails to route the message, an **Alternate Exchange** will handle it.

#### Alternate Exchange
Alternate Exchange is a RabbitMQ concept. It's an internal exchange that, when associated with another exchange (our Header Exchange) receives all the messages the first couldn't route. 

So if we associate a different queue to the _Alternate Exchange_, the final goal is achieved: The messages are segregated into two queues depending on the informed header information on every message published.

That's the final RabbitMQ diagram representing the whole solution:

![](https://bit.ly/2VRvIo8)


### The java code

First, _Header Exchange_ must be changed, adding the Alternate Exchange information

```java
Exchange headersExchange = headersExchange("EXCHANGE_NAME")
        .withArgument("alternate-exchange", "ALTERNATE_EXCHANGE_NAME").build();
```
The Alternate Exchange is created as a Fanout Exchange, and a new queue is binded to it.

```java
Queue queue2 = new Queue("filter.queue.all", true, false, false, args);
amqpAdmin.declareQueue(queue2);

Exchange fanoutExchange = fanoutExchange(ALTERNATE_EXCHANGE_NAME").build();
amqpAdmin.declareExchange(fanoutExchange);
amqpAdmin.declareBinding(bind(queue2).to(fanoutExchange).with("").noargs());
``` 

