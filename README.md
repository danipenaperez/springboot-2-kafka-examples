# springboot-2-kafka-examples
Spring Boot 2 using (dockerized kafka/zookeeper) Consumes, Produces , etc..

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

Maven and Java. 



### Explanation
To integrate our Spring boot project we need these maven dependencies:

```
<!-- spring-kafka -->
<dependency>
  <groupId>org.springframework.kafka</groupId>
  <artifactId>spring-kafka</artifactId>
  <version>2.2.2.RELEASE</version>
</dependency>
```
Other dependencies such as lombok was added to avoid boilerplate code.

The first is configure our KafkaProducerConfig configuration Bean, the code is self explained at KafkaProducerConfig.java and then you will be able to inject a KafkaTemplate that provides mechanish to publish messages and get notified callback methods

```	
@Autowired
private KafkaTemplate<String, String> kafkaTemplate;
```

The KafkaConsumerConfig is easy readable. Are added a featured properties which enables that manage ACK ourselve.


```
...
props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);//Enables manual ACK MODE 
...


...
factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);//Enables manual ACK MODE 
...
```
After that we are able to inject Acknowledgment ack as input argument at Kafka Listener signature methods. The Message Headers info is added too, th

```
@KafkaListener(topics = {"topic1", "topic2"}, groupId = "foo")
public void listen(@Payload String message, @Headers Map<String, String> messageHeaders, final Acknowledgment ack) {

```


## Running the tests

The application 

Before to run this examples, you need a Apache-Kafka+Zookeeper bundle running on your machine. The easy way is using a dockerized Apache Kafka image (that includes the zookeeper).

To run the kafka server we are going to use the christiangda/kafka (https://hub.docker.com/r/christiangda/kafka/)

Start in this way :

```
$ docker run --tty --interactive --rm --name kafka   --publish 9092:9092   --publish 2181:2181   christiangda/kafka WITH_INTERNAL_ZOOKEEPER bin/kafka-server-start.sh config/server.properties
```

After that, the Kafka server is registered on Zookeeper, and Zookeeper asign it a host name. At start up traces look for a trace like that show the assigned hostname. In this case is **a1cf8dd1de85 ** . 
```
[2019-03-01 08:18:30,243] INFO Client environment:host.name=a1cf8dd1de85 (org.apache.zookeeper.ZooKeeper)
```
Now to connect from our SpringBoot app, need to connect to host **a1cf8dd1de85**, but if we try to connect directly using the property kafka.bootstrapAddress=a1cf8dd1de85:9092 you will get the error :
Caused by: org.apache.kafka.common.config.ConfigException: No resolvable bootstrap urls given in bootstrap.servers

so we need to bind this hostname  address to 127.0.0.1 in our /etc/hosts (or windows related file)
```
127.0.0.1 a1cf8dd1de85
```

Now Run the Java Spring Boot APP.

```
ServiceAApplication.java > Run As Java Application 
  or 
$ mvn spring-boot:run
```

On startup are created a initial topics if not exists (topic names from properties file), and related listeners for each one (BrokerService.java)
```
kafka.initial.topics=TopicStart1,TopicStart2

...

@KafkaListener(topics = {"TopicStart1", "TopicStart2"}, groupId = "group1")
public void listenTopicStart1(@Payload String message,@Headers Map<String, String> messageHeaders) {
	String topicName = messageHeaders.get("kafka_receivedTopic");//Read Headers to get topic Name
	saveMessageLocally(topicName , message);
}

//Or using wildCards  TopicStart.*   (On startup request for current Topic that match and add a listener for each one and bind to this method callback)

@KafkaListener(topicPattern="TopicStart.*", groupId = "foo")
private void listenTopicStartALL(@Payload String message,@Headers Map<String, String> messageHeaders) {
	String topicName = messageHeaders.get("kafka_receivedTopic");//Read Headers to get topic Name
	saveMessageLocally(topicName , message);
}

// or one simple listener for each one:

@KafkaListener(topics = "TopicStart1", groupId = "group1")
public void listenTopic1(String message) {
    System.out.println("Received Messasge in TopicStart1: " + message);
}

@KafkaListener(topics = "TopicStart2", groupId = "group1")
public void listenTopic2(String message) {
    System.out.println("Received Messasge in TopicStart2: " + message);
}

//Or creating your custom implementation

https://docs.spring.io/autorepo/docs/spring-kafka-dist/2.0.4.RELEASE/reference/htmlsingle/#message-listeners


```




Are created Controllers to test and manage the app
-MessageController.java): API:
```
Get Available Topics
	curl -X GET -i http://localhost:8080/topics 

	[{"name":"TopicStart2"},{"name":"TopicStart1"}]

Create a Topic
	curl -X POST -H 'Content-Type: application/json' -i http://localhost:8080/topics --data '{"name":"topic3"}'

Publish a message on Topic (if the topic is not created, will be build): Exist a callback functions on publish message BrokerService.java/sendMessage()
	curl -X POST -H 'Content-Type: application/json' -i http://localhost:8080/topics/TopicStart1/messages --data '{"content":"Message 1 to topic 1"}'

Get Messages Received from a Specified Topic
	curl -X GET -i http://localhost:8080/topics/TopicStart1/messages
	
	[{"topicName":"TopicStart1","content":"Message 1 to topic1"}]	

```

**Listening for Messages.  : is not dinamically. If you create a topic after startup you will not receive the messages**

At Startup traces, will see ** KafkaMessageListenerContainer    : partitions assigned: [TopicStart1-0, TopicStart4-0, TopicStart5-0, TopicStart2-0, TopicStart3-0] **  but after that no listeners where added, so if othe process create the TopicStart6 , you will NOT receive the message even though the app is listening using the wildCard TopicStart.* 


To solve this behaviour Spring have to reload application context, to reinitialize Kafka subscriptors using the wildCard and create the configuration again.
If you need this behaviour, add actuator restart endpoints
```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

Enable the Rest endpointManagement in properties configuration

management.endpoint.restart.enabled=true

And Request this path  to restart 

curl -X POST localhost:port/restart



or using another way to reboot your Spring Boot application context 

https://www.baeldung.com/java-restart-spring-boot-app

```

## Perpetrated

* **Me, the ugly and the bad** - *Initial work* - [PurpleBooth](https://github.com/danipenaperez)


## License

This project for educational purpose

## Acknowledgments

* Hat tip to anyone whose code was used
* Inspiration
* etc

