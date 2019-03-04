package com.dppware.serviceA.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import com.dppware.serviceA.dto.MessageBrokerDTO;

@Service
public class BrokerService {

	/**
	 * LocalStorage (Memory)
	 */
	private HashMap<String, List<String>> messages = new HashMap<String, List<String>>();

	@Value(value = "${kafka.initial.topics}")
	private String initialTopics;

	
	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	@Autowired
	private AdminClient kafkaAdminClient;

	/**
	 * Initial topics creation
	 * 
	 * @param kafkaAdminClient
	 */
	@PostConstruct
	private void initialTopics() {
		Arrays.stream(initialTopics.split(",")).forEach(
				topicName -> kafkaAdminClient.createTopics(Arrays.asList(new NewTopic(topicName, 1, (short) 1))));
	}

	/**
	 * Publish a message in simplest way
	 * @param msg
	 */
	public void sendMessage(MessageBrokerDTO msg) {
		ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(msg.getTopicName(), msg.getContent());
		future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {

			@Override
			public void onSuccess(SendResult<String, String> result) {
				System.out.println(
						"Sent message=[" + msg + "] with offset=[" + result.getRecordMetadata().offset() + "]");
			}

			@Override
			public void onFailure(Throwable ex) {
				System.out.println("Unable to send message=[" + msg + "] due to : " + ex.getMessage());
			}
		});
	}

	/**
	 * Listed wildCard Topic Messages from Patter topicName - TopicStart.* -
	 * 
	 * @param message
	 * @param messageHeaders
	 */
	@KafkaListener(topicPattern = "TopicStart.*")
	private void listenTopicStartALL(@Payload String message, @Headers Map<String, String> messageHeaders) {
		String topicName = messageHeaders.get("kafka_receivedTopic");// Read Headers to get topic Name
		System.out.println("Receviced message=[" + message + "]  on ["+ topicName+ "]");
		saveMessageLocally(topicName, message);
	}

	/**
	 * EXAMPLE USING ACK MANAGEMENT
	 * Listed wildCard Topic Messages from Patter topicName - TopicStart.* -
	 * 
	 * @param message
	 * @param messageHeaders
	 
	@KafkaListener(topicPattern = "TopicStart.*")
	private void listenTopicStartALLWithACKManagement(@Payload String message, @Headers Map<String, String> messageHeaders,final Acknowledgment ack) {
		String topicName = messageHeaders.get("kafka_receivedTopic");// Read Headers to get topic Name
		System.out.println("Receviced message=[" + message + "]  on ["+ topicName+ "]");
		saveMessageLocally(topicName, message);
		ack.acknowledge();//Knowledge is done
	}
	*/
	
	
	
	/**
	 * Internal memory saving
	 * 
	 * @param topicName
	 * @param message
	 */
	private void saveMessageLocally(String topicName, String message) {
		if (!messages.containsKey(topicName))
			messages.put(topicName, new ArrayList<String>());

		messages.get(topicName).add(message);
	}

	/**
	 * Return received messages on Topic Specified
	 * 
	 * @param topicName
	 * @return
	 */
	public Optional<List<String>> getTopicReceivedMessages(String topicName) {
		return Optional.ofNullable(messages.get(topicName));
	}


}
