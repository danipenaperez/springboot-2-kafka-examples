# springboot-2-kafka-examples
Spring Boot 2 using (dockerized kafka/zookeeper) Consumes, Produces , etc..

### Prerequisites

Maven and Java. 

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

### The Producer Messages Configuration

The first is configure our KafkaProducerConfig configuration Bean, the code is self explained at KafkaProducerConfig.java and then you will be able to inject a KafkaTemplate that provides mechanish to publish messages and get notified callback methods

```	
@Bean
public ProducerFactory<String, String> producerFactory() {
	Map<String, Object> configProps = new HashMap<>();
	configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
	configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
	configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
	configProps.put(TopicConfig.DELETE_RETENTION_MS_CONFIG, TimeUnit.DAYS.toMillis(1l));//Set global TTL
	//or further configuration params
	return new DefaultKafkaProducerFactory<>(configProps);
}

@Autowired
private KafkaTemplate<String, String> kafkaTemplate;
```

The KafkaConsumerConfig is.java easy readable. 

### The Consumer Messages Configuration
Basically is create a ConsumerFactory (in this example we use ConcurrentKafkaListenerContainerFactory implementation ). After confgure and add the @EnableKafka
Spring will create the internal context that enable to inject the @kafkaListener annotations in our classes method that handle the incoming messages.

```	
@EnableKafka 
...
@Bean
public ConsumerFactory<String, String> consumerFactory() {
	Map<String, Object> props = new HashMap<>();
	props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
	props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
	props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
	props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
	// props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);//Enables manual ACK MODE
	return new DefaultKafkaConsumerFactory<>(props);
}

@Bean
public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
	ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
	factory.setConsumerFactory(consumerFactory());
	// factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);//Enables manual ACK MODE
	return factory;
}


```	

## Running the tests

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
KafkaExampleApplication.java > Run As Java Application 
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

The best way to test this app tutorial is running this code as 2 different process, and play changing the properties/topics/groups and other configurations.
You can start some process in different terminals, using parametrized startup

```
java -jar ./target/serviceKafka-0.0.1-SNAPSHOT.jar --server.port=8080 --kafka.listener.groupId=groupA
java -jar ./target/serviceKafka-0.0.1-SNAPSHOT.jar --server.port=8081 --kafka.listener.groupId=groupB
```


## HTTP CONTROLLERS TO PLAY WITH THE APP

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

