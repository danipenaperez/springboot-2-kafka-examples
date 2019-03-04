package com.dppware.serviceA.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString @Builder @NoArgsConstructor
public class MessageBrokerDTO {

	private String topicName;
	private String content;
	
	public MessageBrokerDTO(String topicName, String content) {
		super();
		this.topicName = topicName;
		this.content = content;
	}
	
	
}
