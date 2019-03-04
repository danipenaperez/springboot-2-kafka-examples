package com.dppware.serviceA.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dppware.serviceA.dto.MessageBrokerDTO;
import com.dppware.serviceA.dto.TopicDescriptionDTO;
import com.dppware.serviceA.services.BrokerService;

@RestController
public class MessagingController {

	@Autowired
	private AdminClient kafkaAdminClient;
	
	
	@Autowired
	private BrokerService brokerService;
	/**
	 * Publish a message over {topicName}
	 * @param messageInfo
	 * @param topic
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@RequestMapping(value="topics/{topicName}/messages", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.ACCEPTED)
    public void publish(@RequestBody MessageBrokerDTO messageInfo, @PathVariable("topicName")String topic) throws InterruptedException, ExecutionException {
		messageInfo.setTopicName(topic);//Set destination Topic
		brokerService.sendMessage(messageInfo);
    }
	
	/**
	 * Return the messages received on topic
	 * @param messageInfo
	 * @param topic
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@RequestMapping(value="topics/{topicName}/messages", method=RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
    public List<MessageBrokerDTO> getTopicMessages(@PathVariable("topicName")String topicName) throws InterruptedException, ExecutionException {
        return brokerService.getTopicReceivedMessages(topicName).orElse(Collections.emptyList()).stream().map(messageInfo-> new MessageBrokerDTO(topicName, messageInfo)).collect(Collectors.toList());
    }
	
	
	/**
	 * Return Topic Names existing on remote Kafka
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@RequestMapping(value="topics", method=RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
    public Set<TopicDescriptionDTO> getTopicNames() throws InterruptedException, ExecutionException {
		return kafkaAdminClient.listTopics().names().get().stream().map(remoteTopicInfo-> new TopicDescriptionDTO(remoteTopicInfo)).collect(Collectors.toSet());
    }
	
	
	/**
	 * Create new Topic
	 * @param messageInfo
	 * @param topic
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@RequestMapping(value="topics", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.ACCEPTED)
    public void createTopic(@RequestBody TopicDescriptionDTO topicInfo ) {
		CreateTopicsResult result = kafkaAdminClient.createTopics(Arrays.asList(new NewTopic(topicInfo.getName(), 1, (short)1)));
		//configure Future creation topic event behaviour
		result.values().entrySet().forEach(topicCreationRequestInfo-> new KafkaFuture.Function<Void, Map<String, TopicDescription>>(){
			@Override
			public Map<String, TopicDescription> apply(Void a) {
				System.out.println(String.format("The topic %s has been created ", topicCreationRequestInfo.getKey() ));
				return null;
			}
			
		});
    }
	
	
	
	
}