## DINAMICALLY SUBSCRIPTION
**Listening for Messages.  : is not dinamically. If you create a topic after startup you will not receive the messages immediately** . After few time (defautls 5 minutes, your application will be notified by kafka server and will listen for the new topics (reconfiguration), but maybe you lost some messages.

If you publish a message on a topic that not exist before you will see this trace, and any service receive the message
```
2019-03-04 14:19:11.346  WARN 28733 --- [ad | producer-1] org.apache.kafka.clients.NetworkClient   : [Producer clientId=producer-1] Error while fetching metadata with correlation id 1 : {TopicStart13=LEADER_NOT_AVAILABLE}

So not exist a leader on topic groud that is listening this Topic already. At kafka serve traces we can read:

Updated PartitionLeaderEpoch. New: {epoch:0, offset:0}, Current: {epoch:-1, offset:-1} for Partition: TopicStart13-0. Cache now contains 0 entries. (kafka.server.epoch.LeaderEpochFileCache
```


At Startup traces, will see ** KafkaMessageListenerContainer    : partitions assigned: [TopicStart1-0, TopicStart4-0, TopicStart5-0, TopicStart2-0, TopicStart3-0] **  but after that no listeners where added, so if othe process create the TopicStart6 , you will NOT receive the message even though the app is listening using the wildCard TopicStart.* 

After few time we see a message Coordinator on our Application:
```
2019-03-04 14:23:48.590  INFO 28776 --- [ntainer#0-0-C-1] o.a.k.c.c.internals.ConsumerCoordinator  : [Consumer clientId=consumer-1, groupId=groupB] Revoking previously assigned partitions [TopicStart4-0, TopicStart5-0, TopicStart2-0, TopicStart11-0, TopicStart3-0, TopicStart12-0, TopicStart8-0, TopicStart10-0, TopicStart9-0, TopicStart6-0, TopicStart7-0, TopicStart1-0]
2019-03-04 14:23:48.590  INFO 28776 --- [ntainer#0-0-C-1] o.s.k.l.KafkaMessageListenerContainer    : partitions revoked: [TopicStart4-0, TopicStart5-0, TopicStart2-0, TopicStart11-0, TopicStart3-0, TopicStart12-0, TopicStart8-0, TopicStart10-0, TopicStart9-0, TopicStart6-0, TopicStart7-0, TopicStart1-0]
2019-03-04 14:23:48.590  INFO 28776 --- [ntainer#0-0-C-1] o.a.k.c.c.internals.AbstractCoordinator  : [Consumer clientId=consumer-1, groupId=groupB] (Re-)joining group
2019-03-04 14:23:48.591  INFO 28776 --- [ntainer#0-0-C-1] o.a.k.c.c.internals.AbstractCoordinator  : [Consumer clientId=consumer-1, groupId=groupB] Successfully joined group with generation 7
2019-03-04 14:23:48.592  INFO 28776 --- [ntainer#0-0-C-1] o.a.k.c.c.internals.ConsumerCoordinator  : [Consumer clientId=consumer-1, groupId=groupB] Setting newly assigned partitions [TopicStart13-0, TopicStart4-0, TopicStart5-0, TopicStart11-0, TopicStart2-0, TopicStart12-0, TopicStart3-0, TopicStart8-0, TopicStart10-0, TopicStart9-0, TopicStart6-0, TopicStart7-0, TopicStart1-0]
2019-03-04 14:23:48.595  INFO 28776 --- [ntainer#0-0-C-1] o.a.k.c.consumer.internals.Fetcher       : [Consumer clientId=consumer-1, groupId=groupB] Resetting offset for partition TopicStart13-0 to offset 1.
2019-03-04 14:23:48.595  INFO 28776 --- [ntainer#0-0-C-1] o.s.k.l.KafkaMessageListenerContainer    : partitions assigned: [TopicStart13-0, TopicStart4-0, TopicStart5-0, TopicStart11-0, TopicStart2-0, TopicStart12-0, TopicStart3-0, TopicStart8-0, TopicStart10-0, TopicStart9-0, TopicStart6-0, TopicStart7-0, TopicStart1-0]
```

The message **Successfully joined group with generation 7** and **Setting newly assigned partitions [..** and you will see that the app rejoined and willl start to receive from the new Topic that matchs with the TopicPattern. GOOD!  (but maybe we lost some messages in this interval.)

**Dealing with ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG and ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG will get you able to adjust this time.**
When a App in the same group leaves the kafka cluster, the coordiator event is fired too. The kafka Server has properties to block the min and max values for the session for the different clients that wants to connect (group.min.session.timeout.ms and group.max.session.timeout.ms)




## Groups And Partitions
**Groups**
Groups are diferent kind of clients that listen for a topic. 
If you start 2 client applications with **THE SAME**  groupId listen the same topic, all acts as Queue and **ONLY ONE consumer application will receive the event**
If you start 2 client applications with **DIFFERENT** groupId listen the same topic, all acts as Topic **ALL OF THEM will receive the event**

To test the application running with different groupId, start it on this way :
```
java -jar ./target/serviceA-0.0.1-SNAPSHOT.jar --server.port=8080 --kafka.listener.groupId=groupA
java -jar ./target/serviceA-0.0.1-SNAPSHOT.jar --server.port=8081 --kafka.listener.groupId=groupB
```
 
Now you can publish messages using the http api (one on 8080 and 8081), and you will see that the two services receive the message.

**Partitions**
Kafka on received an event, store it on diferent partitions (using round-robin) unless you specified the target partition, It is useful when you want to put focus on performance.
All consumers in a consumer group are assigned a set of partitions, under two conditions : no two consumers in the same group have any partition in common - and the consumer group as a whole is assigned every existing partition.



## Dealing with ACKNOWLEDGE 
Sometimes , we want to manage programatically the ACK process. Imagine that our service receive the event, but fails when to process it ... we don't want lost the message and we let another process take care of it. So our "failing" process never send the ack. 
To enable this behaviour change this 
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
	try{
		//some logic
		//if not fails , send the ack:

		ack.acknowledge();//Knowledge is done

	}catch(Exception e){
		//nothing , no ack is send
	}


}
```

## TIME TO LIVE MESSAGES (TTL)
Usually is helpful to set the message Time To live if is not consumed. By default is a week , but to specified a value for it, use:

KafkaPRoducerConfig.java 

configProps.put(TopicConfig.DELETE_RETENTION_MS_CONFIG, TimeUnit.DAYS.toMillis(1l));//TTL



## Perpetrated

* **Me, the ugly and the bad** - *Initial work* - [danipenaperez](https://github.com/danipenaperez)


## License

This project for educational purpose

## Acknowledgments

* Rayo vallecano de Madrid, and Jesus "magic" Diego Cota 

